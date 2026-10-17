package com.kevin.glasslauncher.glass

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxBlurTest {
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun red(p: Int) = (p ushr 16) and 0xFF

    @Test
    fun uniformImageIsUnchanged() {
        val px = IntArray(20 * 10) { argb(255, 40, 80, 120) }
        assertArrayEquals(px, BoxBlur.blur(px, 20, 10, radius = 3))
    }

    @Test
    fun inputIsNotMutated() {
        val px = IntArray(9 * 9) { if (it == 40) argb(255, 255, 255, 255) else argb(255, 0, 0, 0) }
        val copy = px.copyOf()
        BoxBlur.blur(px, 9, 9, radius = 1)
        assertArrayEquals(copy, px)
    }

    @Test
    fun brightPixelSpreadsSymmetrically() {
        val w = 21
        val center = 10 * w + 10
        val px = IntArray(w * w) { if (it == center) argb(255, 255, 0, 0) else argb(255, 0, 0, 0) }
        val out = BoxBlur.blur(px, w, w, radius = 2, passes = 1)
        assertTrue(red(out[center]) in 1 until 255)
        assertEquals(red(out[center - 1]), red(out[center + 1]))
        assertEquals(red(out[center - w]), red(out[center + w]))
        assertEquals(0, red(out[0]))
    }

    @Test
    fun edgesClampInsteadOfDarkening() {
        val px = IntArray(8 * 8) { argb(255, 200, 200, 200) }
        val out = BoxBlur.blur(px, 8, 8, radius = 4)
        assertEquals(200, red(out[0]))
        assertEquals(200, red(out[63]))
    }
}
