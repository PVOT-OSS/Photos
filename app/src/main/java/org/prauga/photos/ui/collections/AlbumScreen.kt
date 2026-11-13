// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotGlassIconButton
import com.prauga.pvot.designsystem.effects.pvotGlassBackdrop
import com.prauga.pvot.designsystem.effects.rememberPvotGlassBackdrop
import org.prauga.photos.R
import org.prauga.photos.data.Album
import org.prauga.photos.data.MediaItem
import org.prauga.photos.ui.common.TileRegistry
import org.prauga.photos.ui.library.GridCountFooter
import org.prauga.photos.ui.library.LargeTitleHeader
import org.prauga.photos.ui.library.MediaGrid
import org.prauga.photos.ui.library.countText

/** One album as a grid, with a glass back button over its title. */
@Composable
fun AlbumScreen(
    album: Album,
    gridState: LazyGridState,
    registry: TileRegistry,
    onOpen: (MediaItem) -> Unit,
    onBack: () -> Unit,
) {
    val backdrop = rememberPvotGlassBackdrop()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 120.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .pvotGlassBackdrop(backdrop)
                .background(MaterialTheme.colorScheme.background)
        ) {
            MediaGrid(
                items = album.items,
                state = gridState,
                registry = registry,
                onOpen = onOpen,
                contentPadding = PaddingValues(top = top, bottom = bottom),
                footer = { GridCountFooter(countText(album.items)) },
            )
            LargeTitleHeader(
                title = album.name,
                subtitle = "",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 52.dp),
            )
        }
        PvotGlassIconButton(
            onClick = onBack,
            backdrop = backdrop,
            contentColor = Color.White,
            modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.action_back),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Remembers an album grid's state, opened at its newest row. */
@Composable
fun rememberAlbumGridState(album: Album): LazyGridState =
    remember(album.id) { LazyGridState(firstVisibleItemIndex = album.items.size) }
