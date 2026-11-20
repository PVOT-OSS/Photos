// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.album.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotGlassIconButton
import com.prauga.pvot.designsystem.effects.pvotGlassBackdrop
import com.prauga.pvot.designsystem.effects.rememberPvotGlassBackdrop
import org.prauga.photos.R
import org.prauga.photos.common.ui.GridCountFooter
import org.prauga.photos.common.ui.LargeTitleHeader
import org.prauga.photos.common.ui.MediaGrid
import org.prauga.photos.common.ui.TileRegistry
import org.prauga.photos.common.ui.albumTitle
import org.prauga.photos.common.ui.largeTitleClearance
import org.prauga.photos.common.ui.scrolledUnderTop
import org.prauga.photos.common.ui.stableNavigationBars
import org.prauga.photos.common.ui.stableStatusBars
import org.prauga.photos.media.model.Album
import org.prauga.photos.media.model.MediaItem

/** [onBack] is null in a pane beside Collections, where there is nothing to go back to. */
@Composable
fun AlbumScreen(
    albumId: String,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    onBack: (() -> Unit)?,
    bottomInset: Dp = 0.dp,
    viewModel: AlbumViewModel =
        hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(key = "album:$albumId") { it.create(albumId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val album = state.album ?: return
    AlbumContent(album, registry, onOpen, onBack, bottomInset)
}

@Composable
private fun AlbumContent(
    album: Album,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    onBack: (() -> Unit)?,
    bottomInset: Dp,
) {
    val gridState = remember(album.id) { LazyGridState(firstVisibleItemIndex = album.items.size) }
    val backdrop = rememberPvotGlassBackdrop()
    val backRoom = if (onBack != null) 52.dp else 0.dp
    // In a pane the status bar spans both panes in one colour of icons: keep photos out from under it.
    val inPane = onBack == null
    val overMedia = gridState.scrolledUnderTop()
    val statusBar = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding()
    val top = (if (inPane) 0.dp else statusBar) + largeTitleClearance() + backRoom
    val bottom =
        maxOf(bottomInset, WindowInsets.stableNavigationBars.asPaddingValues().calculateBottomPadding()) + 24.dp
    val sides = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    Box(Modifier.fillMaxSize().padding(top = if (inPane) statusBar else 0.dp).clipToBounds()) {
        Box(Modifier.fillMaxSize().pvotGlassBackdrop(backdrop).background(MaterialTheme.colorScheme.background)) {
            MediaGrid(
                items = album.items,
                state = gridState,
                registry = registry,
                onOpen = onOpen,
                contentPadding = PaddingValues(top = top, bottom = bottom),
                footer = { GridCountFooter(album.items) },
            )
            LargeTitleHeader(
                title = albumTitle(album),
                subtitle = "",
                modifier = Modifier.align(Alignment.TopCenter),
                overMedia = overMedia,
                topInset = backRoom,
                tintStatusBar = !inPane,
                belowStatusBar = inPane,
            )
        }
        if (onBack != null) {
            PvotGlassIconButton(
                onClick = onBack,
                backdrop = backdrop,
                contentColor =
                    if (overMedia) colorResource(R.color.media_overlay_content) else MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.windowInsetsPadding(WindowInsets.stableStatusBars)
                        .windowInsetsPadding(sides)
                        .padding(start = 16.dp, top = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.action_back),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** The pane beside Collections before an album is picked. */
@Composable
fun AlbumPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.album_pick),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
