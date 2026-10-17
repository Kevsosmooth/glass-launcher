package com.kevin.glasslauncher.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutMathTest {
    private val still = FocusGeometry(
        tileWidth = 124f, tileHeight = 69.75f, focusScale = 1f, focusDamping = 1f,
        tiltDegrees = 0f, wiggleDegrees = 0f, cameraDistance = 12f, elevation = 0f,
    )

    @Test
    fun criticallyDampedSpringDoesNotOvershoot() {
        assertEquals(0f, springOvershoot(1f), 0f)
        assertEquals(0.046f, springOvershoot(0.7f), 0.001f)
    }

    @Test
    fun stillTileHasNoFootprint() {
        assertEquals(Footprint(0f, 0f, 0f), focusFootprint(still))
    }

    @Test
    fun shadowSpreadsHalfItsElevationAndDropsAQuarterMore() {
        val f = focusFootprint(still.copy(elevation = 16f))
        assertEquals(8f, f.side, 0.001f)
        assertEquals(8f, f.top, 0.001f)
        assertEquals(12f, f.bottom, 0.001f)
    }

    @Test
    fun scaleGrowsEachSideByHalfTheExtraSize() {
        val f = focusFootprint(still.copy(focusScale = 1.2f))
        assertEquals(124f * 0.1f, f.side, 0.001f)
        assertEquals(69.75f * 0.1f, f.top, 0.001f)
    }

    @Test
    fun tiltAndWiggleOnlyEverAddRoom() {
        val base = focusFootprint(still.copy(focusScale = 1.15f))
        val turned = focusFootprint(still.copy(focusScale = 1.15f, tiltDegrees = 4f, wiggleDegrees = 1.5f))
        assertTrue(turned.side > base.side)
        assertTrue(turned.top > base.top)
    }

    @Test
    fun currentMotionTokensGiveTheDocumentedFootprint() {
        val f = HomeLayout.Footprint
        assertEquals(18.76f, f.side, 0.01f)
        assertEquals(15.34f, f.top, 0.01f)
        assertEquals(19.34f, f.bottom, 0.01f)
    }

    @Test
    fun metricsFillTheScreenWidthExactly() {
        val m = HomeLayout.Metrics
        assertEquals(960f, 6 * m.tileWidth + 5 * m.columnGap + 2 * m.sidePadding, 0.001f)
        assertEquals(m.columnGap, m.dockPadSide, 0f)
    }

    @Test
    fun currentMetricsMatchTheHomeLayoutDoc() {
        val m = HomeLayout.Metrics
        assertEquals(25f, m.columnGap, 0f)
        assertEquals(26f, m.rowGap, 0f)
        assertEquals(45.5f, m.sidePadding, 0f)
        assertEquals(22f, m.dockPadTop, 0f)
        assertEquals(26f, m.dockPadBottom, 0f)
        assertEquals(20.5f, m.dockInset, 0f)
    }

    @Test
    fun everyGapLeavesTheMarginPastTheFootprint() {
        val f = Footprint(side = 10.2f, top = 7f, bottom = 9.5f)
        val m = homeMetrics(960f, 6, 124f, f, margin = 6f)
        assertEquals(17f, m.columnGap, 0f)
        assertEquals(16f, m.rowGap, 0f)
        assertTrue(m.columnGap - f.side >= 6f)
        assertTrue(m.dockPadTop - f.top >= 6f)
        assertTrue(m.dockPadBottom - f.bottom >= 6f)
    }

    @Test
    fun aLayoutThatCannotFitFails() {
        assertThrows(IllegalArgumentException::class.java) {
            homeMetrics(960f, 6, 150f, Footprint(30f, 10f, 10f), margin = 6f)
        }
    }
}
