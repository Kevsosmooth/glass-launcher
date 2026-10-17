package com.kevin.glasslauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DominantColorTest {
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF

    @Test
    fun mostCommonOpaqueHueWins() {
        val px = IntArray(100) { if (it < 70) argb(255, 200, 30, 30) else argb(255, 30, 30, 200) }
        val c = dominantColor(px, fallback = 0)
        assertTrue("red should dominate: ${Integer.toHexString(c)}", r(c) > b(c))
    }

    @Test
    fun transparentPixelsAreIgnored() {
        val px = IntArray(100) { if (it < 90) argb(0, 255, 255, 255) else argb(255, 20, 160, 40) }
        val c = dominantColor(px, fallback = 0)
        assertTrue(g(c) > r(c) && g(c) > b(c))
    }

    @Test
    fun allTransparentReturnsFallback() {
        assertEquals(123, dominantColor(IntArray(10), fallback = 123))
    }

    @Test
    fun resultIsOpaqueAndDarkEnoughForWhiteText() {
        val c = dominantColor(IntArray(50) { argb(255, 250, 250, 240) }, fallback = 0)
        assertEquals(0xFF, c ushr 24)
        assertTrue("luminance too high for white text", relativeLuminance(c) <= MAX_TILE_LUMINANCE + 1e-6)
    }
}
