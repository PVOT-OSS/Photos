// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.model

import androidx.compose.runtime.Immutable

/** [items] are oldest first; [cover] is the newest. */
@Immutable
data class Album(
    val id: String,
    val kind: AlbumKind,
    val name: String,
    val items: List<MediaItem>,
) {
    val cover: MediaItem
        get() = items.last()

    val count: Int
        get() = items.size
}
