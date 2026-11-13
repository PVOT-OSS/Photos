// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.navigation.PvotNavBar
import com.prauga.pvot.designsystem.navigation.PvotTabItem
import org.prauga.photos.access.ui.AccessScreen
import org.prauga.photos.access.ui.MediaAccess
import org.prauga.photos.access.ui.PartialAccessBanner
import org.prauga.photos.access.ui.rememberMediaAccessState
import org.prauga.photos.album.ui.AlbumScreen
import org.prauga.photos.collections.ui.CollectionsScreen
import org.prauga.photos.common.ui.TileRegistry
import org.prauga.photos.library.ui.LibraryScreen
import org.prauga.photos.viewer.ui.ViewerScreen
import org.prauga.photos.viewer.ui.ViewerViewModel

private data class ViewerRequest(val source: String, val startId: Long)

private val Tabs =
    listOf(
        PvotTabItem(R.drawable.ic_photo, R.string.tab_library, R.string.tab_library),
        PvotTabItem(R.drawable.ic_library_photo, R.string.tab_collections, R.string.tab_collections),
    )

private const val LibraryTab = 0

@Composable
fun PhotosApp(viewModel: PhotosViewModel = hiltViewModel()) {
    val access = rememberMediaAccessState()
    LaunchedEffect(access.access) { viewModel.onEvent(PhotosEvent.AccessChanged(access.access)) }
    if (access.access == MediaAccess.None) {
        AccessScreen(onAllow = access::request)
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(LibraryTab) }
    var albumId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<ViewerRequest?>(null) }
    val registry = remember { TileRegistry() }
    // Far past the end: the grid clamps it, so the library opens on its newest row.
    val libraryGrid = remember { LazyGridState(firstVisibleItemIndex = Int.MAX_VALUE / 2) }

    BackHandler(enabled = viewer == null && (albumId != null || tab != LibraryTab)) {
        if (albumId != null) albumId = null else tab = LibraryTab
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = albumId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "album",
        ) { openAlbum ->
            if (openAlbum != null) {
                AlbumScreen(
                    albumId = openAlbum,
                    registry = registry,
                    onOpen = { viewer = ViewerRequest(openAlbum, it.id) },
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
                        if (selected == LibraryTab) {
                            LibraryScreen(
                                gridState = libraryGrid,
                                registry = registry,
                                onOpen = { viewer = ViewerRequest(ViewerViewModel.LibrarySource, it.id) },
                                banner = {
                                    if (access.access == MediaAccess.Partial) PartialAccessBanner(onManage = access::request)
                                },
                            )
                        } else {
                            CollectionsScreen(onOpen = { albumId = it.id })
                        }
                    }
                }
            }
        }

        viewer?.let { request ->
            ViewerScreen(
                source = request.source,
                startId = request.startId,
                registry = registry,
                onClosed = { viewer = null },
            )
        }
    }
}
