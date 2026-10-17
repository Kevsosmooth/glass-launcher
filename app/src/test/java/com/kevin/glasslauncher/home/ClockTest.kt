package com.kevin.glasslauncher.home

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTest {
    @Test
    fun twelveHour() {
        assertEquals("9:05", clockText(LocalTime.of(9, 5), use24h = false))
        assertEquals("12:00", clockText(LocalTime.of(0, 0), use24h = false))
        assertEquals("1:30", clockText(LocalTime.of(13, 30), use24h = false))
        assertEquals("12:15", clockText(LocalTime.of(12, 15), use24h = false))
    }

    @Test
    fun twentyFourHour() {
        assertEquals("09:05", clockText(LocalTime.of(9, 5), use24h = true))
        assertEquals("23:59", clockText(LocalTime.of(23, 59), use24h = true))
    }

    @Test
    fun millisToNextMinute() {
        assertEquals(60_000L, millisToNextMinute(120_000L))
        assertEquals(1L, millisToNextMinute(119_999L))
        assertEquals(30_000L, millisToNextMinute(90_000L))
    }
}
