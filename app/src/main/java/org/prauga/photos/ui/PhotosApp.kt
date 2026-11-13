// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.designsystem.components.PvotGlassSurface
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.navigation.PvotNavBar
import com.prauga.pvot.designsystem.navigation.PvotTabItem
import org.prauga.photos.R
import org.prauga.photos.data.Album
import org.prauga.photos.data.MediaItem
import org.prauga.photos.data.mediaTypeAlbums
import org.prauga.photos.ui.collections.AlbumScreen
import org.prauga.photos.ui.collections.CollectionsScreen
import org.prauga.photos.ui.collections.rememberAlbumGridState
import org.prauga.photos.ui.common.TileRegistry
import org.prauga.photos.ui.library.LibraryScreen
import org.prauga.photos.ui.permission.MediaAccess
import org.prauga.photos.ui.permission.rememberMediaAccessState
import org.prauga.photos.ui.viewer.MediaViewer
import org.prauga.photos.ui.viewer.rememberMediaActions

private const val LibrarySource = "library"

/** What the viewer shows: the list it pages through (the library or an album) and where it began. */
private data class ViewerRequest(val source: String, val startId: Long)

private val Tabs =
    listOf(
        PvotTabItem(R.drawable.ic_photo, R.string.tab_library, R.string.tab_library),
        PvotTabItem(R.drawable.ic_library_photo, R.string.tab_collections, R.string.tab_collections),
    )

@Composable
fun PhotosApp(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val access = rememberMediaAccessState()
    LaunchedEffect(access.access) { viewModel.setCanRead(access.access != MediaAccess.None) }
    if (access.access == MediaAccess.None) {
        PermissionScreen(onAllow = access::request)
        return
    }

    val media by viewModel.media.collectAsState()
    val buckets by viewModel.albums.collectAsState()
    val favoritesName = stringResource(R.string.album_favorites)
    val videosName = stringResource(R.string.album_videos)
    val items = media.orEmpty()
    val types = remember(items) { mediaTypeAlbums(items, favoritesName, videosName) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var albumId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<ViewerRequest?>(null) }
    val registry = remember { TileRegistry() }
    val actions = rememberMediaActions()

    fun albumById(id: String?): Album? = id?.let { key -> types.find { it.id == key } ?: buckets.find { it.id == key } }
    val openAlbum = albumById(albumId)

    // Opened at the newest row, once the library has loaded.
    val libraryGrid = remember(media != null) { LazyGridState(firstVisibleItemIndex = items.size) }

    BackHandler(enabled = viewer == null && (albumId != null || tab != 0)) {
        if (albumId != null) albumId = null else tab = 0
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = openAlbum,
            contentKey = { it?.id },
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "album",
        ) { album ->
            if (album != null) {
                AlbumScreen(
                    album = album,
                    gridState = rememberAlbumGridState(album),
                    registry = registry,
                    onOpen = { viewer = ViewerRequest(album.id, it.id) },
                    onBack = { albumId = null },
                )
            } else {
                PvotScreenScaffold(
                    bottomBar = {
                        PvotNavBar(
                            selectedTab = tab,
                            onTabClick = { tab = it },
                            tabs = Tabs,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                ) { _ ->
                    Crossfade(targetState = tab, animationSpec = tween(220), label = "tab") { selected ->
                        when (selected) {
                            0 ->
                                LibraryScreen(
                                    items = items,
                                    gridState = libraryGrid,
                                    registry = registry,
                                    onOpen = { viewer = ViewerRequest(LibrarySource, it.id) },
                                    banner = {
                                        if (access.access == MediaAccess.Partial) {
                                            PartialAccessBanner(onManage = access::request)
                                        }
                                    },
                                )
                            else -> CollectionsScreen(types, buckets) { albumId = it.id }
                        }
                    }
                }
            }
        }

        viewer?.let { request ->
            val list: List<MediaItem> =
                if (request.source == LibrarySource) items else albumById(request.source)?.items.orEmpty()
            val start = remember(request) { list.indexOfFirst { it.id == request.startId }.coerceAtLeast(0) }
            MediaViewer(
                items = list,
                startIndex = start,
                registry = registry,
                actions = actions,
                onClosed = { viewer = null },
            )
        }
    }
}

@Composable
private fun PermissionScreen(onAllow: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.permission_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PvotGlassSurface(
                onClick = onAllow,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.permission_allow),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PartialAccessBanner(onManage: () -> Unit) {
    PvotGlassSurface(contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.partial_access),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onManage) { Text(stringResource(R.string.partial_access_manage)) }
        }
    }
}
