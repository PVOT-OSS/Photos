// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import org.prauga.photos.di.ApplicationScope
import org.prauga.photos.media.model.Album
import org.prauga.photos.media.model.CaptureDetails
import org.prauga.photos.media.model.MediaItem

@Singleton
class MediaRepository
@Inject
constructor(
    private val source: MediaStoreDataSource,
    private val exif: ExifDataSource,
    @ApplicationScope scope: CoroutineScope,
) {

    private val canRead = MutableStateFlow(false)

    /** The library, oldest first; null until read, or while the app may not read it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val media: StateFlow<List<MediaItem>?> =
        canRead
            .flatMapLatest { if (it) library() else flowOf(null) }
            .stateIn(scope, SharingStarted.WhileSubscribed(StopTimeoutMs), null)

    val folders: StateFlow<List<Album>> =
        media.map { folderAlbums(it.orEmpty()) }.stateIn(scope, SharingStarted.WhileSubscribed(StopTimeoutMs), emptyList())

    val mediaTypes: StateFlow<List<Album>> =
        media.map { mediaTypeAlbums(it.orEmpty()) }.stateIn(scope, SharingStarted.WhileSubscribed(StopTimeoutMs), emptyList())

    fun setCanRead(value: Boolean) {
        canRead.value = value
    }

    fun album(id: String): Flow<Album?> =
        combine(mediaTypes, folders) { types, buckets ->
            types.find { it.id == id } ?: buckets.find { it.id == id }
        }

    suspend fun captureDetails(item: MediaItem): CaptureDetails? = if (item.isVideo) null else exif.read(item.uri)

    @OptIn(FlowPreview::class)
    private fun library(): Flow<List<MediaItem>> =
        merge(flowOf(Unit), source.changes().debounce(ChangeDebounceMs)).conflate().map { source.query() }

    private companion object {
        const val StopTimeoutMs = 5_000L
        const val ChangeDebounceMs = 250L
    }
}
