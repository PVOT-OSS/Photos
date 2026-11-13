// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.photos.di.DefaultDispatcher
import org.prauga.photos.media.data.MediaRepository

@HiltViewModel
class LibraryViewModel
@Inject
constructor(repository: MediaRepository, @DefaultDispatcher dispatcher: CoroutineDispatcher) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> =
        repository.media
            .map { media ->
                val items = media.orEmpty()
                val zone = ZoneId.systemDefault()
                LibraryUiState(items, groupByMonth(items, zone), groupByYear(items, zone), loaded = media != null)
            }
            .flowOn(dispatcher)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())
}
