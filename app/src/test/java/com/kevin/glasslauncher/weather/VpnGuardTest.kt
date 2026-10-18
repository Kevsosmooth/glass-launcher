package com.kevin.glasslauncher.weather

import java.time.ZonedDateTime
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnGuardTest {
    private val newYork = TimeZone.getTimeZone("America/New_York")
    private val july = ZonedDateTime.parse("2026-07-15T12:00:00Z").toInstant().toEpochMilli()
    private val january = ZonedDateTime.parse("2026-01-15T12:00:00Z").toInstant().toEpochMilli()

    @Test
    fun sameOffsetIsAcceptedEvenForAnotherZoneId() {
        assertTrue(lookupZoneMatches("America/New_York", newYork, july))
        assertTrue(lookupZoneMatches("America/Toronto", newYork, january))
    }

    @Test
    fun differentOffsetIsRejected() {
        assertFalse(lookupZoneMatches("Europe/Amsterdam", newYork, july))
        assertFalse(lookupZoneMatches("America/Los_Angeles", newYork, january))
    }

    @Test
    fun comparesTheCurrentOffsetsSoDstIsHandled() {
        // Puerto Rico is UTC-4 all year; New York is UTC-4 only in summer.
        assertTrue(lookupZoneMatches("America/Puerto_Rico", newYork, july))
        assertFalse(lookupZoneMatches("America/Puerto_Rico", newYork, january))
    }

    @Test
    fun unknownZoneIsRejected() {
        assertFalse(lookupZoneMatches("Mars/Olympus_Mons", newYork, july))
        assertFalse(lookupZoneMatches("", newYork, july))
    }
}
