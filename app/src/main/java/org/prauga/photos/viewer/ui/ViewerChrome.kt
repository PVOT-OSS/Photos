// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotGlassIconButton
import com.prauga.pvot.designsystem.components.PvotGlassSurface
import com.prauga.pvot.designsystem.effects.PvotGlassBackdrop
import kotlin.math.roundToInt
import org.prauga.photos.R
import org.prauga.photos.common.format.formatDayLong
import org.prauga.photos.common.format.formatRelativeDay
import org.prauga.photos.common.format.formatSize
import org.prauga.photos.common.format.formatTime
import org.prauga.photos.media.model.CaptureDetails
import org.prauga.photos.media.model.MediaItem

@Composable
fun ViewerChrome(
    item: MediaItem?,
    visible: Boolean,
    infoVisible: Boolean,
    details: CaptureDetails?,
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
                if (item != null) InfoPanel(item, details, backdrop)
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
    PvotGlassSurface(backdrop = backdrop, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionIcon(R.drawable.ic_share_2, R.string.action_share) { actions.share(item) }
            if (actions.canFavorite) {
                ActionIcon(
                    icon = if (item.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                    label = if (item.isFavorite) R.string.action_unfavorite else R.string.action_favorite,
                    tint = if (item.isFavorite) MaterialTheme.colorScheme.error else null,
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
private fun ActionIcon(@DrawableRes icon: Int, @StringRes label: Int, tint: Color? = null, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(label),
            tint = tint ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun InfoPanel(item: MediaItem, details: CaptureDetails?, backdrop: PvotGlassBackdrop) {
    val context = LocalContext.current
    val separator = stringResource(R.string.detail_separator)
    val fileLine =
        listOfNotNull(
                item.bucketName.takeIf { it.isNotEmpty() },
                if (item.width > 0) stringResource(R.string.detail_dimensions, item.width, item.height) else null,
                formatSize(context, item.size),
            )
            .joinToString(separator)
    val exposure =
        details?.exposureSeconds?.let {
            if (it < 1) stringResource(R.string.exif_exposure_fraction, (1 / it).roundToInt())
            else stringResource(R.string.exif_exposure_seconds, it)
        }
    val captureLine =
        listOfNotNull(
                details?.aperture?.let { stringResource(R.string.exif_aperture, it) },
                exposure,
                details?.iso?.let { stringResource(R.string.exif_iso, it) },
                details?.focalLengthMm?.let { stringResource(R.string.exif_focal_length, it) },
            )
            .joinToString(separator)
    PvotGlassSurface(
        backdrop = backdrop,
        cornerRadius = 26.dp,
        contentAlignment = Alignment.TopStart,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.detail_date_time, formatDayLong(item.date), formatTime(item.date)),
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
            DetailLine(fileLine)
            details?.camera?.let { DetailLine(it) }
            if (captureLine.isNotEmpty()) DetailLine(captureLine)
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
