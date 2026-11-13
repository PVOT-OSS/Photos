// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.data

import android.net.Uri
import androidx.compose.runtime.Immutable

/** One photo or video in MediaStore. */
@Immutable
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val mimeType: String,
    /** When it was taken, or else last modified, in epoch milliseconds. */
    val date: Long,
    val dateModified: Long,
    /** Width and height as displayed, rotation applied; 0 when MediaStore doesn't know. */
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val displayName: String,
    val size: Long,
    val isFavorite: Boolean,
) {
    /** Width over height, 1 when the size is unknown. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 1f

    /** A key that changes when the file does, for image caches. */
    val cacheKey: String
        get() = "$id:$dateModified"
}

/** A MediaStore bucket (a folder such as Camera or Screenshots) or a virtual album. */
@Immutable
data class Album(
    val id: String,
    val name: String,
    val cover: MediaItem,
    val count: Int,
    val items: List<MediaItem>,
)
