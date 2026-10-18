package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.data.TempUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs on Robolectric for Android's real org.json (the plain JVM android.jar only has stubs). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WeatherParseTest {
    private val forecast = """
        {"latitude":40.710335,"longitude":-73.99308,"utc_offset_seconds":0,
         "current_units":{"time":"iso8601","temperature_2m":"°F","weather_code":"wmo code","is_day":""},
         "current":{"time":"2026-10-18T14:45","interval":900,"temperature_2m":71.6,"weather_code":2,"is_day":0}}
    """.trimIndent()

    @Test
    fun forecastGivesRoundedTemperatureAndCondition() {
        val now = parseForecast(forecast, TempUnit.Fahrenheit, 1_000L, 40.71, -74.01)
        assertEquals(WeatherNow(72, TempUnit.Fahrenheit, WeatherCondition.PartlyCloudy, 1_000L, 40.71, -74.01), now)
        val night = forecast.replace("\"weather_code\":2", "\"weather_code\":0")
        assertEquals(WeatherCondition.ClearNight, parseForecast(night, TempUnit.Celsius, 0L, 0.0, 0.0)?.condition)
    }

    @Test
    fun forecastWithoutUsableCurrentIsNull() {
        assertNull(parseForecast("""{"current":{"weather_code":1}}""", TempUnit.Celsius, 0L, 0.0, 0.0))
        assertNull(parseForecast("""{"current":{"temperature_2m":"warm","weather_code":1}}""", TempUnit.Celsius, 0L, 0.0, 0.0))
        assertNull(parseForecast("""{"error":true,"reason":"bad"}""", TempUnit.Celsius, 0L, 0.0, 0.0))
        assertNull(parseForecast("<html>", TempUnit.Celsius, 0L, 0.0, 0.0))
    }

    @Test
    fun geocodingResultsAreLabelledAndRounded() {
        val json = """
            {"results":[
              {"id":1,"name":"Paris","latitude":48.85341,"longitude":2.3488,"country":"France","admin1":"Île-de-France Region"},
              {"id":2,"name":"Paris","latitude":33.66094,"longitude":-95.55551,"country":"United States","admin1":"Texas"},
              {"id":3,"name":"Monaco","latitude":43.73,"longitude":7.42,"country":"Monaco","admin1":"Monaco"},
              {"id":4,"name":"","latitude":1,"longitude":1},
              {"id":5,"name":"No coords"}
            ],"generationtime_ms":0.16}
        """.trimIndent()
        assertEquals(
            listOf(
                City("Paris, Île-de-France Region, France", 48.85, 2.35),
                City("Paris, Texas, United States", 33.66, -95.56),
                City("Monaco", 43.73, 7.42),
            ),
            parseCities(json),
        )
    }

    @Test
    fun geocodingWithNoMatchIsEmptyAndGarbageIsNull() {
        assertEquals(emptyList<City>(), parseCities("""{"generationtime_ms":0.2}"""))
        assertNull(parseCities("not json"))
    }

    @Test
    fun geojsSendsCoordinatesAsStrings() {
        val json = """{"city":"Brooklyn","latitude":"40.6501","longitude":"-73.9496","timezone":"America/New_York","ip":"x"}"""
        assertEquals(IpGeo("Brooklyn", 40.65, -73.95, "America/New_York"), parseIpGeo(json))
        val noCity = """{"latitude":"1.234","longitude":"5.678","timezone":"Europe/Paris"}"""
        assertEquals(IpGeo(null, 1.23, 5.68, "Europe/Paris"), parseIpGeo(noCity))
    }

    @Test
    fun geojsWithoutCoordinatesOrZoneIsNull() {
        assertNull(parseIpGeo("""{"city":"X","timezone":"Europe/Paris"}"""))
        assertNull(parseIpGeo("""{"latitude":"1","longitude":"2"}"""))
        assertNull(parseIpGeo("""{"latitude":"nan?","longitude":"2","timezone":"UTC"}"""))
    }
}
