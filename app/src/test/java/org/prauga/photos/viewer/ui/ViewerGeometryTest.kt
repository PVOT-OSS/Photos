// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.photos.viewer.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewerGeometryTest {

    private val portraitScreen = Size(1000f, 2000f)

    @Test
    fun landscapePhotoFillsTheWidthAndIsCentred() {
        assertEquals(Rect(0f, 750f, 1000f, 1250f), fitRect(2f, portraitScreen))
    }

    @Test
    fun tallPhotoFillsTheHeightAndIsCentred() {
        assertEquals(Rect(250f, 0f, 750f, 2000f), fitRect(0.25f, portraitScreen))
    }

    @Test
    fun scalingKeepsTheCentreAndAppliesTheOffset() {
        val scaled = Rect(0f, 0f, 100f, 100f).scaledAroundCenter(0.5f, Offset(10f, 20f))

        assertEquals(Rect(35f, 45f, 85f, 95f), scaled)
    }

    @Test
    fun lerpRectMovesEachEdgeLinearly() {
        val from = Rect(0f, 0f, 10f, 10f)
        val to = Rect(10f, 20f, 30f, 40f)

        assertEquals(from, lerpRect(from, to, 0f))
        assertEquals(to, lerpRect(from, to, 1f))
        assertEquals(Rect(5f, 10f, 20f, 25f), lerpRect(from, to, 0.5f))
    }
}
