// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.ui.viewer

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import com.prauga.pvot.designsystem.components.PvotGlassIconButton
import com.prauga.pvot.designsystem.components.PvotGlassSurface
import com.prauga.pvot.designsystem.effects.PvotGlassBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.photos.R
import org.prauga.photos.data.MediaItem
import org.prauga.photos.ui.common.formatDayLong
import org.prauga.photos.ui.common.formatRelativeDay
import org.prauga.photos.ui.common.formatSize
import org.prauga.photos.ui.common.formatTime

/** The glass bars over the viewer: back and the date on top, actions at the bottom. */
@Composable
fun ViewerChrome(
    item: MediaItem?,
    visible: Boolean,
    infoVisible: Boolean,
    backdrop: PvotGlassBackdrop,
    actions: MediaActions,
    onBack: () -> Unit,
    onInfo: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it / 3 },
            exit = fadeOut() + slideOutVertically { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                PvotGlassIconButton(onClick = onBack, backdrop = backdrop) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = stringResource(R.string.action_back),
                        modifier = Modifier.size(22.dp),
                    )
                }
                if (item != null) {
                    PvotGlassSurface(
                        backdrop = backdrop,
                        modifier = Modifier.align(Alignment.Center),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatRelativeDay(item.date),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = formatTime(item.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = visible && infoVisible && item != null,
                enter = fadeIn() + expandVertically(spring(stiffness = 500f)),
                exit = fadeOut() + shrinkVertically(),
            ) {
                if (item != null) InfoPanel(item, backdrop)
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                if (item != null) ActionBar(item, backdrop, actions, onInfo)
            }
        }
    }
}

@Composable
private fun ActionBar(item: MediaItem, backdrop: PvotGlassBackdrop, actions: MediaActions, onInfo: () -> Unit) {
    PvotGlassSurface(
        backdrop = backdrop,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionIcon(R.drawable.ic_share_2, R.string.action_share) { actions.share(item) }
            if (actions.canFavorite) {
                ActionIcon(
                    if (item.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                    if (item.isFavorite) R.string.action_unfavorite else R.string.action_favorite,
                    tint = if (item.isFavorite) Color(0xFFFF375F) else null,
                ) {
                    actions.toggleFavorite(item)
                }
            }
            ActionIcon(R.drawable.ic_info_circle, R.string.action_info, onClick = onInfo)
            ActionIcon(R.drawable.ic_trash, R.string.action_delete) { actions.delete(item) }
        }
    }
}

@Composable
private fun ActionIcon(
    @DrawableRes icon: Int,
    label: Int,
    tint: Color? = null,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** What the file is: when, where it came from, and the camera settings if the EXIF has them. */
@Composable
private fun InfoPanel(item: MediaItem, backdrop: PvotGlassBackdrop) {
    val context = LocalContext.current
    val exif by
        produceState<List<String>>(emptyList(), item.cacheKey) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                                val e = ExifInterface(stream)
                                listOfNotNull(
                                    listOfNotNull(e.getAttribute(ExifInterface.TAG_MAKE), e.getAttribute(ExifInterface.TAG_MODEL))
                                        .joinToString(" ")
                                        .takeIf { it.isNotBlank() },
                                    listOfNotNull(
                                            e.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "ƒ/$it" },
                                            e.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()?.let {
                                                if (it < 1) "1/${(1 / it).toInt()} s" else "$it s"
                                            },
                                            e.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" },
                                            e.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                                                .takeIf { it > 0 }
                                                ?.let { "%.1f mm".format(it) },
                                        )
                                        .joinToString("  ·  ")
                                        .takeIf { it.isNotBlank() },
                                )
                            }
                        }
                        .getOrNull()
                        .orEmpty()
                }
        }
    PvotGlassSurface(
        backdrop = backdrop,
        cornerRadius = 26.dp,
        contentAlignment = Alignment.TopStart,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${formatDayLong(item.date)} · ${formatTime(item.date)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOfNotNull(
                            item.bucketName.takeIf { it.isNotEmpty() },
                            if (item.width > 0) "${item.width} × ${item.height}" else null,
                            formatSize(item.size),
                        )
                        .joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            exif.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

