package com.kevin.glasslauncher.glass

import org.junit.Assert.assertEquals
import org.junit.Test

class GlassSourceRectTest {
    @Test
    fun mapsScreenBoundsToDownscaledBitmap() {
        val r = glassSourceRect(800f, 960f, 160f, 80f, downscale = 8, bmpW = 240, bmpH = 135)
        assertEquals(SrcRect(100, 120, 20, 10), r)
    }

    @Test
    fun clampsAtBitmapEdges() {
        val r = glassSourceRect(1900f, 1070f, 160f, 80f, downscale = 8, bmpW = 240, bmpH = 135)
        assertEquals(SrcRect(237, 133, 3, 2), r)
    }

    @Test
    fun negativeOffsetsClampToZero() {
        val r = glassSourceRect(-40f, -16f, 160f, 80f, downscale = 8, bmpW = 240, bmpH = 135)
        assertEquals(SrcRect(0, 0, 20, 10), r)
    }
}
