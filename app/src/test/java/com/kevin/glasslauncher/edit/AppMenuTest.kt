package com.kevin.glasslauncher.edit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class AppMenuTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val picks = mutableListOf<MenuAction>()
    private var closed = 0

    private fun press(key: Key) = rule.onRoot().performKeyInput { pressKey(key) }

    private var menuShown by mutableStateOf(true)

    private fun showGridMenu(slot: DockSlot) {
        val target = MenuTarget(AppEntry("p", "p.Main", "App", true), inDock = false, TileHandle(Rect(46f, 300f, 170f, 370f), FocusRequester()))
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(100.dp).testTag("tile").focusable())
                if (menuShown) {
                    AppMenu(target, menuItems(inDock = false, slot = slot), onPick = { picks += it }, onClose = { closed++ })
                }
            }
        }
        rule.mainClock.advanceTimeBy(500)
    }

    @Test
    fun fullDockSkipsAddAndSaysWhy() {
        showGridMenu(DockSlot.Full)
        rule.onNodeWithTag(TestTags.APP_MENU).assertIsDisplayed()
        rule.onNodeWithText("Dock full").assertIsDisplayed()
        rule.onNodeWithText("Hide").assertIsFocused()
        press(Key.DirectionUp)
        rule.onNodeWithText("Hide").assertIsFocused()
    }

    @Test
    fun focusCannotLeaveTheMenu() {
        showGridMenu(DockSlot.Free)
        rule.onNodeWithText("Add to dock").assertIsFocused()
        repeat(5) { press(Key.DirectionDown) }
        rule.onNodeWithText("Uninstall").assertIsFocused()
        press(Key.DirectionLeft)
        press(Key.DirectionRight)
        rule.onNodeWithText("Uninstall").assertIsFocused()
    }

    @Test
    fun heldSelectFromTheLongPressPicksNothing() {
        menuShown = false
        showGridMenu(DockSlot.Free)
        rule.onNodeWithTag("tile").requestFocus()
        rule.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        menuShown = true
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Add to dock").assertIsFocused()
        rule.onRoot().performKeyInput {
            advanceEventTime(600)
            keyUp(Key.DirectionCenter)
        }
        assertEquals(emptyList<MenuAction>(), picks)
        press(Key.DirectionCenter)
        assertEquals(listOf(MenuAction.AddToDock), picks)
    }

    @Test
    fun backCloses() {
        showGridMenu(DockSlot.Free)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(1, closed)
    }
}
