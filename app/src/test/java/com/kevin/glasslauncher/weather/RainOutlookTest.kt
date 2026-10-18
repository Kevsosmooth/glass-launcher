package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class RainOutlookTest {
    /** Twelve hours from 1 PM; [wet] hours get an 80% chance, [drizzle] ones 0.3 mm at 20%. */
    private fun hours(wet: Set<Int> = emptySet(), drizzle: Set<Int> = emptySet(), snow: Boolean = false) =
        (0 until 12).map { i ->
            val hour = (13 + i) % 24
            when (i) {
                in wet -> HourForecast(hour, 50, 80, 0.0, if (snow) WeatherCondition.Snow else WeatherCondition.Rain)
                in drizzle -> HourForecast(hour, 50, 20, 0.3, WeatherCondition.Rain)
                else -> HourForecast(hour, 50, 49, 0.1, WeatherCondition.Cloudy)
            }
        }

    @Test
    fun dryWhenNoHourReachesFiftyPercentOrAFifthOfAMillimetre() {
        assertEquals(RainOutlook.Dry, rainOutlook(hours()))
        assertEquals(UiText.Res(R.string.weather_dry), rainSummary(RainOutlook.Dry, clock24 = false))
    }

    @Test
    fun startsAndStops() {
        val outlook = rainOutlook(hours(wet = setOf(2, 3, 4)))
        assertEquals(RainOutlook.Later(start = 15, stop = 18, snow = false), outlook)
        assertEquals(UiText.Res(R.string.weather_rain_starts_stops, listOf("3 PM", "6 PM")), rainSummary(outlook, clock24 = false))
    }

    @Test
    fun rainingNowUntil() {
        val outlook = rainOutlook(hours(wet = setOf(0, 1), drizzle = setOf(2, 3)))
        assertEquals(RainOutlook.Now(stop = 17, snow = false), outlook)
        assertEquals(UiText.Res(R.string.weather_rain_until, listOf("5 PM")), rainSummary(outlook, clock24 = false))
    }

    @Test
    fun startsWithoutStopping() {
        val outlook = rainOutlook(hours(wet = (8 until 12).toSet()))
        assertEquals(RainOutlook.Later(start = 21, stop = null, snow = false), outlook)
        assertEquals(UiText.Res(R.string.weather_rain_starts, listOf("9 PM")), rainSummary(outlook, clock24 = false))
        val allDay = rainOutlook(hours(wet = (0 until 12).toSet()))
        assertEquals(RainOutlook.Now(stop = null, snow = false), allDay)
        assertEquals(UiText.Res(R.string.weather_rain_all), rainSummary(allDay, clock24 = false))
    }

    @Test
    fun snowWhenEveryWetHourIsSnow() {
        val outlook = rainOutlook(hours(wet = setOf(3, 4), snow = true))
        assertEquals(RainOutlook.Later(start = 16, stop = 18, snow = true), outlook)
        assertEquals(UiText.Res(R.string.weather_snow_starts_stops, listOf("4 PM", "6 PM")), rainSummary(outlook, clock24 = false))
        // One rainy hour in the spell makes it rain.
        assertEquals(RainOutlook.Later(16, 19, snow = false), rainOutlook(hours(wet = setOf(3, 4), snow = true).let {
            it.toMutableList().also { list -> list[5] = HourForecast(18, 50, 0, 0.5, WeatherCondition.Rain) }
        }))
    }

    @Test
    fun hourLabelsFollowTheClockSetting() {
        assertEquals(UiText.Res(R.string.weather_rain_starts_stops, listOf("15:00", "18:00")), rainSummary(RainOutlook.Later(15, 18, false), clock24 = true))
        assertEquals("12 AM", hourLabel(0, clock24 = false))
        assertEquals("12 PM", hourLabel(12, clock24 = false))
        assertEquals("9 AM", hourLabel(9, clock24 = false))
        assertEquals("09:00", hourLabel(9, clock24 = true))
        assertEquals("00:00", hourLabel(0, clock24 = true))
    }
}
