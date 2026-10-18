package com.kevin.glasslauncher.controls

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.glass.WallpaperCatalog
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.settings.SettingsPage
import com.kevin.glasslauncher.settings.UiText
import com.kevin.glasslauncher.settings.resolve
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.system.WifiSource
import com.kevin.glasslauncher.system.wifiSummary
import com.kevin.glasslauncher.ui.TestTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * tvOS-style Control Center: the status corner, then Sleep, Wi-Fi, Sleep timer, Sound, Display,
 * Wallpaper and round buttons (Inputs, Bluetooth, Android Settings, Launcher Settings), with no panel behind
 * them, under the update card when a new version is out. Wi-Fi, Sound, Display, Wallpaper and Bluetooth open our Settings pages. Focus starts on Sleep and
 * cannot leave. The inputs list replaces the grid in a glass card; Back from it returns here,
 * top-level Back is LauncherRoot's. Anything that fails to open shows a short message instead of
 * crashing.
 */
@Composable
fun QuickControlsPanel(
    container: AppContainer,
    onClose: () -> Unit,
    onOpenSettings: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs by container.prefs.prefs.collectAsStateWithLifecycle(initialValue = null)
    val sleep = remember(context) { SleepControl(context) }
    val inputs by produceState(emptyList<PassthroughInput>(), context) {
        value = withContext(Dispatchers.IO) { passthroughInputs(context) }
    }
    val wifi by produceState<UiText?>(null, context) {
        value = withContext(Dispatchers.IO) { wifiSummary(WifiSource(context).status()) }
    }
    var showInputs by remember { mutableStateOf(false) }
    var focusInputs by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<ControlsMessage?>(null) }
    val adminExplanation = stringResource(R.string.sleep_admin_description)
    // External screens take over the TV, so the controls close behind them.
    val open: (Intent) -> Unit = { intent ->
        if (context.tryStart(intent)) onClose() else message = ControlsMessage.CannotOpen
    }

    val closeInputs = {
        showInputs = false
        focusInputs = true
    }

    BackHandler(enabled = showInputs, onBack = closeInputs)
    val current = prefs ?: return
    ControlCenterLayout(TestTags.QUICK_CONTROLS, current.clock24, onClose, modifier) {
        if (showInputs) {
            InputsList(
                inputs,
                onPick = {
                    closeInputs()
                    open(inputIntent(it))
                },
                Modifier.width(ControlsWidth).glass(PanelStyle.PanelCorner).padding(12.dp),
            )
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                UpdateSlot(container.updates, onNoInstallScreen = { message = ControlsMessage.NoInstallScreen })
                ControlsHome(
                    state = ControlsState(
                        current.sleepAtMillis, wifi?.resolve(), WallpaperCatalog.byId(current.wallpaperId).name,
                        inputs.isNotEmpty(), message,
                    ),
                    focusInputs = focusInputs,
                    actions = ControlsActions(
                        onSleep = {
                            if (sleep.sleepNow()) {
                                onClose()
                            } else if (!context.tryStart(sleep.adminRequest(adminExplanation))) {
                                message = ControlsMessage.AdminNeeded
                            }
                        },
                        onSleepTimer = { minutes ->
                            container.scope.launch { setSleepTimer(context.applicationContext, container.prefs, minutes) }
                            if (minutes > 0 && !sleep.isAdmin) message = ControlsMessage.AdminNeeded
                        },
                        onWifi = { onOpenSettings(SettingsPage.Wifi) },
                        onInputs = { showInputs = true },
                        onDisplay = { onOpenSettings(SettingsPage.Display) },
                        onSound = { onOpenSettings(SettingsPage.Sound) },
                        onWallpaper = { onOpenSettings(SettingsPage.Wallpaper) },
                        onBluetooth = { onOpenSettings(SettingsPage.Bluetooth) },
                        onAndroidSettings = { open(Intent(Settings.ACTION_SETTINGS)) },
                        onLauncherSettings = { onOpenSettings(SettingsPage.Main) },
                    ),
                )
            }
        }
    }
}
