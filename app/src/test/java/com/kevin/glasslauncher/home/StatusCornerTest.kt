package com.kevin.glasslauncher.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import com.kevin.glasslauncher.data.TempUnit
import com.kevin.glasslauncher.weather.WeatherCondition
import com.kevin.glasslauncher.weather.WeatherNow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class StatusCornerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun quickControlsButtonOpensControls() {
        var opened = 0
        rule.setContent { StatusCorner(clock24 = false, active = false, onOpen = { opened++ }) }
        rule.onNode(hasContentDescription("Quick Controls")).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun weatherShowsBeforeTheTimeWhenThereIsData() {
        val weather = WeatherNow(72, TempUnit.Fahrenheit, WeatherCondition.PartlyCloudy, 0L, 40.65, -73.95)
        rule.setContent { StatusCorner(clock24 = false, active = false, onOpen = {}, weather = weather) }
        rule.onNode(hasText("72\u00B0"), useUnmergedTree = true).assertExists()
        rule.onNode(hasContentDescription("Partly cloudy, 72\u00B0")).assertExists()
    }

    @Test
    fun weatherIsAFocusableButtonOnlyWithDataAndAnOpener() {
        val weather = WeatherNow(72, TempUnit.Fahrenheit, WeatherCondition.Rain, 0L, 40.65, -73.95)
        var data by mutableStateOf<WeatherNow?>(weather)
        var opened = 0
        rule.setContent { StatusCorner(clock24 = false, active = false, onOpen = {}, weather = data, onOpenWeather = { opened++ }) }
        val button = hasContentDescription("Rain, 72\u00B0") and isFocusable()
        rule.onNode(button).performClick()
        assertEquals(1, opened)
        data = null
        rule.onNode(button).assertDoesNotExist()
        rule.onNode(hasText("\u00B0", substring = true), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun closingAPanelPutsFocusBackOnTheButtonThatOpenedIt() {
        val weather = WeatherNow(72, TempUnit.Fahrenheit, WeatherCondition.Rain, 0L, 40.65, -73.95)
        val memory = FocusMemory()
        rule.setContent {
            StatusCorner(clock24 = false, active = false, onOpen = {}, weather = weather, onOpenWeather = {}, focusMemory = memory)
        }
        val button = hasContentDescription("Rain, 72\u00B0") and isFocusable()
        rule.onNode(button).requestFocus()
        memory.save()
        rule.onNode(hasContentDescription("Quick Controls")).requestFocus()
        rule.runOnIdle { assertTrue(memory.restore()) }
        rule.onNode(button).assertIsFocused()
    }

    @Test
    fun weatherWithoutAnOpenerIsNotFocusable() {
        val weather = WeatherNow(72, TempUnit.Fahrenheit, WeatherCondition.Rain, 0L, 40.65, -73.95)
        rule.setContent { StatusCorner(clock24 = false, active = false, onOpen = {}, weather = weather) }
        rule.onNode(hasContentDescription("Rain, 72\u00B0")).assert(isFocusable().not())
    }

    @Test
    fun noWeatherShowsNoTemperature() {
        rule.setContent { StatusCorner(clock24 = false, active = false, onOpen = {}) }
        rule.onNode(hasText("\u00B0", substring = true), useUnmergedTree = true).assertDoesNotExist()
    }
}
