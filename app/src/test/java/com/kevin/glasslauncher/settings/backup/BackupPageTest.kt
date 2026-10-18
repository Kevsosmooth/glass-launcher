package com.kevin.glasslauncher.settings.backup

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.settings.ui.GlassPanel
import com.kevin.glasslauncher.settings.ui.SettingsPanelWidth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val WAIT_MS = 5_000L

/** The Backup page draws its title and both rows on Robolectric's stand-in TV. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class BackupPageTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(startOnImport: Boolean) {
        rule.setContent {
            GlassPanel("panel", width = SettingsPanelWidth) { BackupPage(Prefs(), restore = {}, startOnImport = startOnImport) }
        }
        rule.waitForIdle()
    }

    private fun assertShows(vararg texts: String) {
        texts.forEach { text ->
            rule.waitUntil(WAIT_MS) { rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        }
    }

    @Test
    fun showsTitleAndBothRowsWithExportFocused() {
        show(startOnImport = false)
        assertShows("Backup", "Export settings", "Import settings")
        rule.onNode(hasText("Export settings")).assertIsFocused()
    }

    @Test
    fun opensOnImportWhenThatRowWasPicked() {
        show(startOnImport = true)
        assertShows("Import settings")
        rule.onNode(hasText("Import settings")).assertIsFocused()
    }
}
