// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import java.text.NumberFormat
import org.prauga.photos.R
import org.prauga.photos.data.MediaItem
import org.prauga.photos.ui.common.TileRegistry
import org.prauga.photos.ui.common.formatMonthYear

/** Every photo and video, newest at the bottom, under a large title that fades over them. */
@Composable
fun LibraryScreen(
    items: List<MediaItem>,
    gridState: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    banner: @Composable () -> Unit = {},
) {
    val subtitle by remember(items) {
        derivedStateOf {
            val visible = gridState.layoutInfo.visibleItemsInfo
            val index =
                (visible.firstOrNull { it.key != "footer" }?.index ?: gridState.firstVisibleItemIndex)
                    .coerceIn(0, (items.size - 1).coerceAtLeast(0))
            items.getOrNull(index)?.let { formatMonthYear(it.date) }.orEmpty()
        }
    }
    val bottom =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
            LocalPvotNavBarSizes.current.barHeight +
            24.dp
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp

    Box(Modifier.fillMaxSize()) {
        MediaGrid(
            items = items,
            state = gridState,
            registry = registry,
            onOpen = onOpen,
            contentPadding = PaddingValues(top = top, bottom = bottom),
            footer = { GridCountFooter(countText(items)) },
        )
        LargeTitleHeader(
            title = LocalContext.current.getString(R.string.tab_library),
            subtitle = subtitle,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 96.dp)) { banner() }
    }
}

/**
 * The big bold title and its subtitle: white over a dark fade when it sits on photos, the theme's
 * colours over the plain background when it doesn't.
 */
@Composable
fun LargeTitleHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    overMedia: Boolean = true,
) {
    val fade =
        if (overMedia) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.55f),
                0.6f to Color.Black.copy(alpha = 0.25f),
                1f to Color.Transparent,
            )
        } else {
            val background = MaterialTheme.colorScheme.background
            Brush.verticalGradient(0.7f to background, 1f to background.copy(alpha = 0f))
        }
    val color = if (overMedia) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .fillMaxWidth()
            .background(fade)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp)
    ) {
        val shadow = if (overMedia) Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 12f) else null
        Text(
            text = title,
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = shadow,
                ),
            color = color,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        shadow = shadow,
                    ),
                color = color.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
fun countText(items: List<MediaItem>): String {
    val resources = LocalContext.current.resources
    val videos = items.count { it.isVideo }
    val photos = items.size - videos
    val format = NumberFormat.getIntegerInstance()
    return listOfNotNull(
            photos
                .takeIf { it > 0 }
                ?.let { resources.getQuantityString(R.plurals.photo_count, it, format.format(it)) },
            videos
                .takeIf { it > 0 }
                ?.let { resources.getQuantityString(R.plurals.video_count, it, format.format(it)) },
        )
        .joinToString(", ")
}
