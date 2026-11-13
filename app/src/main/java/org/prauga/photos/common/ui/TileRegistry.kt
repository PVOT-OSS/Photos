// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import org.prauga.photos.media.model.MediaItem

/** Bounds are a plain map, not state: state here would recompose the grid on every scrolled frame. */
@Stable
class TileRegistry {
    private val bounds = HashMap<Long, Rect>()

    var hiddenId: Long? by mutableStateOf(null)

    var reveal: (suspend (MediaItem) -> Unit)? = null

    fun place(id: Long, rect: Rect) {
        bounds[id] = rect
    }

    fun remove(id: Long) {
        bounds.remove(id)
    }

    fun boundsOf(id: Long): Rect? = bounds[id]
}
