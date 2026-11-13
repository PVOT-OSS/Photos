// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.data

import org.prauga.photos.media.model.Album
import org.prauga.photos.media.model.AlbumKind
import org.prauga.photos.media.model.MediaItem

internal const val FavoritesAlbumId = "favorites"
internal const val VideosAlbumId = "videos"

internal fun folderAlbums(items: List<MediaItem>): List<Album> =
    items
        .groupBy { it.bucketId }
        .map { (bucket, list) -> Album("bucket:$bucket", AlbumKind.Folder, list.last().bucketName, list) }
        .sortedByDescending { it.cover.date }

internal fun mediaTypeAlbums(items: List<MediaItem>): List<Album> =
    listOfNotNull(
        items
            .filter { it.isFavorite }
            .takeIf { it.isNotEmpty() }
            ?.let { Album(FavoritesAlbumId, AlbumKind.Favorites, "", it) },
        items
            .filter { it.isVideo }
            .takeIf { it.isNotEmpty() }
            ?.let { Album(VideosAlbumId, AlbumKind.Videos, "", it) },
    )
