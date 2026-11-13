// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.album.ui

import androidx.compose.runtime.Immutable
import org.prauga.photos.media.model.Album

@Immutable
data class AlbumUiState(val album: Album? = null)
