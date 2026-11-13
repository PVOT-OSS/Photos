// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos

import org.prauga.photos.access.ui.MediaAccess

sealed interface PhotosEvent {
    data class AccessChanged(val access: MediaAccess) : PhotosEvent
}
