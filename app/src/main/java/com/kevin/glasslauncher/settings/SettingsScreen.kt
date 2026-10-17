package com.kevin.glasslauncher.settings

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.settings.ui.GlassPanel
import com.kevin.glasslauncher.settings.ui.SettingsPanelWidth
import com.kevin.glasslauncher.ui.TestTags
import kotlinx.coroutines.launch

/**
 * Launcher Settings, drawn over home as a compact glass panel. Focus starts on the first row and
 * cannot leave the panel. Back on a sub-page returns to the main page (focused on the row that
 * opened it); Back on the main page is LauncherRoot's and closes the panel.
 */
@Composable
fun SettingsScreen(container: AppContainer, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val prefs by container.prefs.prefs.collectAsStateWithLifecycle(initialValue = null)
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(SettingsPage.Main) }
    var returnTo by remember { mutableStateOf<SettingId?>(null) }
    var confirmingReset by remember { mutableStateOf(false) }
    var focusKey by remember { mutableIntStateOf(0) }
    val apply: (PrefChange) -> Unit = { change -> scope.launch { change.applyTo(container.prefs) } }

    BackHandler(enabled = page != SettingsPage.Main) { page = SettingsPage.Main }
    GlassPanel(TestTags.SETTINGS, modifier, width = SettingsPanelWidth) {
        val current = prefs ?: return@GlassPanel
        when (page) {
            SettingsPage.Main -> MainSettingsPage(
                sections = settingsSections(current, confirmingReset, BuildConfig.DEBUG, BuildConfig.VERSION_NAME),
                focusId = returnTo,
                focusKey = focusKey,
                onSelect = { id ->
                    when (val action = onSelect(id, current)) {
                        is SettingsAction.Change -> {
                            if (action.change == PrefChange.ResetDock) {
                                // The confirm row goes away: keep focus in the panel on Reset dock.
                                confirmingReset = false
                                returnTo = SettingId.ResetDock
                                focusKey++
                            }
                            apply(action.change)
                        }
                        is SettingsAction.Open -> {
                            returnTo = id
                            page = action.page
                        }
                        SettingsAction.ToggleResetConfirm -> confirmingReset = !confirmingReset
                        SettingsAction.None -> Unit
                    }
                },
            )
            SettingsPage.Wallpaper -> WallpaperPage(current.wallpaperId) { apply(PrefChange.SetWallpaper(it)) }
            SettingsPage.HiddenApps -> HiddenAppsPage(
                apps = apps,
                hidden = current.hidden,
                onToggle = { apply(PrefChange.SetHidden(toggleHidden(current.hidden, it))) },
                onKeepDockOnly = { apps?.let { apply(PrefChange.SetHidden(hideAllButDock(it, current))) } },
                onShowAll = { apply(PrefChange.SetHidden(emptySet())) },
            )
            SettingsPage.MakeDefault -> MakeDefaultPage()
        }
    }
}
