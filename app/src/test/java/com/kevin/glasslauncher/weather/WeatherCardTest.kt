package com.kevin.glasslauncher.weather

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kevin.glasslauncher.data.TempUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class WeatherCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val hours = (0 until 12).map { i ->
        val wet = i in 2..4
        HourForecast((13 + i) % 24, 70 - i, if (wet) 70 else 0, 0.0, if (wet) WeatherCondition.Rain else WeatherCondition.Cloudy)
    }
    private val days = (0 until 5).map { DayForecast(LocalDate.of(2026, 10, 18).plusDays(it.toLong()), WeatherCondition.Rain, 75, 61, 85) }
    private val weather = WeatherNow(
        72, TempUnit.Fahrenheit, WeatherCondition.PartlyCloudy, 0L, 40.65, -73.95,
        WeatherDetails(70, 40, 8, hours, days),
    )

    @Test
    fun showsPlaceSummaryAndTwelveHours() {
        rule.setContent { WeatherCard(WeatherCardState(weather, "Brooklyn, New York, United States", clock24 = false), onClose = {}) }
        rule.onNodeWithText("Brooklyn", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Rain starting around 3 PM, stopping around 6 PM", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Feels like 70°   H 75°   L 61°", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Humidity 40%   Wind 8 mph", useUnmergedTree = true).assertExists()
        val labels = listOf("1 PM", "2 PM", "3 PM", "4 PM", "5 PM", "6 PM", "7 PM", "8 PM", "9 PM", "10 PM", "11 PM", "12 AM")
        labels.forEach { rule.onAllNodes(hasText(it, substring = false), useUnmergedTree = true).assertCountEquals(1) }
        rule.onAllNodesWithText("Today", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun selectCloses() {
        var closed = 0
        rule.setContent { WeatherCard(WeatherCardState(weather, null, clock24 = true), onClose = { closed++ }) }
        rule.onNodeWithText("Your location", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Rain starting around 15:00, stopping around 18:00", useUnmergedTree = true).performClick()
        assertEquals(1, closed)
    }

    @Test
    fun loadingAndUnavailable() {
        var state by mutableStateOf(WeatherCardState(null, null, false))
        rule.setContent { WeatherCard(state, onClose = {}) }
        rule.onNodeWithText("Weather is loading", useUnmergedTree = true).assertExists()
        state = WeatherCardState(weather.copy(details = null), null, false)
        rule.onNodeWithText("Weather unavailable", useUnmergedTree = true).assertExists()
    }
}
