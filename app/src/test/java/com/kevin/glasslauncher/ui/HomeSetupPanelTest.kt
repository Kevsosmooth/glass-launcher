package com.kevin.glasslauncher.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
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
class HomeSetupPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val picked = mutableListOf<String>()

    private fun show() = rule.setContent {
        HomeSetupPanel(onSetUp = { picked += "setup" }, onClose = { picked += "close" })
    }

    private fun press(key: Key) = rule.onRoot().performKeyInput { pressKey(key) }

    @Test
    fun selectOnFirstRowOpensSetup() {
        show()
        rule.onNode(hasText("Show me how", substring = true)).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("setup"), picked)
    }

    @Test
    fun notNowCloses() {
        show()
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        assertEquals(listOf("close"), picked)
    }
}
