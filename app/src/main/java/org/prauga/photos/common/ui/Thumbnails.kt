// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import android.content.Context
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import org.prauga.photos.media.model.MediaItem

/** Sharp at three columns on a 1080 px wide phone. */
private const val ThumbnailSize = 384

fun thumbnailKey(item: MediaItem): String = "thumb:${item.cacheKey}"

/** The viewer reuses this request's memory-cache entry as its placeholder, so keep the key stable. */
fun thumbnailRequest(context: Context, item: MediaItem): ImageRequest =
    ImageRequest.Builder(context)
        .data(item.uri)
        .size(ThumbnailSize)
        .scale(Scale.FILL)
        .precision(Precision.INEXACT)
        .memoryCacheKey(thumbnailKey(item))
        .build()
