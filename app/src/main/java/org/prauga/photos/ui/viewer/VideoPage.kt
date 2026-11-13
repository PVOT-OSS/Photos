// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.viewer

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import coil3.compose.AsyncImage
import com.prauga.pvot.designsystem.components.PvotGlassSurface
import com.prauga.pvot.designsystem.components.progress.PvotLinearProgress
import kotlinx.coroutines.delay
import org.prauga.photos.R
import org.prauga.photos.data.MediaItem
import org.prauga.photos.ui.common.formatDuration
import org.prauga.photos.ui.common.thumbnailRequest

/**
 * A video page: its first frame until it plays, then the video, looping, with glass controls
 * (play/pause, scrubber, mute) that come and go with the viewer's chrome.
 *
 * A player exists only while the page is the one on screen.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPage(item: MediaItem, playing: Boolean, chromeVisible: Boolean, onTap: () -> Unit) {
    val context = LocalContext.current
    var active by remember { mutableStateOf(false) }
    LaunchedEffect(playing) { if (playing) active = true }

    Box(
        Modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        var firstFrame by remember { mutableStateOf(false) }
        val player =
            if (active) {
                remember(item.id) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(Media3Item.fromUri(item.uri))
                        repeatMode = Player.REPEAT_MODE_ONE
                        prepare()
                    }
                }
            } else null
        if (player != null) {
            DisposableEffect(player) {
                val listener =
                    object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            firstFrame = true
                        }
                    }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                    player.release()
                }
            }
            LaunchedEffect(playing) { if (playing) player.play() else player.pause() }
        }
        Box(Modifier.aspectRatio(item.aspectRatio)) {
            if (player != null) {
                PlayerSurface(player, Modifier.fillMaxSize(), surfaceType = SURFACE_TYPE_TEXTURE_VIEW)
            }
            if (!firstFrame) {
                AsyncImage(
                    model = remember(item.cacheKey) { thumbnailRequest(context, item) },
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (player != null) VideoControls(player, chromeVisible, Modifier)
    }
}

@Composable
private fun VideoControls(player: ExoPlayer, visible: Boolean, modifier: Modifier) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var muted by remember { mutableStateOf(player.volume == 0f) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(1L) }
    var fraction by remember { mutableFloatStateOf(0f) }
    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (true) {
            position = player.currentPosition
            duration = player.duration.takeIf { it > 0 } ?: 1L
            fraction = (position.toFloat() / duration).coerceIn(0f, 1f)
            delay(100)
        }
    }
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 84.dp),
        ) {
            PvotGlassSurface(
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                        Icon(
                            painter =
                                painterResource(
                                    if (isPlaying) R.drawable.ic_player_pause_filled
                                    else R.drawable.ic_player_play_filled
                                ),
                            contentDescription =
                                stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = formatDuration(position),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    PvotLinearProgress(
                        progress = { fraction },
                        onSeek = { player.seekTo((it * duration).toLong()) },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text(
                        text = "-" + formatDuration((duration - position).coerceAtLeast(0)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(
                        onClick = {
                            muted = !muted
                            player.volume = if (muted) 0f else 1f
                        }
                    ) {
                        Icon(
                            painter = painterResource(if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume),
                            contentDescription =
                                stringResource(if (muted) R.string.action_unmute else R.string.action_mute),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
