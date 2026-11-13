// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.media.model

import androidx.compose.runtime.Immutable

@Immutable
data class CaptureDetails(
    val camera: String?,
    val aperture: String?,
    val exposureSeconds: Double?,
    val iso: String?,
    val focalLengthMm: Double?,
)
