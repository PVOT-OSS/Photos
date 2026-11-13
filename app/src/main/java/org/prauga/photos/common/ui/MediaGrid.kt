// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.text.NumberFormat
import kotlinx.coroutines.launch
import org.prauga.photos.R
import org.prauga.photos.common.format.formatDuration
import org.prauga.photos.media.model.MediaItem

private val ColumnSteps = listOf(1, 3, 5, 9)
private const val DefaultColumnStep = 1
private const val PinchInThreshold = 1.35f
private const val PinchOutThreshold = 0.74f

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    state: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    footer: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(DefaultColumnStep) }
    val columns = ColumnSteps[step]
    val currentItems by rememberUpdatedState(items)

    SideEffect {
        registry.reveal = { item ->
            val index = currentItems.indexOfFirst { it.id == item.id }
            if (index >= 0 && !state.isFullyVisible(index)) {
                state.scrollToItem(index, -state.layoutInfo.viewportSize.height / 3)
                withFrameNanos {}
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier =
            modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var zoom = 1f
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            zoom *= event.calculateZoom()
                            event.changes.forEach { it.consume() }
                            val next =
                                when {
                                    zoom > PinchInThreshold && step > 0 -> step - 1
                                    zoom < PinchOutThreshold && step < ColumnSteps.lastIndex -> step + 1
                                    else -> step
                                }
                            if (next != step) {
                                val anchor = state.firstVisibleItemIndex
                                step = next
                                zoom = 1f
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                scope.launch { state.scrollToItem(anchor) }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        items(items, key = { it.id }, contentType = { "tile" }) { item ->
            MediaTile(
                item = item,
                hidden = registry.hiddenId == item.id,
                showBadges = columns <= 5,
                request = remember(item.cacheKey) { thumbnailRequest(context, item) },
                modifier =
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                        .onGloballyPositioned { registry.place(item.id, it.boundsInRoot()) }
                        .clickable { onOpen(item) },
            )
            DisposableEffect(item.id) { onDispose { registry.remove(item.id) } }
        }
        if (footer != null) {
            item(key = FooterKey, span = { GridItemSpan(maxLineSpan) }, contentType = FooterKey) { footer() }
        }
    }
}

const val FooterKey: String = "footer"

private fun LazyGridState.isFullyVisible(index: Int): Boolean {
    val info = layoutInfo
    return info.visibleItemsInfo.any {
        it.index == index &&
            it.offset.y >= info.viewportStartOffset &&
            it.offset.y + it.size.height <= info.viewportEndOffset
    }
}

@Composable
private fun MediaTile(
    item: MediaItem,
    hidden: Boolean,
    showBadges: Boolean,
    request: ImageRequest,
    modifier: Modifier = Modifier,
) {
    val overlay = colorResource(R.color.media_overlay_content)
    val badgeStyle =
        MaterialTheme.typography.labelMedium.copy(
            color = overlay,
            fontWeight = FontWeight.SemiBold,
            shadow = Shadow(colorResource(R.color.media_scrim).copy(alpha = 0.6f), blurRadius = 6f),
        )
    Box(modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        AsyncImage(
            model = request,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (hidden) 0f else 1f },
        )
        if (showBadges && !hidden) {
            if (item.isVideo) {
                Text(
                    text = formatDuration(item.durationMs),
                    style = badgeStyle,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
            if (item.isFavorite) {
                Icon(
                    painter = painterResource(R.drawable.ic_heart_filled),
                    contentDescription = null,
                    tint = overlay,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(14.dp),
                )
            }
        }
    }
}

@Composable
fun GridCountFooter(items: List<MediaItem>, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            text = mediaCountText(items),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun mediaCountText(items: List<MediaItem>): String {
    val videos = items.count { it.isVideo }
    val photos = items.size - videos
    val format = NumberFormat.getIntegerInstance()
    val parts = buildList {
        if (photos > 0) add(pluralStringResource(R.plurals.photo_count, photos, format.format(photos)))
        if (videos > 0) add(pluralStringResource(R.plurals.video_count, videos, format.format(videos)))
    }
    return when (parts.size) {
        2 -> stringResource(R.string.count_pair, parts[0], parts[1])
        else -> parts.firstOrNull().orEmpty()
    }
}
