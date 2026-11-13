// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.prauga.pvot.designsystem.effects.pvotGlassBackdrop
import com.prauga.pvot.designsystem.effects.rememberPvotGlassBackdrop
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import org.prauga.photos.R
import org.prauga.photos.common.ui.TileRegistry
import org.prauga.photos.common.ui.thumbnailKey
import org.prauga.photos.common.ui.thumbnailRequest
import org.prauga.photos.media.model.CaptureDetails
import org.prauga.photos.media.model.MediaItem

private enum class Phase {
    Opening,
    Open,
    Closing,
}

private val OpenSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 420f)
private val CloseSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 480f)
private val SnapBackSpring =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 500f, visibilityThreshold = Offset(1f, 1f))
private const val DismissVelocity = 1800f
private const val DragShrink = 0.3f
private const val MaxZoom = 6f
private const val BackDragShare = 0.25f

@Composable
fun ViewerScreen(
    source: String,
    startId: Long,
    registry: TileRegistry,
    onClosed: () -> Unit,
    viewModel: ViewerViewModel =
        hiltViewModel<ViewerViewModel, ViewerViewModel.Factory>(key = "viewer:$source") { it.create(source) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.loaded) return
    val start = remember { state.items.indexOfFirst { it.id == startId }.coerceAtLeast(0) }
    ViewerContent(
        items = state.items,
        startIndex = start,
        details = state.details,
        registry = registry,
        actions = rememberMediaActions(),
        onDetails = { viewModel.onEvent(ViewerEvent.DetailsRequested(it)) },
        onClosed = onClosed,
    )
}

@Composable
private fun ViewerContent(
    items: List<MediaItem>,
    startIndex: Int,
    details: Map<Long, CaptureDetails?>,
    registry: TileRegistry,
    actions: MediaActions,
    onDetails: (MediaItem) -> Unit,
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

    val startItem = remember { items.getOrNull(startIndex) }
    var heroItem by remember { mutableStateOf(startItem) }
    var heroFrom by remember { mutableStateOf(startItem?.let { registry.boundsOf(it.id) }) }
    var heroTo by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(current?.id) { registry.hiddenId = current?.id }
    LaunchedEffect(info, current?.id) { if (info) current?.let(onDetails) }
    DisposableEffect(Unit) { onDispose { registry.hiddenId = null } }
    LaunchedEffect(items.isEmpty()) { if (items.isEmpty()) onClosed() }
    LaunchedEffect(pager) {
        var lastId = current?.id
        snapshotFlow { currentItems }
            .collect { list ->
                val index = list.indexOfFirst { it.id == lastId }
                if (index >= 0 && index != pager.currentPage) pager.scrollToPage(index)
                lastId = list.getOrNull(pager.currentPage)?.id
            }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val container = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val dismissDistance = with(LocalDensity.current) { 160.dp.toPx() }
        val dragFraction = (abs(drag.value.y) / (container.height / 2)).coerceIn(0f, 1f)
        val dragScale = 1f - DragShrink * dragFraction

        fun close() {
            if (phase == Phase.Closing) return
            val item = current ?: return onClosed()
            scope.launch {
                registry.reveal?.invoke(item)
                heroItem = item
                heroFrom = fitRect(item.aspectRatio, container).scaledAroundCenter(dragScale, drag.value)
                heroTo = registry.boundsOf(item.id)
                info = false
                phase = Phase.Closing
                progress.snapTo(0f)
                progress.animateTo(1f, CloseSpring)
                onClosed()
            }
        }

        LaunchedEffect(Unit) {
            heroTo = startItem?.let { fitRect(it.aspectRatio, container) }
            progress.animateTo(1f, OpenSpring)
            phase = Phase.Open
        }
        PredictiveBackHandler(enabled = phase == Phase.Open && !info) { events ->
            try {
                events.collect { drag.snapTo(Offset(0f, it.progress * container.height * BackDragShare)) }
                close()
            } catch (e: CancellationException) {
                drag.animateTo(Offset.Zero, SnapBackSpring)
                throw e
            }
        }
        BackHandler(enabled = info) { info = false }
        ImmersiveSystemBars(immersive = phase == Phase.Open && !chrome)

        val chromeVisible = phase == Phase.Open && chrome && abs(drag.value.y) < 1f
        val backgroundAlpha =
            when (phase) {
                Phase.Opening -> progress.value
                Phase.Open -> 1f - dragFraction
                Phase.Closing -> (1f - progress.value) * (1f - dragFraction)
            }
        val background by
            animateColorAsState(
                if (chrome) MaterialTheme.colorScheme.background else colorResource(R.color.viewer_immersive_background),
                label = "viewerBackground",
            )
        val backdrop = rememberPvotGlassBackdrop()

        // One backdrop element: two elements marked with the same backdrop overwrite each other's recording.
        Box(Modifier.fillMaxSize().pvotGlassBackdrop(backdrop)) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = backgroundAlpha }.background(background))
            // Composed while hidden, so the full image loads during the opening animation.
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
                            if (offset.y > dismissDistance || velocity.y > DismissVelocity) close()
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
                val to = heroTo ?: fitRect(item.aspectRatio, container)
                val from = heroFrom ?: to.scaledAroundCenter(0.85f)
                val alpha = if (phase == Phase.Closing && heroTo == null) 1f - progress.value else 1f
                HeroImage(item, lerpRect(from, to, progress.value), alpha)
            }
        }

        ViewerChrome(
            item = current,
            visible = chromeVisible,
            infoVisible = info,
            details = current?.let { details[it.id] },
            backdrop = backdrop,
            actions = actions,
            onBack = ::close,
            onInfo = { info = !info },
        )
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
private fun PhotoPage(item: MediaItem, isCurrent: Boolean, onTap: () -> Unit, onZoomedChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val state = rememberZoomableImageState(rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = MaxZoom)))
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
            snapshotFlow { (state.zoomableState.zoomFraction ?: 0f) > 0.01f }.collect { onZoomedChange(it) }
        }
    } else {
        LaunchedEffect(Unit) { state.zoomableState.resetZoom(animationSpec = snap()) }
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

@Composable
private fun ImmersiveSystemBars(immersive: Boolean) {
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window, immersive) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (immersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
