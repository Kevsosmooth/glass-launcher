package com.kevin.glasslauncher.system

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.kevin.glasslauncher.settings.ui.GlassPanel
import com.kevin.glasslauncher.settings.ui.SettingsPanelWidth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each device page draws its title and its link to Android Settings on Robolectric's stand-in TV. */
private const val WAIT_MS = 5_000L

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class SystemPagesTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(page: @Composable ColumnScope.() -> Unit) {
        rule.setContent { GlassPanel("panel", width = SettingsPanelWidth) { page() } }
        rule.waitForIdle()
    }

    /** Waits for the background read to land, then checks each text is on screen at least once. */
    private fun assertShows(vararg texts: String) {
        texts.forEach { text ->
            rule.waitUntil(WAIT_MS) { rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun wifiPage() {
        show { WifiPage() }
        assertShows("Wi-Fi", "Join, forget or turn off Wi-Fi")
    }

    @Test
    fun bluetoothPage() {
        show { BluetoothPage() }
        assertShows("Bluetooth", "Pair a remote or accessory")
    }

    @Test
    fun soundPageSliderTakesLeftAndRight() {
        show { SoundPage() }
        assertShows("Sound", "Volume", "Surround sound and audio output")
        rule.onRoot().performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionLeft)
        }
        rule.waitForIdle()
        assertShows("Volume")
    }

    @Test
    fun displayPage() {
        show { DisplayPage() }
        assertShows("Display", "Change resolution and display")
    }

    @Test
    fun storagePage() {
        show { StoragePage() }
        assertShows("Storage", "Manage storage and apps")
    }

    @Test
    fun aboutPage() {
        show { AboutPage() }
        assertShows("About this TV", "Date and time", "System update")
    }
}
