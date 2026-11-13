// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.collections.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.prauga.photos.media.data.MediaRepository

@HiltViewModel
class CollectionsViewModel @Inject constructor(repository: MediaRepository) : ViewModel() {
    val uiState: StateFlow<CollectionsUiState> =
        combine(repository.mediaTypes, repository.folders, ::CollectionsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionsUiState())
}
