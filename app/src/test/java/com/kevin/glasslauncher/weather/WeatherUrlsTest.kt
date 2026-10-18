package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.TempUnit
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUrlsTest {
    @Test
    fun coordinatesRoundToTwoDecimals() {
        assertEquals(40.71, roundCoord(40.7128), 0.0)
        assertEquals(-74.01, roundCoord(-74.0060), 0.0)
        assertEquals(51.5, roundCoord(51.4999), 0.0)
        assertEquals(0.0, roundCoord(0.001), 0.0)
    }

    @Test
    fun forecastUrlIsRoundedAndUsesADotInAnyLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(
                "https://api.open-meteo.com/v1/forecast?latitude=40.71&longitude=-74.01" +
                    "&current=temperature_2m,weather_code,is_day,apparent_temperature,relative_humidity_2m,wind_speed_10m" +
                    "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,is_day" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&forecast_hours=12&forecast_days=5&timezone=auto&temperature_unit=fahrenheit&wind_speed_unit=mph",
                forecastUrl(40.7128, -74.006, TempUnit.Fahrenheit),
            )
        } finally {
            Locale.setDefault(saved)
        }
        assertTrue(forecastUrl(1.0, 2.0, TempUnit.Celsius).endsWith("temperature_unit=celsius&wind_speed_unit=kmh"))
    }

    @Test
    fun citySearchIsEncoded() {
        assertEquals(
            "https://geocoding-api.open-meteo.com/v1/search?name=S%C3%A3o+Paulo%26x%3D1&count=8",
            citySearchUrl("  São Paulo&x=1 "),
        )
    }

    @Test
    fun onlyHttpsOnAllowedHosts() {
        assertTrue(isAllowedUrl("https://api.open-meteo.com/v1/forecast?latitude=1"))
        assertTrue(isAllowedUrl("https://get.geojs.io/v1/ip/geo.json"))
        assertFalse(isAllowedUrl("http://api.open-meteo.com/v1/forecast"))
        assertFalse(isAllowedUrl("https://evil.example.com/v1/forecast"))
        assertFalse(isAllowedUrl("https://api.open-meteo.com.evil.example/x"))
        assertFalse(isAllowedUrl("https://user@get.geojs.io/v1/ip/geo.json"))
        assertFalse(isAllowedUrl("https://get.geojs.io:8443/v1/ip/geo.json"))
        assertFalse(isAllowedUrl("not a url"))
    }

    @Test
    fun temperatureHasOnlyTheDegreeSign() {
        assertEquals("72°", temperatureText(72))
        assertEquals("-3°", temperatureText(-3))
    }
}
