// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.photos.media.data.MediaRepository
import org.prauga.photos.media.model.CaptureDetails

/** [source] is [LibrarySource] or an album id. */
@HiltViewModel(assistedFactory = ViewerViewModel.Factory::class)
class ViewerViewModel
@AssistedInject
constructor(@Assisted source: String, private val repository: MediaRepository) : ViewModel() {

    private val details = MutableStateFlow<Map<Long, CaptureDetails?>>(emptyMap())

    val uiState: StateFlow<ViewerUiState> =
        combine(
                if (source == LibrarySource) {
                    repository.media.map { it.orEmpty() to (it != null) }
                } else {
                    repository.album(source).map { it?.items.orEmpty() to true }
                },
                details,
            ) { (items, loaded), details ->
                ViewerUiState(items, loaded, details)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState())

    fun onEvent(event: ViewerEvent) {
        when (event) {
            is ViewerEvent.DetailsRequested -> {
                if (event.item.id in details.value) return
                viewModelScope.launch {
                    val read = repository.captureDetails(event.item)
                    details.update { it + (event.item.id to read) }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(source: String): ViewerViewModel
    }

    companion object {
        const val LibrarySource: String = "library"
    }
}
