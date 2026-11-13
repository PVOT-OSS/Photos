// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.photos.media.model.AlbumKind
import org.prauga.photos.media.model.MediaItem

class AlbumGroupingTest {

    private fun item(id: Long, date: Long, bucket: Long, video: Boolean = false, favorite: Boolean = false) =
        MediaItem(
            id = id,
            uri = "content://media/external/images/media/$id",
            isVideo = video,
            mimeType = if (video) "video/mp4" else "image/jpeg",
            date = date,
            dateModified = date,
            width = 4,
            height = 3,
            durationMs = 0,
            bucketId = bucket,
            bucketName = "bucket $bucket",
            displayName = "$id.jpg",
            size = 1,
            isFavorite = favorite,
        )

    @Test
    fun foldersAreOrderedByTheirNewestItem() {
        val items = listOf(item(1, 10, bucket = 1), item(2, 20, bucket = 2), item(3, 30, bucket = 1))

        val folders = folderAlbums(items)

        assertEquals(listOf("bucket:1", "bucket:2"), folders.map { it.id })
        assertEquals(3L, folders.first().cover.id)
        assertEquals(2, folders.first().count)
    }

    @Test
    fun mediaTypesAppearOnlyWhenTheyHaveItems() {
        assertTrue(mediaTypeAlbums(listOf(item(1, 10, bucket = 1))).isEmpty())

        val types = mediaTypeAlbums(listOf(item(1, 10, 1, favorite = true), item(2, 20, 1, video = true)))

        assertEquals(listOf(AlbumKind.Favorites, AlbumKind.Videos), types.map { it.kind })
        assertEquals(listOf(1L), types[0].items.map { it.id })
        assertEquals(listOf(2L), types[1].items.map { it.id })
    }
}
