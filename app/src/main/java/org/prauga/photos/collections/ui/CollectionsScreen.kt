// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.collections.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.prauga.pvot.designsystem.effects.pvotPressScale
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import java.text.NumberFormat
import org.prauga.photos.R
import org.prauga.photos.common.ui.LargeTitleHeader
import org.prauga.photos.common.ui.albumTitle
import org.prauga.photos.common.ui.stableNavigationBars
import org.prauga.photos.common.ui.stableStatusBars
import org.prauga.photos.common.ui.thumbnailRequest
import org.prauga.photos.media.model.Album

@Composable
fun CollectionsScreen(onOpen: (Album) -> Unit, viewModel: CollectionsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CollectionsContent(state, onOpen)
}

@Composable
private fun CollectionsContent(state: CollectionsUiState, onOpen: (Album) -> Unit) {
    val top = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val bottom =
        WindowInsets.stableNavigationBars.asPaddingValues().calculateBottomPadding() +
            LocalPvotNavBarSizes.current.barHeight +
            24.dp

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top, bottom = bottom),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.mediaTypes.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.section_media_types)) }
                items(state.mediaTypes, key = { it.id }) { AlbumCard(it, onOpen) }
            }
            if (state.folders.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.section_albums)) }
                items(state.folders, key = { it.id }) { AlbumCard(it, onOpen) }
            }
        }
        LargeTitleHeader(title = stringResource(R.string.tab_collections), subtitle = "", overMedia = false)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AlbumCard(album: Album, onOpen: (Album) -> Unit) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val title = albumTitle(album)
    Column(
        Modifier.pvotPressScale(interaction).clickable(interactionSource = interaction, indication = null) {
            onOpen(album)
        }
    ) {
        AsyncImage(
            model = remember(album.cover.cacheKey) { thumbnailRequest(context, album.cover) },
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text =
                pluralStringResource(R.plurals.item_count, album.count, NumberFormat.getIntegerInstance().format(album.count)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
