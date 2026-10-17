package com.kevin.glasslauncher.home

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.motion.DirectionTracker
import com.kevin.glasslauncher.ui.TestTags
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Closing a panel puts focus back on the tile it was opened from, not the first one. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class FocusMemoryTest {
    @get:Rule
    val rule = createComposeRule()

    private val memory = FocusMemory()
    private var panelOpen by mutableStateOf(false)
    private var showSecond by mutableStateOf(true)

    private fun show() {
        val env = TileEnv(
            art = object : TileArtSource {
                override fun peek(app: AppEntry) = null

                override suspend fun load(app: AppEntry) = null
            },
            tracker = DirectionTracker(),
            onLaunch = {},
            onLongPress = { _, _, _ -> },
            focusMemory = memory,
        )
        rule.setContent {
            Row {
                listOf("a", "b").forEach { key ->
                    if (key == "a" || showSecond) {
                        AppTile(AppEntry(key, "$key.Main", key, isLeanback = true), env, TilePlace.Grid, Modifier.width(160.dp))
                    }
                }
                if (panelOpen) {
                    val panel = remember { FocusRequester() }
                    Box(Modifier.size(50.dp).testTag("panel").focusRequester(panel).focusable())
                    LaunchedEffect(Unit) { panel.requestFocus() }
                }
            }
        }
    }

    private fun tile(i: Int) = rule.onAllNodesWithTag(TestTags.GRID_TILE)[i]

    @Test
    fun restoreReturnsToTheTileThePanelWasOpenedFrom() {
        show()
        tile(1).requestFocus()
        rule.runOnIdle { memory.save() }
        panelOpen = true
        rule.onNode(hasTestTag("panel")).assertIsFocused()
        panelOpen = false
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(memory.restore()) }
        tile(1).assertIsFocused()
    }

    @Test
    fun nothingIsRestoredAfterHomeDiscardsIt() {
        show()
        tile(1).requestFocus()
        rule.runOnIdle {
            memory.save()
            memory.discard()
            assertFalse(memory.restore())
        }
    }

    @Test
    fun aTileThatLeftTheScreenIsForgotten() {
        show()
        tile(1).requestFocus()
        rule.runOnIdle { memory.save() }
        showSecond = false
        rule.waitForIdle()
        rule.runOnIdle { assertFalse(memory.restore()) }
    }
}
