// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.format

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun local(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

fun formatMonthYear(context: Context, ms: Long): String =
    DateUtils.formatDateTime(context, ms, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_MONTH_DAY or DateUtils.FORMAT_SHOW_YEAR)

fun formatYear(ms: Long): String = local(ms).year.toString()

fun formatDayLong(ms: Long): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).format(local(ms))

fun formatTime(ms: Long): String = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(local(ms))

fun formatRelativeDay(ms: Long): String =
    DateUtils.getRelativeTimeSpanString(ms, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()

fun formatDuration(ms: Long): String = DateUtils.formatElapsedTime(ms / 1000)

fun formatSize(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)
