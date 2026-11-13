// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.photos.media.model.MediaItem

class LibraryGroupingTest {

    private val zone: ZoneId = ZoneOffset.ofHours(5)

    private fun item(id: Long, at: LocalDateTime, video: Boolean = false): MediaItem {
        val date = at.atZone(zone).toInstant().toEpochMilli()
        return MediaItem(id, "content://$id", video, "image/jpeg", date, date, 1, 1, 0, 1, "b", "$id", 1, false)
    }

    private val items =
        listOf(
            item(1, LocalDateTime.of(2025, 12, 31, 23, 30)),
            item(2, LocalDateTime.of(2026, 1, 1, 0, 30)),
            item(3, LocalDateTime.of(2026, 1, 20, 9, 0)),
            item(4, LocalDateTime.of(2026, 3, 2, 9, 0)),
        )

    @Test
    fun monthsSplitAtLocalMidnightAndCoverWithTheNewest() {
        val months = groupByMonth(items, zone)

        assertEquals(listOf(1, 2, 1), months.map { it.count })
        assertEquals(listOf(0, 1, 3), months.map { it.firstIndex })
        assertEquals(listOf(1L, 3L, 4L), months.map { it.cover.id })
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0).atZone(zone).toInstant().toEpochMilli(), months[1].start)
    }

    @Test
    fun yearsGroupEveryMonthOfTheYear() {
        val years = groupByYear(items, zone)

        assertEquals(listOf(1, 3), years.map { it.count })
        assertEquals(listOf(1L, 4L), years.map { it.cover.id })
    }

    @Test
    fun coverSkipsVideosWhenTheGroupHasAPhoto() {
        val withVideo = items + item(5, LocalDateTime.of(2026, 3, 9, 9, 0), video = true)

        assertEquals(4L, groupByMonth(withVideo, zone).last().cover.id)
        assertEquals(6L, groupByMonth(listOf(item(6, LocalDateTime.of(2026, 3, 9, 9, 0), video = true)), zone)[0].cover.id)
    }

    @Test
    fun emptyLibraryHasNoGroups() {
        assertTrue(groupByMonth(emptyList(), zone).isEmpty())
    }
}
