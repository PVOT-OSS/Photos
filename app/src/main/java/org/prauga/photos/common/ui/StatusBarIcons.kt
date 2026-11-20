// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat

/** White status bar icons while [overMedia], readable over the photos under a title's dark fade. */
@Composable
fun StatusBarOverMedia(overMedia: Boolean) {
    val window = LocalActivity.current?.window ?: return
    // Restored to the theme's own icons, not to what was there: two headers can overlap in a crossfade.
    val themeDarkIcons = MaterialTheme.colorScheme.background.luminance() > 0.5f
    DisposableEffect(window, overMedia, themeDarkIcons) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = themeDarkIcons && !overMedia
        onDispose { controller.isAppearanceLightStatusBars = themeDarkIcons }
    }
}
