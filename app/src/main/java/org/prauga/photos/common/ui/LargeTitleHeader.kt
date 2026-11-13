// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.prauga.photos.R

@Composable
fun LargeTitleHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    overMedia: Boolean = true,
    topInset: Dp = 0.dp,
) {
    val scrim = colorResource(R.color.media_scrim)
    val background = MaterialTheme.colorScheme.background
    val fade =
        if (overMedia) {
            Brush.verticalGradient(
                0f to scrim.copy(alpha = 0.55f),
                0.6f to scrim.copy(alpha = 0.25f),
                1f to scrim.copy(alpha = 0f),
            )
        } else {
            Brush.verticalGradient(0.7f to background, 1f to background.copy(alpha = 0f))
        }
    val color = if (overMedia) colorResource(R.color.media_overlay_content) else MaterialTheme.colorScheme.onSurface
    val shadow = if (overMedia) Shadow(scrim.copy(alpha = 0.35f), blurRadius = 12f) else null
    Column(
        modifier
            .fillMaxWidth()
            .background(fade)
            .windowInsetsPadding(WindowInsets.stableStatusBars)
            .padding(start = 20.dp, end = 20.dp, top = 8.dp + topInset, bottom = 28.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, shadow = shadow),
            color = color,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, shadow = shadow),
                color = color.copy(alpha = 0.85f),
            )
        }
    }
}
