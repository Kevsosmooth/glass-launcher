package com.kevin.glasslauncher.edit

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class MoveKeysTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private var active by mutableStateOf(false)

    private fun show(active: Boolean) {
        this.active = active
        val guard = SelectGuard()
        rule.setContent {
            Box(
                Modifier
                    .moveKeys(this.active, guard, onStep = { events += "step $it" }, onDrop = { events += "drop" })
                    .size(100.dp)
                    .testTag("tile")
                    .focusable(),
            )
        }
        rule.onNodeWithTag("tile").requestFocus()
    }

    private fun press(key: Key) = rule.onNodeWithTag("tile").performKeyInput { pressKey(key) }

    @Test
    fun leftAndRightStepAndAFreshSelectDrops() {
        show(active = false)
        rule.onNodeWithTag("tile").performKeyInput { keyDown(Key.DirectionCenter) }
        active = true
        rule.onNodeWithTag("tile").performKeyInput { keyUp(Key.DirectionCenter) }
        press(Key.DirectionRight)
        press(Key.DirectionLeft)
        press(Key.DirectionCenter)
        assertEquals(listOf("step 1", "step -1", "drop"), events)
    }

    @Test
    fun inactiveDoesNothing() {
        show(active = false)
        press(Key.DirectionRight)
        press(Key.DirectionCenter)
        assertEquals(emptyList<String>(), events)
    }
}
