package com.kevin.glasslauncher.settings.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class GlassPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private fun press(key: Key) = rule.onRoot().performKeyInput { pressKey(key) }

    @Test
    fun focusOnShowFocusesAPlainRow() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                ListRow("One", "A", {}, Modifier.testTag("one").focusOnShow())
            }
        }
        rule.onNodeWithTag("one").assertIsFocused()
    }

    @Test
    fun focusStartsOnFirstRowAndCannotLeaveThePanel() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.testTag("home").size(200.dp).focusable())
                GlassPanel("panel") {
                    ListRow("One", "A", {}, Modifier.testTag("one").focusOnShow())
                    ToggleRow("Two", checked = true, onToggle = {}, modifier = Modifier.testTag("two"))
                }
            }
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("one").assertIsFocused()
        press(Key.DirectionLeft)
        rule.onNodeWithTag("one").assertIsFocused()
        press(Key.DirectionUp)
        rule.onNodeWithTag("one").assertIsFocused()
        press(Key.DirectionDown)
        rule.onNodeWithTag("two").assertIsFocused()
        press(Key.DirectionDown)
        rule.onNodeWithTag("two").assertIsFocused()
        press(Key.DirectionLeft)
        rule.onNodeWithTag("two").assertIsFocused()
    }

    /** The panel is as tall as its rows, not the screen, and sits in the top-right corner. */
    @Test
    fun panelWrapsItsContentInTheTopRightCorner() {
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                GlassPanel("panel") {
                    ListRow("One", "A", {}, Modifier.focusOnShow())
                    ToggleRow("Two", checked = false, onToggle = {})
                }
            }
        }
        rule.mainClock.advanceTimeBy(1_000)
        val bounds = rule.onNodeWithTag("panel").getUnclippedBoundsInRoot()
        assertTrue("height ${bounds.height}", bounds.height < 140.dp)
        assertTrue("width ${bounds.width}", bounds.width == CompactPanelWidth)
        assertTrue("top ${bounds.top}", bounds.top < 40.dp)
        assertTrue("right ${bounds.right}", bounds.right > 900.dp)
    }
}
