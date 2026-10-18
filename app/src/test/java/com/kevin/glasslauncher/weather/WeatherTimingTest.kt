package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.TempUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTimingTest {
    private val t0 = 1_800_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val here = Located("Brooklyn", 40.65, -73.95, LocationSource.Internet, t0)
    private fun weather(at: Long, unit: TempUnit = TempUnit.Fahrenheit, lat: Double = 40.65) =
        WeatherNow(72, unit, WeatherCondition.ClearDay, at, lat, -73.95)

    @Test
    fun locationIsLookedUpAgainAfterSixHoursOrWhenPickedChanges() {
        assertTrue(WeatherTiming.locationDue(null, pickedSet = false, deviceAllowed = false, now = t0))
        assertFalse(WeatherTiming.locationDue(here, pickedSet = false, deviceAllowed = false, now = t0 + 6 * hour - 1))
        assertTrue(WeatherTiming.locationDue(here, pickedSet = false, deviceAllowed = false, now = t0 + 6 * hour))
        assertTrue(WeatherTiming.locationDue(here, pickedSet = true, deviceAllowed = false, now = t0))
        assertTrue(WeatherTiming.locationDue(here.copy(source = LocationSource.Picked), pickedSet = false, deviceAllowed = true, now = t0))
        // Clock moved backwards: treat as old.
        assertTrue(WeatherTiming.locationDue(here, pickedSet = false, deviceAllowed = false, now = t0 - minute))
    }

    @Test
    fun tvLocationIsDroppedOnceNotAllowed() {
        val fromTv = here.copy(name = null, source = LocationSource.Network)
        assertFalse(WeatherTiming.locationDue(fromTv, pickedSet = false, deviceAllowed = true, now = t0))
        assertTrue(WeatherTiming.locationDue(fromTv, pickedSet = false, deviceAllowed = false, now = t0))
        assertFalse(WeatherTiming.usableAutomatic(fromTv, deviceAllowed = false))
        assertTrue(WeatherTiming.usableAutomatic(here, deviceAllowed = false))
    }

    @Test
    fun failedLookupsWaitHalfAnHour() {
        assertTrue(WeatherTiming.retryDue(null, t0))
        assertFalse(WeatherTiming.retryDue(t0, t0 + 29 * minute))
        assertTrue(WeatherTiming.retryDue(t0, t0 + 30 * minute))
    }

    @Test
    fun weatherRefreshesEveryThirtyMinutesOrOnChange() {
        assertTrue(WeatherTiming.weatherDue(null, here, TempUnit.Fahrenheit, t0))
        assertFalse(WeatherTiming.weatherDue(weather(t0), here, TempUnit.Fahrenheit, t0 + 29 * minute))
        assertTrue(WeatherTiming.weatherDue(weather(t0), here, TempUnit.Fahrenheit, t0 + 30 * minute))
        assertTrue(WeatherTiming.weatherDue(weather(t0), here, TempUnit.Celsius, t0))
        assertTrue(WeatherTiming.weatherDue(weather(t0, lat = 51.5), here, TempUnit.Fahrenheit, t0))
    }

    @Test
    fun cachedWeatherShowsOnlyUnderThreeHoursForTheSamePlace() {
        val w = weather(t0)
        assertEquals(w, WeatherTiming.showable(w, here, TempUnit.Fahrenheit, t0 + 3 * hour - 1))
        assertNull(WeatherTiming.showable(w, here, TempUnit.Fahrenheit, t0 + 3 * hour))
        assertNull(WeatherTiming.showable(w, null, TempUnit.Fahrenheit, t0))
        assertNull(WeatherTiming.showable(w, here, TempUnit.Celsius, t0))
        assertNull(WeatherTiming.showable(null, here, TempUnit.Fahrenheit, t0))
    }

    @Test
    fun nextCheckWaitsUntilDueOrRetriesSoon() {
        assertEquals(RETRY_MS, WeatherTiming.nextCheckIn(null, here, TempUnit.Fahrenheit, t0))
        assertEquals(REFRESH_MS, WeatherTiming.nextCheckIn(weather(t0), here, TempUnit.Fahrenheit, t0))
        assertEquals(20 * minute, WeatherTiming.nextCheckIn(weather(t0), here, TempUnit.Fahrenheit, t0 + 10 * minute))
        assertEquals(RETRY_MS, WeatherTiming.nextCheckIn(weather(t0), here, TempUnit.Fahrenheit, t0 + 29 * minute))
    }
}
