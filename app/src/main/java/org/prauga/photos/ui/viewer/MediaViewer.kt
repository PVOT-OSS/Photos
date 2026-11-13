// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.viewer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.prauga.pvot.designsystem.effects.pvotGlassBackdrop
import com.prauga.pvot.designsystem.effects.rememberPvotGlassBackdrop
import kotlin.math.abs
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.prauga.photos.data.MediaItem
import org.prauga.photos.ui.common.TileRegistry
import org.prauga.photos.ui.common.thumbnailKey
import org.prauga.photos.ui.common.thumbnailRequest

private enum class Phase {
    Opening,
    Open,
    Closing,
}

/** Springs for the zoom out of and back into the grid. */
private val OpenSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 420f)
private val CloseSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 480f)
private val SnapBackSpring =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f, visibilityThreshold = Offset(1f, 1f))

/**
 * The full-screen viewer over the grid.
 *
 * It grows out of the tapped tile, pages through [items], zooms with pinch and double tap, and
 * closes back onto the tile of whatever it shows: by back, or by dragging the photo down, which
 * uncovers the grid as it goes.
 */
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    startIndex: Int,
    registry: TileRegistry,
    actions: MediaActions,
    onClosed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
    val current = items.getOrNull(pager.currentPage)
    val currentItems by rememberUpdatedState(items)

    var phase by remember { mutableStateOf(Phase.Opening) }
    val progress = remember { Animatable(0f) }
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var chrome by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }

    // What the hero animation draws: the item and the rectangles it travels between.
    val startItem = remember { items.getOrNull(startIndex) }
    var heroItem by remember { mutableStateOf(startItem) }
    var heroFrom by remember { mutableStateOf<Rect?>(startItem?.let { registry.boundsOf(it.id) }) }
    var heroTo by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(current?.id) { registry.hiddenId = current?.id }
    DisposableEffect(Unit) { onDispose { registry.hiddenId = null } }
    LaunchedEffect(items.isEmpty()) { if (items.isEmpty()) onClosed() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val container = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val density = LocalDensity.current
        val dismissDistance = with(density) { 160.dp.toPx() }
        val dragFraction = (abs(drag.value.y) / (container.height / 2)).coerceIn(0f, 1f)
        val dragScale = 1f - 0.3f * dragFraction

        fun close() {
            if (phase == Phase.Closing) return
            val item = current ?: return onClosed()
            scope.launch {
                registry.reveal?.invoke(item)
                val fit = fitRect(item, container)
                heroItem = item
                // Where the photo is now: centred, moved by the drag, shrunk by it.
                val size = Size(fit.width * dragScale, fit.height * dragScale)
                heroFrom = Rect(fit.center + drag.value - Offset(size.width / 2, size.height / 2), size)
                heroTo = registry.boundsOf(item.id)
                info = false
                phase = Phase.Closing
                progress.snapTo(0f)
                progress.animateTo(1f, CloseSpring)
                onClosed()
            }
        }

        LaunchedEffect(Unit) {
            heroTo = startItem?.let { fitRect(it, container) }
            progress.animateTo(1f, OpenSpring)
            phase = Phase.Open
        }
        BackHandler(enabled = phase != Phase.Closing) { if (info) info = false else close() }

        val chromeVisible = phase == Phase.Open && chrome && abs(drag.value.y) < 1f
        val backgroundAlpha =
            when (phase) {
                Phase.Opening -> progress.value
                Phase.Open -> 1f - dragFraction
                Phase.Closing -> (1f - progress.value) * (1f - 0.9f * dragFraction)
            }
        val background by
            animateColorAsState(
                if (chrome) MaterialTheme.colorScheme.background else Color.Black,
                label = "viewer background",
            )
        val backdrop = rememberPvotGlassBackdrop()

        // One backdrop for the glass chrome: the background and the photo under it.
        Box(Modifier.fillMaxSize().pvotGlassBackdrop(backdrop)) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = backgroundAlpha }.background(background))

            // The pager composes from the start, hidden, so the full image loads while the hero
            // flies.
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        alpha = if (phase == Phase.Open) 1f else 0f
                        translationX = drag.value.x
                        translationY = drag.value.y
                        scaleX = dragScale
                        scaleY = dragScale
                    }
                    .dismissGesture(
                        enabled = { phase == Phase.Open && !zoomed },
                        onDrag = { scope.launch { drag.snapTo(it) } },
                        onRelease = { offset, velocity ->
                            if (offset.y > dismissDistance || velocity.y > 1800f) close()
                            else scope.launch { drag.animateTo(Offset.Zero, SnapBackSpring) }
                        },
                    )
            ) {
                ViewerPager(
                    pager = pager,
                    items = items,
                    isOpen = phase == Phase.Open,
                    chromeVisible = chromeVisible,
                    onTap = { chrome = !chrome },
                    onZoomedChange = { zoomed = it },
                )
            }
        }

        if (phase != Phase.Open) {
            heroItem?.let { item ->
                val to = heroTo ?: fitRect(item, container)
                val from = heroFrom ?: centeredShrink(to)
                val t = progress.value
                val rect = lerpRect(from, to, t)
                val fade = if (phase == Phase.Closing && heroTo == null) 1f - t else 1f
                HeroImage(item, rect, fade)
            }
        }

        ViewerChrome(
            item = current,
            visible = chromeVisible,
            infoVisible = info,
            backdrop = backdrop,
            actions = actions,
            onBack = ::close,
            onInfo = { info = !info },
        )
    }

    // Keep the pager on the same item when the list changes under it (a delete, a new photo).
    LaunchedEffect(pager) {
        var lastId = current?.id
        snapshotFlow { currentItems }
            .collect { list ->
                val index = list.indexOfFirst { it.id == lastId }
                if (index >= 0 && index != pager.currentPage) pager.scrollToPage(index)
                lastId = list.getOrNull(pager.currentPage)?.id
            }
    }
}

@Composable
private fun ViewerPager(
    pager: PagerState,
    items: List<MediaItem>,
    isOpen: Boolean,
    chromeVisible: Boolean,
    onTap: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
) {
    HorizontalPager(
        state = pager,
        beyondViewportPageCount = 1,
        pageSpacing = 16.dp,
        key = { items[it].id },
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val item = items[page]
        val isCurrent = page == pager.currentPage
        if (item.isVideo) {
            VideoPage(
                item = item,
                playing = isOpen && isCurrent && !pager.isScrollInProgress,
                chromeVisible = chromeVisible,
                onTap = onTap,
            )
        } else {
            PhotoPage(item, isCurrent, onTap, onZoomedChange)
        }
    }
}

@Composable
private fun PhotoPage(
    item: MediaItem,
    isCurrent: Boolean,
    onTap: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 6f)))
    val request =
        remember(item.cacheKey) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .placeholderMemoryCacheKey(thumbnailKey(item))
                .memoryCacheKey("full:${item.cacheKey}")
                .build()
        }
    if (isCurrent) {
        LaunchedEffect(state) {
            snapshotFlow { (state.zoomableState.zoomFraction ?: 0f) > 0.01f }
                .collect { onZoomedChange(it) }
        }
    } else {
        // A page swiped away comes back unzoomed.
        LaunchedEffect(Unit) { state.zoomableState.resetZoom(withAnimation = false) }
    }
    ZoomableAsyncImage(
        model = request,
        contentDescription = item.displayName,
        state = state,
        modifier = Modifier.fillMaxSize(),
        onClick = { onTap() },
    )
}

@Composable
private fun HeroImage(item: MediaItem, rect: Rect, alpha: Float) {
    val context = LocalContext.current
    val density = LocalDensity.current
    AsyncImage(
        model = remember(item.cacheKey) { thumbnailRequest(context, item) },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            Modifier.offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .graphicsLayer { this.alpha = alpha },
    )
}

/** Where [item] sits when shown whole in [container]. */
private fun fitRect(item: MediaItem, container: Size): Rect {
    val ratio = item.aspectRatio
    val width: Float
    val height: Float
    if (container.width / container.height > ratio) {
        height = container.height
        width = height * ratio
    } else {
        width = container.width
        height = width / ratio
    }
    return Rect(
        Offset((container.width - width) / 2, (container.height - height) / 2),
        Size(width, height),
    )
}

private fun centeredShrink(rect: Rect): Rect {
    val w = rect.width * 0.85f
    val h = rect.height * 0.85f
    return Rect(rect.center - Offset(w / 2, h / 2), Size(w, h))
}

private fun lerpRect(a: Rect, b: Rect, t: Float): Rect =
    Rect(lerp(a.left, b.left, t), lerp(a.top, b.top, t), lerp(a.right, b.right, t), lerp(a.bottom, b.bottom, t))

/**
 * Takes over a mostly-downward one-finger drag before the pager or the zoomable see it, and leaves
 * every other gesture (sideways paging, pinch, taps) to them.
 */
private fun Modifier.dismissGesture(
    enabled: () -> Boolean,
    onDrag: (Offset) -> Unit,
    onRelease: (offset: Offset, velocity: Offset) -> Unit,
): Modifier = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!enabled()) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        var offset = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } > 1 && !dragging) return@awaitEachGesture
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            if (!change.pressed) {
                if (dragging) {
                    val velocity = tracker.calculateVelocity()
                    onRelease(offset, Offset(velocity.x, velocity.y))
                }
                return@awaitEachGesture
            }
            tracker.addPosition(change.uptimeMillis, change.position)
            val total = change.position - down.position
            if (!dragging) {
                if (total.getDistance() > slop) {
                    if (total.y > 0 && abs(total.y) > abs(total.x) * 1.3f) dragging = true
                    else return@awaitEachGesture
                }
            }
            if (dragging) {
                change.consume()
                offset = Offset(total.x * 0.6f, total.y)
                onDrag(offset)
            }
        }
    }
}
