// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.compose.runtime.Immutable
import org.prauga.photos.media.model.CaptureDetails
import org.prauga.photos.media.model.MediaItem

@Immutable
data class ViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val loaded: Boolean = false,
    /** Keyed by item id; an id mapped to null was read and has no details. */
    val details: Map<Long, CaptureDetails?> = emptyMap(),
)
