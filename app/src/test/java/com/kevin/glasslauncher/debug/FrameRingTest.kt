package com.kevin.glasslauncher.debug

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameRingTest {
    private val ms = 1_000_000L

    @Test
    fun snapshotIsOldestToNewestAndWraps() {
        val ring = FrameRing(3)
        listOf(1f, 2f, 3f, 4f).forEachIndexed { i, d -> ring.add(d, i * 16 * ms) }
        assertArrayEquals(floatArrayOf(2f, 3f, 4f), ring.snapshot(false).frameMs, 0f)
    }

    @Test
    fun fpsCountsFramesInLastSecond() {
        val ring = FrameRing(200)
        repeat(120) { ring.add(5f, it * 16_666_667L) } // ~2 s at 60 Hz
        assertEquals(60, ring.snapshot(false).fps)
    }

    @Test
    fun jankyPercent() {
        val ring = FrameRing(10)
        repeat(9) { ring.add(5f, it * 16 * ms) }
        ring.add(20f, 9 * 16 * ms)
        assertEquals(10f, ring.snapshot(false).jankyPercent, 0.01f)
    }

    @Test
    fun publisherIgnoresSingleFrames() {
        val published = mutableListOf<HudSnapshot>()
        val pub = HudPublisher(FrameRing(10), 250) { published += it }
        pub.onFrame(5f, 0, nowMs = 1000) // one frame (e.g. the HUD's own redraw): no publish
        assertEquals(0, published.size)
        pub.onFrame(5f, 16 * ms, nowMs = 1016)
        assertEquals(1, published.size)
        pub.onFrame(5f, 32 * ms, nowMs = 1032)
        pub.onFrame(5f, 48 * ms, nowMs = 1048) // within 250 ms: throttled
        assertEquals(1, published.size)
    }

    @Test
    fun droppedFlagSetBySlowFrameThenCleared() {
        val published = mutableListOf<HudSnapshot>()
        val pub = HudPublisher(FrameRing(10), 0) { published += it }
        pub.onFrame(30f, 0, 0)
        pub.onFrame(5f, 16 * ms, 1)
        pub.onFrame(5f, 32 * ms, 2)
        pub.onFrame(5f, 48 * ms, 3)
        assertEquals(listOf(true, false), published.map { it.dropped })
    }
}
