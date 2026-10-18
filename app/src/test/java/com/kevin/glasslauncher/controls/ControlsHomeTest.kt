package com.kevin.glasslauncher.controls

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class ControlsHomeTest {
    @get:Rule
    val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val actions = ControlsActions(
        onSleep = { calls += "sleep" },
        onSleepTimer = { calls += "timer $it" },
        onWifi = { calls += "wifi" },
        onInputs = { calls += "inputs" },
        onDisplay = { calls += "display" },
        onSound = { calls += "sound" },
        onWallpaper = { calls += "wallpaper" },
        onBluetooth = { calls += "bluetooth" },
        onAndroidSettings = { calls += "android" },
        onLauncherSettings = { calls += "launcher" },
    )

    private fun show(hasInputs: Boolean = false, focusInputs: Boolean = false) = rule.setContent {
        ControlsHome(ControlsState(0L, "Home Wi-Fi", "Earth", hasInputs, null), focusInputs, actions)
    }

    private fun tile(title: String): SemanticsMatcher = hasText(title) and hasClickAction()

    private fun press(key: Key) = rule.onRoot().performKeyInput { pressKey(key) }

    @Test
    fun focusStartsOnTheBigSleepTile() {
        show()
        rule.onNode(tile("Sleep")).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("sleep"), calls)
    }

    @Test
    fun wifiShowsTheNetworkNameBesideSleep() {
        show()
        rule.onNode(tile("Home Wi-Fi")).assertExists()
        press(Key.DirectionRight)
        rule.onNode(tile("Wi-Fi")).assertIsFocused()
        press(Key.DirectionDown)
        rule.onNode(tile("Sleep timer")).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("timer ${nextTimerMinutes(0)}"), calls)
    }

    @Test
    fun soundAndDisplaySitUnderSleep() {
        show()
        press(Key.DirectionDown)
        rule.onNode(tile("Sound")).assertIsFocused()
        press(Key.DirectionRight)
        rule.onNode(tile("Display")).assertIsFocused()
    }

    @Test
    fun wallpaperIsOneStepDown() {
        show()
        rule.onNode(tile("Earth")).assertExists()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        rule.onNode(tile("Wallpaper")).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("wallpaper"), calls)
    }

    @Test
    fun inputsButtonOnlyWithInputs() {
        show()
        rule.onNode(hasContentDescription("Inputs")).assertDoesNotExist()
        rule.onNode(hasContentDescription("Bluetooth")).assertExists()
    }

    @Test
    fun inputsButtonTakesFocusBackAfterTheList() {
        show(hasInputs = true, focusInputs = true)
        rule.onNode(hasContentDescription("Inputs")).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("inputs"), calls)
    }
}
