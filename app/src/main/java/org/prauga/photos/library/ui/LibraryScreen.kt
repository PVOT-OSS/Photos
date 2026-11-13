// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.list.PvotFastScrollSection
import com.prauga.pvot.designsystem.components.list.PvotFastScroller
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import org.prauga.photos.R
import org.prauga.photos.common.format.formatMonthYear
import org.prauga.photos.common.format.formatShortMonthYear
import org.prauga.photos.common.format.formatYear
import org.prauga.photos.common.ui.FooterKey
import org.prauga.photos.common.ui.GridCountFooter
import org.prauga.photos.common.ui.LargeTitleHeader
import org.prauga.photos.common.ui.MediaGrid
import org.prauga.photos.common.ui.TileRegistry
import org.prauga.photos.common.ui.stableNavigationBars
import org.prauga.photos.common.ui.stableStatusBars
import org.prauga.photos.media.model.MediaItem

private val ModeBarClearance = 64.dp

@Composable
fun LibraryScreen(
    mode: LibraryMode,
    onModeChange: (LibraryMode) -> Unit,
    gridState: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    banner: @Composable () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.loaded) return
    LibraryContent(state, mode, onModeChange, gridState, registry, onOpen, banner)
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    mode: LibraryMode,
    onModeChange: (LibraryMode) -> Unit,
    gridState: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    banner: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Created once the library has loaded: far past the end, they open on the newest row.
    val months = rememberSaveable(saver = LazyListState.Saver) { LazyListState(Int.MAX_VALUE / 2) }
    val years = rememberSaveable(saver = LazyListState.Saver) { LazyListState(Int.MAX_VALUE / 2) }
    val items = state.items
    val subtitle by
        remember(items) {
            derivedStateOf {
                val index =
                    gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key != FooterKey && it.offset.y >= 0 }?.index
                        ?: gridState.firstVisibleItemIndex
                items.getOrNull(index.coerceAtMost(items.lastIndex))?.let { formatMonthYear(context, it.date) }.orEmpty()
            }
        }
    val top = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding() + 72.dp
    val bottom: Dp =
        WindowInsets.stableNavigationBars.asPaddingValues().calculateBottomPadding() +
            LocalPvotNavBarSizes.current.barHeight +
            ModeBarClearance +
            24.dp
    val padding = PaddingValues(top = top, bottom = bottom)
    val sections =
        remember(state.months) {
            state.months.map { PvotFastScrollSection(formatShortMonthYear(context, it.start), it.firstIndex) }
        }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = mode, animationSpec = tween(220), label = "libraryMode") { shown ->
            when (shown) {
                LibraryMode.All ->
                    Box(Modifier.fillMaxSize()) {
                        MediaGrid(
                            items = items,
                            state = gridState,
                            registry = registry,
                            onOpen = onOpen,
                            contentPadding = padding,
                            footer = if (items.isNotEmpty()) ({ GridCountFooter(items) }) else null,
                        )
                        PvotFastScroller(
                            state = gridState,
                            sections = sections,
                            modifier = Modifier.align(Alignment.TopEnd).padding(padding),
                        )
                    }
                LibraryMode.Months ->
                    MediaGroupList(
                        groups = state.months,
                        state = months,
                        contentPadding = padding,
                        label = { formatMonthYear(context, it.start) },
                        onPick = {
                            gridState.requestScrollToItem(it.firstIndex)
                            onModeChange(LibraryMode.All)
                        },
                    )
                LibraryMode.Years ->
                    MediaGroupList(
                        groups = state.years,
                        state = years,
                        contentPadding = padding,
                        label = { formatYear(it.start) },
                        onPick = { year ->
                            val month = state.months.indexOfLast { it.firstIndex < year.firstIndex + year.count }
                            if (month >= 0) months.requestScrollToItem(month)
                            onModeChange(LibraryMode.Months)
                        },
                    )
            }
        }
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        LargeTitleHeader(
            title = stringResource(R.string.tab_library),
            subtitle = if (mode == LibraryMode.All) subtitle else "",
            overMedia = mode == LibraryMode.All && items.isNotEmpty(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Box(
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.stableStatusBars).padding(top = 96.dp)
        ) {
            banner()
        }
    }
}
