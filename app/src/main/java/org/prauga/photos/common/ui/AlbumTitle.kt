// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.prauga.photos.R
import org.prauga.photos.media.model.Album
import org.prauga.photos.media.model.AlbumKind

@Composable
fun albumTitle(album: Album): String =
    when (album.kind) {
        AlbumKind.Favorites -> stringResource(R.string.album_favorites)
        AlbumKind.Videos -> stringResource(R.string.album_videos)
        AlbumKind.Folder -> album.name.ifEmpty { stringResource(R.string.album_unknown) }
    }
