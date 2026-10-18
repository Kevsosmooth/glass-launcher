package com.kevin.glasslauncher.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.settings.ui.GlassPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-land")
class SettingsPagesTest {
    @get:Rule
    val rule = createComposeRule()

    private fun press(key: Key) = rule.onRoot().performKeyInput { pressKey(key) }

    @Test
    fun mainPageStartsOnLookAndDownSelectOpensWallpaper() {
        val selected = mutableListOf<SettingId>()
        rule.setContent {
            GlassPanel("panel") {
                MainSettingsPage(settingsSections(Prefs(), false, "1.0"), null, 0) { selected += it }
            }
        }
        rule.onNode(hasText("Look", substring = true)).assertIsFocused()
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        assertEquals(listOf(SettingId.Wallpaper), selected)
    }

    @Test
    fun mainPageFocusesTheRowItCameBackTo() {
        rule.setContent {
            GlassPanel("panel") {
                MainSettingsPage(settingsSections(Prefs(), false, "1.0"), SettingId.MakeDefault, 0) {}
            }
        }
        rule.onNode(hasText("Make Glass your home screen", substring = true)).assertIsFocused()
    }

    @Test
    fun wallpaperPageFocusesTheCurrentSwatch() {
        var picked: String? = null
        rule.setContent {
            GlassPanel("panel") { WallpaperPage("ocean") { picked = it } }
        }
        rule.onNode(hasContentDescription("Ocean, current wallpaper")).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals("ocean", picked)
    }

    @Test
    fun wallpaperRowsScrollLeftAndRightPastTheEdge() {
        var picked: String? = null
        rule.setContent {
            GlassPanel("panel") { WallpaperPage("aurora") { picked = it } }
        }
        repeat(5) { press(Key.DirectionRight) }
        rule.onNode(hasContentDescription("Graphite")).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals("graphite", picked)
        repeat(5) { press(Key.DirectionLeft) }
        rule.onNode(hasContentDescription("Aurora, current wallpaper")).assertIsFocused()
    }

    @Test
    fun wallpaperPageStartsOnACurrentPhotoAtTheEndOfItsRow() {
        rule.setContent {
            GlassPanel("panel") { WallpaperPage("carina-nebula") { } }
        }
        rule.onNode(hasContentDescription("Carina Nebula, current wallpaper")).assertIsFocused()
    }

    @Test
    fun hiddenAppsListIsSortedAndTogglesByKey() {
        val apps = listOf(AppEntry("z.pkg", "z.Main", "Zeta", true), AppEntry("a.pkg", "a.Main", "alpha", false))
        val events = mutableListOf<String>()
        rule.setContent {
            GlassPanel("panel") {
                HiddenAppsPage(apps, emptySet(), { events += it }, { events += "dock-only" }, { events += "show-all" })
            }
        }
        rule.onNode(hasText("Show only dock apps")).assertIsFocused()
        press(Key.DirectionCenter)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        press(Key.DirectionDown)
        rule.onNode(hasText("alpha", substring = true)).assertIsFocused()
        press(Key.DirectionCenter)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        assertEquals(listOf("dock-only", "show-all", "a.pkg/a.Main", "z.pkg/z.Main"), events)
    }
}
