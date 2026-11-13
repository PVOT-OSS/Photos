// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.model

import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: Long,
    val uri: String,
    val isVideo: Boolean,
    val mimeType: String,
    /** When it was taken, or else last modified, in epoch milliseconds. */
    val date: Long,
    val dateModified: Long,
    /** As displayed, rotation applied; 0 when MediaStore doesn't know. */
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val displayName: String,
    val size: Long,
    val isFavorite: Boolean,
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) width.toFloat() / height else 1f

    val cacheKey: String
        get() = "$id:$dateModified"
}
