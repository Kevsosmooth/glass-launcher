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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.settings.ui.GlassPanel
import com.kevin.glasslauncher.settings.ui.SettingsPanelWidth
import com.kevin.glasslauncher.ui.TestTags
import com.kevin.glasslauncher.system.AboutPage
import com.kevin.glasslauncher.system.BluetoothPage
import com.kevin.glasslauncher.system.DeviceSummaryReader
import com.kevin.glasslauncher.system.DisplayPage
import com.kevin.glasslauncher.system.SoundPage
import com.kevin.glasslauncher.system.StoragePage
import com.kevin.glasslauncher.system.WifiPage
import com.kevin.glasslauncher.system.rememberPolled
import kotlinx.coroutines.launch

private const val SUMMARY_POLL_MS = 5_000L

/**
 * Launcher Settings, drawn over home as a compact glass panel. Focus starts on the first row and
 * cannot leave the panel. Back on a sub-page returns to the main page (focused on the row that
 * opened it); Back on the main page is LauncherRoot's and closes the panel.
 */
@Composable
fun SettingsScreen(
    container: AppContainer,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    startPage: SettingsPage = SettingsPage.Main,
) {
    val context = LocalContext.current
    val prefs by container.prefs.prefs.collectAsStateWithLifecycle(initialValue = null)
    val apps by container.apps.apps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var page by remember(startPage) { mutableStateOf(startPage) }
    val summaryReader = remember(context) { DeviceSummaryReader(context) }
    val device by rememberPolled(Unit, SUMMARY_POLL_MS, DeviceSummary(), active = page == SettingsPage.Main) { summaryReader.read() }
    var returnTo by remember { mutableStateOf<SettingId?>(null) }
    var confirmingReset by remember { mutableStateOf(false) }
    var focusKey by remember { mutableIntStateOf(0) }
    val apply: (PrefChange) -> Unit = { change -> scope.launch { change.applyTo(container.prefs) } }

    BackHandler(enabled = page != SettingsPage.Main) { page = SettingsPage.Main }
    GlassPanel(TestTags.SETTINGS, modifier, width = SettingsPanelWidth) {
        val current = prefs ?: return@GlassPanel
        when (page) {
            SettingsPage.Main -> MainSettingsPage(
                sections = settingsSections(current, confirmingReset, BuildConfig.DEBUG, BuildConfig.VERSION_NAME, device),
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
                        // The panel stays open behind Android's screen, so Back lands here again.
                        is SettingsAction.OpenNative -> action.page.open(context)
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
            SettingsPage.Wifi -> WifiPage()
            SettingsPage.Bluetooth -> BluetoothPage()
            SettingsPage.Sound -> SoundPage()
            SettingsPage.Display -> DisplayPage()
            SettingsPage.Storage -> StoragePage()
            SettingsPage.About -> AboutPage()
        }
    }
}
