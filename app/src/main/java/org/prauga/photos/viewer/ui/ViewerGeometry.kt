// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.util.lerp

internal fun fitRect(aspectRatio: Float, container: Size): Rect {
    val size =
        if (container.width / container.height > aspectRatio) {
            Size(container.height * aspectRatio, container.height)
        } else {
            Size(container.width, container.width / aspectRatio)
        }
    return Rect(Offset((container.width - size.width) / 2, (container.height - size.height) / 2), size)
}

internal fun Rect.scaledAroundCenter(scale: Float, offset: Offset = Offset.Zero): Rect {
    val size = Size(width * scale, height * scale)
    return Rect(center + offset - Offset(size.width / 2, size.height / 2), size)
}

internal fun lerpRect(from: Rect, to: Rect, fraction: Float): Rect =
    Rect(
        lerp(from.left, to.left, fraction),
        lerp(from.top, to.top, fraction),
        lerp(from.right, to.right, fraction),
        lerp(from.bottom, to.bottom, fraction),
    )
