// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.collections.ui

import androidx.compose.runtime.Immutable
import org.prauga.photos.media.model.Album

@Immutable
data class CollectionsUiState(val mediaTypes: List<Album> = emptyList(), val folders: List<Album> = emptyList())
