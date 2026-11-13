// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.album.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.photos.media.data.MediaRepository

@HiltViewModel(assistedFactory = AlbumViewModel.Factory::class)
class AlbumViewModel @AssistedInject constructor(@Assisted albumId: String, repository: MediaRepository) :
    ViewModel() {

    val uiState: StateFlow<AlbumUiState> =
        repository
            .album(albumId)
            .map(::AlbumUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumUiState())

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumViewModel
    }
}
