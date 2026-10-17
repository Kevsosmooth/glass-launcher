package com.kevin.glasslauncher.home

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.ui.TestTags
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A held D-pad centre opens the tile menu; a quick press launches. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class AppTileLongPressTest {
    @get:Rule
    val rule = createComposeRule()

    private val app = AppEntry("p", "p.Main", "App", isLeanback = true)
    private val events = mutableListOf<String>()

    private fun show() {
        val env = TileEnv(
            art = object : TileArtSource {
                override fun peek(app: AppEntry) = null

                override suspend fun load(app: AppEntry) = null
            },
            tracker = DirectionTracker(),
            onLaunch = { events += "launch" },
            onLongPress = { _, place, _ -> events += "menu $place" },
        )
        rule.setContent { AppTile(app, env, TilePlace.Grid) }
        rule.onNodeWithTag(TestTags.GRID_TILE).requestFocus()
    }

    @Test
    fun quickSelectLaunches() {
        show()
        rule.onNodeWithTag(TestTags.GRID_TILE).performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("launch"), events)
    }

    @Test
    fun heldSelectOpensTheMenuAndDoesNotLaunch() {
        show()
        val tile = rule.onNodeWithTag(TestTags.GRID_TILE)
        tile.performKeyInput { keyDown(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(1_000)
        tile.performKeyInput { keyUp(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(listOf("menu Grid"), events)
    }

    /** What `adb shell input keyevent --longpress` and the input dispatcher's key repeat send. */
    @Test
    fun aLongPressRepeatOpensTheMenuAtOnce() {
        show()
        val tile = rule.onNodeWithTag(TestTags.GRID_TILE)
        val code = NativeKeyEvent.KEYCODE_DPAD_CENTER
        tile.performKeyPress(KeyEvent(NativeKeyEvent(0, 0, NativeKeyEvent.ACTION_DOWN, code, 0)))
        tile.performKeyPress(KeyEvent(NativeKeyEvent(0, 0, NativeKeyEvent.ACTION_DOWN, code, 1, 0, 0, 0, NativeKeyEvent.FLAG_LONG_PRESS)))
        tile.performKeyPress(KeyEvent(NativeKeyEvent(0, 0, NativeKeyEvent.ACTION_UP, code, 0)))
        rule.waitForIdle()
        assertEquals(listOf("menu Grid"), events)
    }
}
