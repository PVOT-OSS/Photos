// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.prauga.photos.R
import org.prauga.photos.data.MediaItem
import org.prauga.photos.ui.common.TileRegistry
import org.prauga.photos.ui.common.formatDuration
import org.prauga.photos.ui.common.thumbnailRequest

/** Column counts a pinch steps through, densest last. */
val GridColumnSteps: List<Int> = listOf(1, 3, 5, 9)

/**
 * The square-tile photo grid: oldest first, pinch to change the column count, tiles that report
 * their bounds to [registry] so the viewer zooms out of them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    state: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    initialColumnStep: Int = 1,
    footer: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(initialColumnStep) }
    val columns = GridColumnSteps[step]
    val currentItems by rememberUpdatedState(items)

    // Lets the viewer bring a tile on screen before it closes onto it.
    SideEffect {
        registry.reveal = { item ->
            val index = currentItems.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                val visible = state.layoutInfo.visibleItemsInfo
                val fully =
                    visible.any {
                        it.index == index &&
                            it.offset.y >= state.layoutInfo.viewportStartOffset &&
                            it.offset.y + it.size.height <= state.layoutInfo.viewportEndOffset
                    }
                if (!fully) {
                    state.scrollToItem(index, -state.layoutInfo.viewportSize.height / 3)
                    withFrameNanos {}
                }
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
                // Pinch steps the column count; one finger is left to the grid's scrolling.
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
                                    zoom > 1.35f && step > 0 -> step - 1
                                    zoom < 0.74f && step < GridColumnSteps.lastIndex -> step + 1
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
                modifier =
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                        .onGloballyPositioned { registry.place(item.id, it.boundsInRoot()) }
                        .clickable { onOpen(item) },
                request = remember(item.cacheKey) { thumbnailRequest(context, item) },
            )
            DisposableEffect(item.id) { onDispose { registry.remove(item.id) } }
        }
        if (footer != null) {
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                footer()
            }
        }
    }
}

@Composable
private fun MediaTile(
    item: MediaItem,
    hidden: Boolean,
    showBadges: Boolean,
    request: coil3.request.ImageRequest,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
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
                    style = BadgeText,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
            if (item.isFavorite) {
                Icon(
                    painter = painterResource(R.drawable.ic_heart_filled),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(14.dp),
                )
            }
        }
    }
}

private val BadgeText =
    TextStyle(
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
    )

/** "1,204 Photos, 87 Videos" under the last row, as a full-width footer. */
@Composable
fun GridCountFooter(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
