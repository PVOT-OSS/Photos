// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.common

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import org.prauga.photos.data.MediaItem

/** Thumbnail edge in pixels: sharp at three columns on a 1080 px wide phone. */
const val ThumbnailSize: Int = 384

fun thumbnailKey(item: MediaItem): String = "thumb:${item.cacheKey}"

/** The grid's thumbnail request; the viewer reuses its memory-cache entry as a placeholder. */
fun thumbnailRequest(context: Context, item: MediaItem): ImageRequest =
    ImageRequest.Builder(context)
        .data(item.uri)
        .size(ThumbnailSize)
        .scale(Scale.FILL)
        .precision(Precision.INEXACT)
        .memoryCacheKey(thumbnailKey(item))
        .build()

/**
 * Where each visible grid tile is on screen, so the viewer can open from and close onto it.
 *
 * Bounds are plain fields written as tiles are placed: reading them must not recompose anything.
 */
class TileRegistry {
    private val bounds = HashMap<Long, Rect>()

    /** The tile the viewer is showing, drawn empty in the grid while the viewer covers it. */
    var hiddenId: Long? by mutableStateOf(null)

    /** Scrolls the grid so this item's tile is on screen; set by the grid on screen. */
    var reveal: (suspend (MediaItem) -> Unit)? = null

    fun place(id: Long, rect: Rect) {
        bounds[id] = rect
    }

    fun remove(id: Long) {
        bounds.remove(id)
    }

    fun boundsOf(id: Long): Rect? = bounds[id]
}

private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val dayLong = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun local(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

fun formatMonthYear(ms: Long): String = monthYear.format(local(ms))

fun formatDayLong(ms: Long): String = dayLong.format(local(ms))

fun formatTime(ms: Long): String = time.format(local(ms))

/** "Today", "Yesterday" or the date, for the viewer's title. */
fun formatRelativeDay(ms: Long): String =
    when {
        DateUtils.isToday(ms) -> "Today"
        DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS) -> "Yesterday"
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(local(ms))
    }

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatSize(bytes: Long): String =
    when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30).toDouble())
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20).toDouble())
        else -> "%d KB".format(bytes / 1024)
    }
