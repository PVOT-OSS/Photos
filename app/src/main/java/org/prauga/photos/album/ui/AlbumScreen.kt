// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.album.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import org.prauga.photos.common.ui.stableNavigationBars
import org.prauga.photos.common.ui.stableStatusBars
import org.prauga.photos.media.model.Album
import org.prauga.photos.media.model.MediaItem

@Composable
fun AlbumScreen(
    albumId: String,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumViewModel =
        hiltViewModel<AlbumViewModel, AlbumViewModel.Factory>(key = "album:$albumId") { it.create(albumId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val album = state.album ?: return
    AlbumContent(album, registry, onOpen, onBack)
}

@Composable
private fun AlbumContent(album: Album, registry: TileRegistry, onOpen: (MediaItem) -> Unit, onBack: () -> Unit) {
    val gridState = remember(album.id) { LazyGridState(firstVisibleItemIndex = album.items.size) }
    val backdrop = rememberPvotGlassBackdrop()
    val top = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding() + 120.dp
    val bottom = WindowInsets.stableNavigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    Box(Modifier.fillMaxSize()) {
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
                topInset = 52.dp,
            )
        }
        PvotGlassIconButton(
            onClick = onBack,
            backdrop = backdrop,
            contentColor = colorResource(R.color.media_overlay_content),
            modifier = Modifier.windowInsetsPadding(WindowInsets.stableStatusBars).padding(start = 16.dp, top = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.action_back),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
