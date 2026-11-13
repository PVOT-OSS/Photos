// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.photos.media.data.MediaRepository

@HiltViewModel
class LibraryViewModel @Inject constructor(repository: MediaRepository) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> =
        repository.media
            .map { LibraryUiState(items = it.orEmpty(), loaded = it != null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
