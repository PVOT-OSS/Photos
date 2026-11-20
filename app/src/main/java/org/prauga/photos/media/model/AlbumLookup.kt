// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.model

/** An album asked for by id: still loading, gone (its last item deleted or moved), or found. */
sealed interface AlbumLookup {
    data object Loading : AlbumLookup

    data object Missing : AlbumLookup

    data class Found(val album: Album) : AlbumLookup
}
