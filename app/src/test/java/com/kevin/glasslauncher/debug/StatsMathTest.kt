package com.kevin.glasslauncher.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsMathTest {
    // Fields 14 and 15 (utime, stime) are 300 and 50; the name holds a space and a ')'.
    private val stat = "1234 (glass (x) launcher) S 1 1234 0 0 -1 4194624 9000 0 0 0 300 50 0 0 20 0 30 0 100 0 0"

    @Test
    fun parsesUserPlusSystemTicks() {
        assertEquals(350L, parseCpuTicks(stat))
        assertNull(parseCpuTicks("garbage"))
        assertNull(parseCpuTicks("1 (x) S 1 2"))
    }

    @Test
    fun cpuPercentFromTwoSamples() {
        val later = stat.replace(" 300 50 ", " 400 70 ") // +120 ticks
        // 120 ticks at 100/s = 1.2 s busy over 2 s on 4 cores = 15 %.
        assertEquals(15f, requireNotNull(cpuPercent(parseCpuTicks(stat), parseCpuTicks(later), 2_000, 100, 4)), 0.001f)
        assertNull(cpuPercent(null, 350, 2_000, 100, 4))
        assertNull(cpuPercent(350, 470, 2_000, -1, 4))
    }

    @Test
    fun rateIsBytesDeltaPerSecondInKb() {
        assertEquals(10.0, requireNotNull(kbPerSecond(0, 20_480, 2_000)), 0.0001)
        assertNull(kbPerSecond(-1, 20_480, 2_000))
        assertNull(kbPerSecond(0, -1, 2_000))
        assertNull(kbPerSecond(0, 10, 0))
    }
}
