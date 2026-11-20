// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.prauga.pvot.designsystem.adaptive.PvotAdaptiveDefaults
import com.prauga.pvot.designsystem.adaptive.PvotTwoPane
import com.prauga.pvot.designsystem.adaptive.PvotWindowHeight
import com.prauga.pvot.designsystem.adaptive.rememberPvotWindowLayout
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import com.prauga.pvot.designsystem.navigation.PvotNavBar
import com.prauga.pvot.designsystem.navigation.PvotTabItem
import org.prauga.photos.access.ui.AccessScreen
import org.prauga.photos.access.ui.MediaAccess
import org.prauga.photos.access.ui.PartialAccessBanner
import org.prauga.photos.access.ui.rememberMediaAccessState
import org.prauga.photos.album.ui.AlbumPlaceholder
import org.prauga.photos.album.ui.AlbumScreen
import org.prauga.photos.collections.ui.CollectionsScreen
import org.prauga.photos.common.ui.TileRegistry
import org.prauga.photos.library.ui.LibraryMode
import org.prauga.photos.library.ui.LibraryModeBar
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

    val window = rememberPvotWindowLayout()
    val short = window.height == PvotWindowHeight.Compact
    var tab by rememberSaveable { mutableIntStateOf(LibraryTab) }
    var libraryMode by rememberSaveable { mutableStateOf(LibraryMode.All) }
    val bars = rememberCollapsingBarsState()
    val density = LocalDensity.current
    var bottomBarsHeight by remember { mutableStateOf(0.dp) }
    bars.homeIsTop = !(tab == LibraryTab && libraryMode == LibraryMode.All)
    LaunchedEffect(tab, libraryMode) { bars.expand() }
    var albumId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewer by remember { mutableStateOf<ViewerRequest?>(null) }
    val registry = remember { TileRegistry() }
    // Far past the end: the grid clamps it, so the library opens on its newest row.
    val libraryGrid = remember { LazyGridState(firstVisibleItemIndex = Int.MAX_VALUE / 2) }
    // Side by side, an album lives in the pane beside Collections and never covers the tabs; the
    // Library tab leaves it there for when Collections is back.
    val albumBeside = window.twoPane && tab != LibraryTab
    val albumFullScreen = albumId.takeUnless { window.twoPane }
    val albumShown = albumFullScreen != null || albumBeside && albumId != null

    BackHandler(enabled = viewer == null && (albumShown || tab != LibraryTab)) {
        if (albumShown) albumId = null else tab = LibraryTab
    }

    val navSizes = LocalPvotNavBarSizes.current
    CompositionLocalProvider(
        LocalPvotNavBarSizes provides if (short) navSizes.copy(barHeight = navSizes.compactBarHeight) else navSizes
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = albumFullScreen,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 4 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
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
                        // The library's photos run under a side camera cutout; its text keeps clear.
                        fullBleedContent = tab == LibraryTab,
                        bottomBar = {
                            BottomBars(
                                tab = tab,
                                onTab = {
                                    tab = it
                                    bars.expand()
                                },
                                libraryMode = libraryMode,
                                onLibraryMode = { libraryMode = it },
                                collapsed = bars.collapsed,
                                short = short,
                                // Measured only while expanded, so collapsing doesn't move the content under it.
                                modifier =
                                    Modifier.onSizeChanged {
                                        if (!bars.collapsed) bottomBarsHeight = with(density) { it.height.toDp() }
                                    },
                            )
                        }
                    ) { _ ->
                        Crossfade(
                            targetState = tab,
                            animationSpec = tween(220),
                            label = "tab",
                            modifier = Modifier.nestedScroll(bars.connection),
                        ) { selected ->
                            when {
                                selected == LibraryTab ->
                                    LibraryScreen(
                                        bottomInset = bottomBarsHeight,
                                        mode = libraryMode,
                                        onModeChange = { libraryMode = it },
                                        gridState = libraryGrid,
                                        registry = registry,
                                        onOpen = { viewer = ViewerRequest(ViewerViewModel.LibrarySource, it.id) },
                                        banner = {
                                            if (access.access == MediaAccess.Partial) {
                                                PartialAccessBanner(onManage = access::request)
                                            }
                                        },
                                    )
                                albumBeside ->
                                    PvotTwoPane(
                                        first = {
                                            CollectionsScreen(
                                                bottomInset = bottomBarsHeight,
                                                onOpen = { albumId = it.id },
                                                selectedId = albumId,
                                            )
                                        },
                                        second = {
                                            val open = albumId
                                            if (open == null) {
                                                AlbumPlaceholder()
                                            } else {
                                                AlbumScreen(
                                                    albumId = open,
                                                    registry = registry,
                                                    onOpen = { viewer = ViewerRequest(open, it.id) },
                                                    onBack = null,
                                                    bottomInset = bottomBarsHeight,
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                else -> CollectionsScreen(bottomInset = bottomBarsHeight, onOpen = { albumId = it.id })
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
}

/** The mode bar above the nav bar, or at its side on a short window, where the height is the photos'. */
@Composable
private fun BottomBars(
    tab: Int,
    onTab: (Int) -> Unit,
    libraryMode: LibraryMode,
    onLibraryMode: (LibraryMode) -> Unit,
    collapsed: Boolean,
    short: Boolean,
    modifier: Modifier = Modifier,
) {
    val modeBar: @Composable (Modifier) -> Unit = { barModifier ->
        AnimatedVisibility(
            visible = tab == LibraryTab && !collapsed,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = barModifier,
        ) {
            LibraryModeBar(mode = libraryMode, onSelect = onLibraryMode)
        }
    }
    val navBar: @Composable (Modifier) -> Unit = { barModifier ->
        PvotNavBar(
            selectedTab = tab,
            onTabClick = onTab,
            tabs = Tabs,
            compact = collapsed || short,
            modifier = barModifier.widthIn(max = PvotAdaptiveDefaults.ContentMaxWidth),
        )
    }
    if (short) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            navBar(Modifier.padding(bottom = 8.dp))
            modeBar(
                Modifier.align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Start))
                    .padding(start = 22.dp, bottom = 20.dp)
            )
        }
    } else {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            modeBar(Modifier.padding(bottom = 12.dp))
            navBar(Modifier.padding(bottom = 8.dp))
        }
    }
}
