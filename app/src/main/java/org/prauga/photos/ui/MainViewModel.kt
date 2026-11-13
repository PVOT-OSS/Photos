// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.photos.PhotosApplication
import org.prauga.photos.data.Album
import org.prauga.photos.data.MediaItem
import org.prauga.photos.data.MediaRepository
import org.prauga.photos.data.bucketAlbums

class MainViewModel(repository: MediaRepository) : ViewModel() {

    private val canRead = MutableStateFlow(false)

    /** The library, oldest first; null until the first load. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val media: StateFlow<List<MediaItem>?> =
        canRead
            .flatMapLatest { if (it) repository.media() else emptyFlow() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val albums: StateFlow<List<Album>> =
        media
            .map { bucketAlbums(it.orEmpty()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Starts or stops reading MediaStore as the permission comes and goes. */
    fun setCanRead(value: Boolean) {
        canRead.value = value
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                MainViewModel((app as PhotosApplication).container.mediaRepository)
            }
        }
    }
}
