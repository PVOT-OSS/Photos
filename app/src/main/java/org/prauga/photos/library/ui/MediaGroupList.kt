// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prauga.pvot.designsystem.effects.pvotPressScale
import java.text.NumberFormat
import org.prauga.photos.R
import org.prauga.photos.common.ui.thumbnailRequest

private const val CardAspectRatio = 1.5f

@Composable
fun MediaGroupList(
    groups: List<MediaGroup>,
    state: LazyListState,
    contentPadding: PaddingValues,
    label: (MediaGroup) -> String,
    onPick: (MediaGroup) -> Unit,
) {
    LazyColumn(
        state = state,
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(groups, key = { it.start }) { group -> GroupCard(group, label(group)) { onPick(group) } }
    }
}

@Composable
private fun GroupCard(group: MediaGroup, title: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val overlay = colorResource(R.color.media_overlay_content)
    val scrim = colorResource(R.color.media_scrim)
    Box(
        Modifier.fillMaxWidth()
            .aspectRatio(CardAspectRatio)
            .pvotPressScale(interaction)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        AsyncImage(
            model = remember(group.cover.cacheKey) { thumbnailRequest(context, group.cover) },
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to scrim.copy(alpha = 0f), 1f to scrim.copy(alpha = 0.55f)))
                .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = overlay,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.item_count,
                        group.count,
                        NumberFormat.getIntegerInstance().format(group.count),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = overlay.copy(alpha = 0.85f),
            )
        }
    }
}
