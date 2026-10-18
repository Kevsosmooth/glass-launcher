package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.UiText
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartColumnsTest {
    private val hours = (0 until 14).map { HourForecast((22 + it) % 24, 60 + it, it * 7, 0.0, WeatherCondition.Cloudy) }

    @Test
    fun oneColumnPerHourUpToTwelve() {
        val columns = chartColumns(hours, clock24 = false)
        assertEquals(12, columns.size)
        assertEquals(listOf("10 PM", "11 PM", "12 AM", "1 AM"), columns.take(4).map { it.hourLabel })
        assertEquals("60°", columns.first().temperature)
        assertEquals(listOf("22:00", "23:00", "00:00"), chartColumns(hours, clock24 = true).take(3).map { it.hourLabel })
    }

    @Test
    fun rainLabelsFromTenPercentAndBarsFollowTheChance() {
        val columns = chartColumns(hours, clock24 = false)
        assertNull(columns[1].chanceLabel)
        assertEquals("14%", columns[2].chanceLabel)
        assertEquals(0.14f, columns[2].chance, 0.0001f)
        assertEquals(0f, columns[0].chance, 0f)
    }

    @Test
    fun dayAndPlaceLabels() {
        assertEquals(UiText.Res(R.string.weather_today), dayLabel(0, LocalDate.of(2026, 10, 18)))
        assertEquals(UiText.Raw("Mon"), dayLabel(1, LocalDate.of(2026, 10, 19), Locale.US))
        assertEquals(UiText.Raw("Paris"), placeLabel("Paris, Ile-de-France, France"))
        assertEquals(UiText.Res(R.string.weather_your_location), placeLabel(null))
        assertNull(dayChanceLabel(DayForecast(LocalDate.of(2026, 10, 18), WeatherCondition.Cloudy, 1, 0, 9)))
    }
}
