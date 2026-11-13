// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotSegmentedControl
import com.prauga.pvot.designsystem.components.PvotSegmentedControlDefaults
import com.prauga.pvot.designsystem.components.pvotGlassSurface
import com.prauga.pvot.designsystem.effects.LocalPvotGlassBackdrop
import org.prauga.photos.R

@Composable
fun LibraryModeBar(mode: LibraryMode, onSelect: (LibraryMode) -> Unit, modifier: Modifier = Modifier) {
    val modes = LibraryMode.entries
    val labels =
        modes.map {
            when (it) {
                LibraryMode.Years -> stringResource(R.string.mode_years)
                LibraryMode.Months -> stringResource(R.string.mode_months)
                LibraryMode.All -> stringResource(R.string.mode_all)
            }
        }
    Box(modifier.width(280.dp).pvotGlassSurface(backdrop = LocalPvotGlassBackdrop.current, role = null)) {
        PvotSegmentedControl(
            options = labels,
            selectedIndex = modes.indexOf(mode),
            onSelect = { onSelect(modes[it]) },
            colors =
                PvotSegmentedControlDefaults.colors(MaterialTheme.colorScheme, trackColor = Color.Transparent),
        )
    }
}
