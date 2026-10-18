package com.kevin.glasslauncher.debug

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class StatsOverlayTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsFramesAndEmptyRequestList() {
        rule.setContent { StatsOverlay() }
        rule.onNode(hasText("fps", substring = true)).assertIsDisplayed()
        rule.onNode(hasText("Recent requests")).assertIsDisplayed()
        rule.onNode(hasText("No requests yet")).assertIsDisplayed()
        rule.onNode(hasText("Glass ", substring = true)).assertIsDisplayed()
    }
}
