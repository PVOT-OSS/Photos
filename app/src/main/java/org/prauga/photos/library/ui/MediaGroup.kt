// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.compose.runtime.Immutable
import org.prauga.photos.media.model.MediaItem

/** A month or a year of the library; [firstIndex] is where it starts in the library's item list. */
@Immutable
data class MediaGroup(val start: Long, val cover: MediaItem, val count: Int, val firstIndex: Int)
