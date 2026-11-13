// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.prauga.photos.di.IoDispatcher
import org.prauga.photos.media.model.MediaItem
import timber.log.Timber

class MediaStoreDataSource
@Inject
constructor(
    private val resolver: ContentResolver,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) {
    private val filesUri: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

    fun changes(): Flow<Unit> = callbackFlow {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
        resolver.registerContentObserver(filesUri, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    /** Every image and video the app may read, oldest first. */
    suspend fun query(): List<MediaItem> =
        withContext(io) {
            try {
                resolver.query(filesUri, projection(), Selection, null, null)?.use(::read).orEmpty()
            } catch (e: SecurityException) {
                Timber.w(e, "Media permission revoked while querying MediaStore")
                emptyList()
            }
        }

    private fun projection(): Array<String> =
        buildList {
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
            .toTypedArray()

    private fun read(c: Cursor): List<MediaItem> {
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
        val items = ArrayList<MediaItem>(c.count)
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
                    uri = ContentUris.withAppendedId(base, rowId).toString(),
                    isVideo = video,
                    mimeType = c.getString(mime).orEmpty(),
                    date = if (takenMs > 0) takenMs else modifiedMs,
                    dateModified = modifiedMs,
                    width = if (rotated) h else w,
                    height = if (rotated) w else h,
                    durationMs = if (video) c.getLong(duration) else 0,
                    bucketId = c.getLong(bucketId),
                    bucketName = c.getString(bucketName).orEmpty(),
                    displayName = c.getString(name).orEmpty(),
                    size = c.getLong(size),
                    isFavorite = favorite >= 0 && c.getInt(favorite) == 1,
                )
        }
        items.sortWith(compareBy<MediaItem> { it.date }.thenBy { it.id })
        return items
    }

    private companion object {
        const val Selection =
            "${FileColumns.MEDIA_TYPE} IN (${FileColumns.MEDIA_TYPE_IMAGE}, ${FileColumns.MEDIA_TYPE_VIDEO})"
    }
}
