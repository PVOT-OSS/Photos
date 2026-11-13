// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.prauga.photos.common.ui.thumbnailRequest
import org.prauga.photos.media.model.MediaItem

private val ThumbWidth = 30.dp
private val CurrentThumbWidth = 44.dp
private val ThumbHeight = 44.dp
private val ThumbGap = 2.dp

@Composable
fun Filmstrip(items: List<MediaItem>, currentIndex: Int, onPick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state = rememberLazyListState(currentIndex)
    LaunchedEffect(currentIndex) { state.animateScrollToItem(currentIndex) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val sidePadding = (maxWidth - CurrentThumbWidth) / 2
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(ThumbGap),
            modifier = Modifier.fillMaxWidth().height(ThumbHeight),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val width by animateDpAsState(if (index == currentIndex) CurrentThumbWidth else ThumbWidth, label = "thumb")
                AsyncImage(
                    model = remember(item.cacheKey) { thumbnailRequest(context, item) },
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.width(width).height(ThumbHeight).clip(RoundedCornerShape(4.dp)).clickable {
                            onPick(index)
                        },
                )
            }
        }
    }
}
