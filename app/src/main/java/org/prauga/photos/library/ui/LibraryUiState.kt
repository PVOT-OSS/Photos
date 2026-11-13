// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.compose.runtime.Immutable
import org.prauga.photos.media.model.MediaItem

@Immutable
data class LibraryUiState(val items: List<MediaItem> = emptyList(), val loaded: Boolean = false)
