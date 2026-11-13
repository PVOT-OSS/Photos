// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.common.ui

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable

/** The viewer hides the system bars; screens under it pad by these so they don't shift meanwhile. */
@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = statusBarsIgnoringVisibility

@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.stableNavigationBars: WindowInsets
    @Composable get() = navigationBarsIgnoringVisibility
