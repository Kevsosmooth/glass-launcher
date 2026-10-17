package com.kevin.glasslauncher.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerTest {
    private val now = 1_000_000_000L

    @Test
    fun remainingRoundsUpAndIsZeroWhenOffOrPast() {
        assertEquals(0, remainingMinutes(0L, now))
        assertEquals(0, remainingMinutes(now, now))
        assertEquals(0, remainingMinutes(now - 5, now))
        assertEquals(1, remainingMinutes(now + 1, now))
        assertEquals(1, remainingMinutes(now + MINUTE_MILLIS, now))
        assertEquals(2, remainingMinutes(now + MINUTE_MILLIS + 1, now))
        assertEquals(90, remainingMinutes(now + 90 * MINUTE_MILLIS, now))
    }

    @Test
    fun selectCyclesOffThroughPresetsBackToOff() {
        assertEquals(15, nextTimerMinutes(0))
        assertEquals(30, nextTimerMinutes(15))
        assertEquals(60, nextTimerMinutes(30))
        assertEquals(90, nextTimerMinutes(60))
        assertEquals(0, nextTimerMinutes(90))
    }

    @Test
    fun partlyElapsedTimerMovesToTheNextLongerPreset() {
        assertEquals(15, nextTimerMinutes(9))
        assertEquals(30, nextTimerMinutes(16))
        assertEquals(90, nextTimerMinutes(61))
    }

    @Test
    fun sleepAtForOffIsZero() {
        assertEquals(0L, sleepAtFor(0, now))
        assertEquals(now + 15 * MINUTE_MILLIS, sleepAtFor(15, now))
    }

    @Test
    fun nextDropIsWhenTheRoundedUpMinuteChanges() {
        assertEquals(MINUTE_MILLIS, millisToNextMinuteDrop(now + MINUTE_MILLIS, now))
        assertEquals(1L, millisToNextMinuteDrop(now + MINUTE_MILLIS + 1, now))
        assertEquals(30_000L, millisToNextMinuteDrop(now + 90_000L, now))
        val at = now + 90_000L
        val later = now + millisToNextMinuteDrop(at, now)
        assertEquals(remainingMinutes(at, now) - 1, remainingMinutes(at, later))
    }
}
