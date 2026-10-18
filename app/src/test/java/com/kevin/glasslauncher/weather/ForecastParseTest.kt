package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.TempUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs on Robolectric for Android's real org.json. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ForecastParseTest {
    private val json = """
        {"timezone":"America/New_York","utc_offset_seconds":-14400,
         "current":{"time":"2026-10-18T14:45","temperature_2m":71.6,"weather_code":2,"is_day":1,
                    "apparent_temperature":69.8,"relative_humidity_2m":40,"wind_speed_10m":7.6},
         "hourly":{"time":["2026-10-18T13:00","2026-10-18T14:00","2026-10-18T15:00","2026-10-18T16:00","bad"],
                   "temperature_2m":[70.0,71.4,72.6,null,60],
                   "precipitation_probability":[0,5,null,80,0],
                   "precipitation":[0.0,0.0,0.3,1.2,0],
                   "weather_code":[1,2,61,63,0],
                   "is_day":[1,1,1,0,0]},
         "daily":{"time":["2026-10-18","2026-10-19"],
                  "weather_code":[61,71],
                  "temperature_2m_max":[75.2,50.4],
                  "temperature_2m_min":[60.6,null],
                  "precipitation_probability_max":[85,null]}}
    """.trimIndent()

    @Test
    fun hoursStartAtTheCurrentHourAndSkipBadEntries() {
        val details = parseForecast(json, TempUnit.Fahrenheit, 0L, 40.71, -74.01)?.details
        assertEquals(
            listOf(
                HourForecast(14, 71, 5, 0.0, WeatherCondition.PartlyCloudy),
                HourForecast(15, 73, 0, 0.3, WeatherCondition.Rain),
            ),
            details?.hours,
        )
    }

    @Test
    fun currentExtrasAndDaysAreRead() {
        val details = parseForecast(json, TempUnit.Fahrenheit, 0L, 40.71, -74.01)?.details
        assertEquals(70, details?.feelsLike)
        assertEquals(40, details?.humidity)
        assertEquals(8, details?.wind)
        // The second day has no low, so it is skipped.
        assertEquals(listOf(DayForecast(LocalDate.of(2026, 10, 18), WeatherCondition.Rain, 75, 61, 85)), details?.days)
    }

    @Test
    fun keepsAtMostTwelveHoursAndFiveDays() {
        val hours = (0 until 24).joinToString(",") { "\"2026-10-18T%02d:00\"".format(it) }
        val values = (0 until 24).joinToString(",") { "1" }
        val days = (1..7).joinToString(",") { "\"2026-10-%02d\"".format(it) }
        val sevens = (1..7).joinToString(",") { "1" }
        val long = """
            {"current":{"time":"2026-10-18T00:10","temperature_2m":1,"weather_code":0,"is_day":0},
             "hourly":{"time":[$hours],"temperature_2m":[$values],"weather_code":[$values]},
             "daily":{"time":[$days],"weather_code":[$sevens],"temperature_2m_max":[$sevens],"temperature_2m_min":[$sevens]}}
        """.trimIndent()
        val details = parseForecast(long, TempUnit.Celsius, 0L, 0.0, 0.0)?.details
        assertEquals(12, details?.hours?.size)
        assertEquals(0, details?.hours?.first()?.hour)
        assertEquals(5, details?.days?.size)
        assertNull(details?.feelsLike)
    }

    @Test
    fun noHoursOrDaysMeansNoDetails() {
        val bare = """{"current":{"time":"2026-10-18T14:45","temperature_2m":10,"weather_code":3,"is_day":1}}"""
        assertNull(parseForecast(bare, TempUnit.Celsius, 0L, 0.0, 0.0)?.details)
    }
}
