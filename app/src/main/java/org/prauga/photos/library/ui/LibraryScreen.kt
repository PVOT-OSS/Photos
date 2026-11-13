// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import org.prauga.photos.R
import org.prauga.photos.common.format.formatMonthYear
import org.prauga.photos.common.ui.FooterKey
import org.prauga.photos.common.ui.GridCountFooter
import org.prauga.photos.common.ui.LargeTitleHeader
import org.prauga.photos.common.ui.MediaGrid
import org.prauga.photos.common.ui.TileRegistry
import org.prauga.photos.common.ui.stableNavigationBars
import org.prauga.photos.common.ui.stableStatusBars
import org.prauga.photos.media.model.MediaItem

@Composable
fun LibraryScreen(
    gridState: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    banner: @Composable () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryContent(state, gridState, registry, onOpen, banner)
}

@Composable
private fun LibraryContent(
    state: LibraryUiState,
    gridState: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    banner: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val items = state.items
    val subtitle by
        remember(items) {
            derivedStateOf {
                val index =
                    gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key != FooterKey }?.index
                        ?: gridState.firstVisibleItemIndex
                items.getOrNull(index.coerceAtMost(items.lastIndex))?.let { formatMonthYear(context, it.date) }.orEmpty()
            }
        }
    val bottom =
        WindowInsets.stableNavigationBars.asPaddingValues().calculateBottomPadding() +
            LocalPvotNavBarSizes.current.barHeight +
            24.dp
    val top = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding() + 72.dp

    Box(Modifier.fillMaxSize()) {
        MediaGrid(
            items = items,
            state = gridState,
            registry = registry,
            onOpen = onOpen,
            contentPadding = PaddingValues(top = top, bottom = bottom),
            footer = if (items.isNotEmpty()) ({ GridCountFooter(items) }) else null,
        )
        if (state.loaded && items.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        LargeTitleHeader(
            title = stringResource(R.string.tab_library),
            subtitle = subtitle,
            overMedia = items.isNotEmpty(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Box(Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.stableStatusBars).padding(top = 96.dp)) { banner() }
    }
}
