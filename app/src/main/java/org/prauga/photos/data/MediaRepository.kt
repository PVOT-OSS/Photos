// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Reads photos and videos from MediaStore and follows its changes. */
class MediaRepository(private val resolver: ContentResolver) {

    private val filesUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    /** Every photo and video, oldest first, reloaded whenever MediaStore changes. */
    @OptIn(FlowPreview::class)
    fun media(): Flow<List<MediaItem>> =
        changes()
            .onStart { emit(Unit) }
            .debounce(250)
            .conflate()
            .map { query() }
            .flowOn(Dispatchers.IO)

    private fun changes(): Flow<Unit> = callbackFlow {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
        resolver.registerContentObserver(filesUri, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    private fun query(): List<MediaItem> {
        val projection = buildList {
            add(FileColumns._ID)
            add(FileColumns.MEDIA_TYPE)
            add(FileColumns.MIME_TYPE)
            add(FileColumns.DATE_TAKEN)
            add(FileColumns.DATE_MODIFIED)
            add(FileColumns.WIDTH)
            add(FileColumns.HEIGHT)
            add(FileColumns.ORIENTATION)
            add(FileColumns.DURATION)
            add(FileColumns.BUCKET_ID)
            add(FileColumns.BUCKET_DISPLAY_NAME)
            add(FileColumns.DISPLAY_NAME)
            add(FileColumns.SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(FileColumns.IS_FAVORITE)
        }
        val selection =
            "${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO})"
        val items = ArrayList<MediaItem>()
        resolver.query(filesUri, projection.toTypedArray(), selection, null, null)?.use { c ->
            val id = c.getColumnIndexOrThrow(FileColumns._ID)
            val type = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val mime = c.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val taken = c.getColumnIndexOrThrow(FileColumns.DATE_TAKEN)
            val modified = c.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
            val width = c.getColumnIndexOrThrow(FileColumns.WIDTH)
            val height = c.getColumnIndexOrThrow(FileColumns.HEIGHT)
            val orientation = c.getColumnIndexOrThrow(FileColumns.ORIENTATION)
            val duration = c.getColumnIndexOrThrow(FileColumns.DURATION)
            val bucketId = c.getColumnIndexOrThrow(FileColumns.BUCKET_ID)
            val bucketName = c.getColumnIndexOrThrow(FileColumns.BUCKET_DISPLAY_NAME)
            val name = c.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val size = c.getColumnIndexOrThrow(FileColumns.SIZE)
            val favorite = c.getColumnIndex(FileColumns.IS_FAVORITE)
            while (c.moveToNext()) {
                val rowId = c.getLong(id)
                val video = c.getInt(type) == FileColumns.MEDIA_TYPE_VIDEO
                val base =
                    if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val modifiedMs = c.getLong(modified) * 1000
                val takenMs = c.getLong(taken)
                val rotated = c.getInt(orientation).let { it == 90 || it == 270 }
                val w = c.getInt(width)
                val h = c.getInt(height)
                items +=
                    MediaItem(
                        id = rowId,
                        uri = ContentUris.withAppendedId(base, rowId),
                        isVideo = video,
                        mimeType = c.getString(mime) ?: "",
                        date = if (takenMs > 0) takenMs else modifiedMs,
                        dateModified = modifiedMs,
                        width = if (rotated) h else w,
                        height = if (rotated) w else h,
                        durationMs = if (video) c.getLong(duration) else 0,
                        bucketId = c.getLong(bucketId),
                        bucketName = c.getString(bucketName) ?: "",
                        displayName = c.getString(name) ?: "",
                        size = c.getLong(size),
                        isFavorite = favorite >= 0 && c.getInt(favorite) == 1,
                    )
            }
        }
        items.sortWith(compareBy<MediaItem> { it.date }.thenBy { it.id })
        return items
    }
}

/** Groups [items] into albums: one per bucket, most recent first. */
fun bucketAlbums(items: List<MediaItem>): List<Album> =
    items
        .groupBy { it.bucketId }
        .map { (bucket, list) ->
            Album(
                id = "bucket:$bucket",
                name = list.last().bucketName.ifEmpty { "Unknown" },
                cover = list.last(),
                count = list.size,
                items = list,
            )
        }
        .sortedByDescending { it.cover.date }

/** The virtual albums: favourites and videos, each only when it has something. */
fun mediaTypeAlbums(items: List<MediaItem>, favoritesName: String, videosName: String): List<Album> =
    listOfNotNull(
        items
            .filter { it.isFavorite }
            .takeIf { it.isNotEmpty() }
            ?.let { Album("favorites", favoritesName, it.last(), it.size, it) },
        items
            .filter { it.isVideo }
            .takeIf { it.isNotEmpty() }
            ?.let { Album("videos", videosName, it.last(), it.size, it) },
    )
