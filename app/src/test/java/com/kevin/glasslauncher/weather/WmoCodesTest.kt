package com.kevin.glasslauncher.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WmoCodesTest {
    @Test
    fun clearHasDayAndNight() {
        assertEquals(WeatherCondition.ClearDay, weatherCondition(0, isDay = true))
        assertEquals(WeatherCondition.ClearNight, weatherCondition(0, isDay = false))
        assertEquals(WeatherCondition.ClearDay, weatherCondition(1, isDay = true))
    }

    @Test
    fun everyGroupMapsToItsCondition() {
        assertEquals(WeatherCondition.PartlyCloudy, weatherCondition(2, true))
        assertEquals(WeatherCondition.Cloudy, weatherCondition(3, true))
        listOf(45, 48).forEach { assertEquals(WeatherCondition.Fog, weatherCondition(it, true)) }
        listOf(51, 55, 61, 65, 67, 80, 82).forEach { assertEquals(WeatherCondition.Rain, weatherCondition(it, false)) }
        listOf(71, 75, 77, 85, 86).forEach { assertEquals(WeatherCondition.Snow, weatherCondition(it, true)) }
        listOf(95, 96, 99).forEach { assertEquals(WeatherCondition.Thunder, weatherCondition(it, true)) }
    }

    @Test
    fun unknownCodesShowAsCloudy() {
        assertEquals(WeatherCondition.Cloudy, weatherCondition(42, true))
        assertEquals(WeatherCondition.Cloudy, weatherCondition(-1, false))
    }
}
