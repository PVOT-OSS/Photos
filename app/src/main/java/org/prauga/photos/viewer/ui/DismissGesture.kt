// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs

private const val VerticalBias = 1.3f
private const val SidewaysDamping = 0.6f

/**
 * Takes a mostly downward one-finger drag in the Initial pass, before the pager or the zoomable
 * image can claim it; sideways paging, pinches and taps still reach them.
 */
internal fun Modifier.dismissGesture(
    enabled: () -> Boolean,
    onDrag: (Offset) -> Unit,
    onRelease: (offset: Offset, velocity: Offset) -> Unit,
): Modifier = pointerInput(Unit) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!enabled()) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        var offset = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (!dragging && event.changes.count { it.pressed } > 1) return@awaitEachGesture
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            if (!change.pressed) {
                if (dragging) {
                    val velocity = tracker.calculateVelocity()
                    onRelease(offset, Offset(velocity.x, velocity.y))
                }
                return@awaitEachGesture
            }
            tracker.addPosition(change.uptimeMillis, change.position)
            val total = change.position - down.position
            if (!dragging && total.getDistance() > slop) {
                if (total.y > 0 && abs(total.y) > abs(total.x) * VerticalBias) dragging = true
                else return@awaitEachGesture
            }
            if (dragging) {
                change.consume()
                offset = Offset(total.x * SidewaysDamping, total.y)
                onDrag(offset)
            }
        }
    }
}
