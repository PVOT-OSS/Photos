// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sign

private val CollapseDistance = 32.dp

/**
 * Collapses the bottom bars while the user scrolls away from the screen's home edge and brings them
 * back on the way home. The library opens on its newest row, so its home edge is the bottom.
 */
@Stable
class CollapsingBarsState internal constructor(private val thresholdPx: Float) {
    var collapsed: Boolean by mutableStateOf(false)
        private set

    var homeIsTop: Boolean = true

    /** Distance scrolled in the current direction; positive toward home. */
    private var travel = 0f

    fun expand() {
        collapsed = false
        travel = 0f
    }

    val connection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // A positive y delta moves the content toward its top edge.
                val home = if (homeIsTop) 1f else -1f
                if (consumed.y != 0f) {
                    val step = consumed.y * home
                    if (step.sign != travel.sign) travel = 0f
                    travel += step
                    if (travel < -thresholdPx) collapsed = true else if (travel > thresholdPx) collapsed = false
                } else if (available.y * home > 0f) {
                    expand()
                }
                return Offset.Zero
            }
        }
}

@Composable
fun rememberCollapsingBarsState(): CollapsingBarsState {
    val threshold = with(LocalDensity.current) { CollapseDistance.toPx() }
    return remember { CollapsingBarsState(threshold) }
}
