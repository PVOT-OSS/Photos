// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import org.prauga.photos.media.model.MediaItem

internal fun groupByMonth(items: List<MediaItem>, zone: ZoneId): List<MediaGroup> =
    group(items, zone) { it.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS) }

internal fun groupByYear(items: List<MediaItem>, zone: ZoneId): List<MediaGroup> =
    group(items, zone) { it.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS) }

/** [items] must be oldest first; a group's cover is its newest photo, or newest video if it has no photo. */
private fun group(
    items: List<MediaItem>,
    zone: ZoneId,
    startOf: (ZonedDateTime) -> ZonedDateTime,
): List<MediaGroup> {
    val groups = ArrayList<MediaGroup>()
    var groupStart = Long.MIN_VALUE
    var firstIndex = 0
    items.forEachIndexed { index, item ->
        val start = startOf(Instant.ofEpochMilli(item.date).atZone(zone)).toInstant().toEpochMilli()
        if (start != groupStart) {
            if (index > 0) groups += groupOf(items, groupStart, firstIndex, index)
            groupStart = start
            firstIndex = index
        }
    }
    if (items.isNotEmpty()) groups += groupOf(items, groupStart, firstIndex, items.size)
    return groups
}

private fun groupOf(items: List<MediaItem>, start: Long, from: Int, until: Int): MediaGroup {
    val members = items.subList(from, until)
    val cover = members.lastOrNull { !it.isVideo } ?: members.last()
    return MediaGroup(start, cover, until - from, from)
}
