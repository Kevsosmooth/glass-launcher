package com.kevin.glasslauncher.motion

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectionTrackerTest {
    @Test
    fun tiltLeansTowardTravel() {
        val t = DirectionTracker()
        assertEquals(0f to 0f, t.tiltDegrees())
        t.last = Direction.Right
        assertEquals(0f to Motion.TILT_DEGREES, t.tiltDegrees())
        t.last = Direction.Left
        assertEquals(0f to -Motion.TILT_DEGREES, t.tiltDegrees())
        t.last = Direction.Down
        assertEquals(-Motion.TILT_DEGREES to 0f, t.tiltDegrees())
        t.last = Direction.Up
        assertEquals(Motion.TILT_DEGREES to 0f, t.tiltDegrees())
    }
}
