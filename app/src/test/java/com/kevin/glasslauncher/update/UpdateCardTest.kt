package com.kevin.glasslauncher.update

import android.app.Application
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.kevin.glasslauncher.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class UpdateCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()

    private val release = ReleaseInfo("0.3.0", "Glass v0.3.0", "Faster home screen", "https://github.com/x.apk", 10, null, "")

    @Test
    fun showsVersionNotesAndButtonsAndUpdates() {
        var updates = 0
        var laters = 0
        rule.setContent {
            UpdateCard(UpdateState.Available(release, dismissed = false), { updates++ }, { laters++ }, Modifier.width(420.dp))
        }
        rule.onNodeWithText("Update available: 0.3.0").assertIsDisplayed()
        rule.onNodeWithText("What's new").assertIsDisplayed()
        rule.onNodeWithText("Faster home screen").assertIsDisplayed()
        rule.onNodeWithText("Later").assertIsDisplayed()
        rule.onNodeWithText("Update").performClick()
        assertEquals(1, updates)
        assertEquals(0, laters)
    }

    @Test
    fun downloadShowsProgressAndIgnoresPresses() {
        var updates = 0
        rule.setContent { UpdateCard(UpdateState.Downloading(release, 0.42f), { updates++ }, {}, Modifier.width(420.dp)) }
        rule.onNodeWithText("Downloading 42%").performClick()
        rule.onNodeWithText("Later").assertDoesNotExist()
        assertEquals(0, updates)
    }

    @Test
    fun askingForInstallsTellsFireTvUsersWhereToGo() {
        rule.setContent { UpdateCard(UpdateState.NeedsPermission(release), {}, {}, Modifier.width(420.dp)) }
        rule.onNodeWithText("turn on Glass under Install unknown apps", substring = true).assertExists()

        shadowOf(app.packageManager).setSystemFeature("amazon.hardware.fire_tv", true)
        assertEquals(R.string.update_needs_permission_fire, permissionHintRes(app))
    }

    @Test
    fun dismissedCardDrawsNothing() {
        rule.setContent { UpdateCard(UpdateState.Available(release, dismissed = true), {}, {}) }
        rule.onNodeWithText("Update available: 0.3.0").assertDoesNotExist()
    }
}
