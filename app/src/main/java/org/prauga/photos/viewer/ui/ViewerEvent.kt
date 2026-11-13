// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import org.prauga.photos.media.model.MediaItem

sealed interface ViewerEvent {
    data class DetailsRequested(val item: MediaItem) : ViewerEvent
}
