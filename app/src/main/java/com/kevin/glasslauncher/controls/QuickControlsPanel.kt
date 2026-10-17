package com.kevin.glasslauncher.controls

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.GlassPanel
import com.kevin.glasslauncher.ui.TestTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * tvOS-style Control Center in a compact top-right panel: clock, Sleep, Sleep timer, Inputs, and
 * round buttons for Wi-Fi, Display, Sound, Android Settings and Launcher Settings. Focus starts on Sleep and
 * cannot leave the panel. Back from the inputs list returns here; top-level Back is LauncherRoot's.
 * Anything that fails to open shows a short message in the panel instead of crashing.
 */
@Composable
fun QuickControlsPanel(
    container: AppContainer,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs by container.prefs.prefs.collectAsStateWithLifecycle(initialValue = null)
    val sleep = remember(context) { SleepControl(context) }
    val inputs by produceState(emptyList<PassthroughInput>(), context) {
        value = withContext(Dispatchers.IO) { passthroughInputs(context) }
    }
    var showInputs by remember { mutableStateOf(false) }
    var focusInputs by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<ControlsMessage?>(null) }
    val adminExplanation = stringResource(R.string.sleep_admin_description)
    // External screens take over the TV, so the panel closes behind them.
    val open: (Intent) -> Unit = { intent ->
        if (context.tryStart(intent)) onClose() else message = ControlsMessage.CannotOpen
    }

    val closeInputs = {
        showInputs = false
        focusInputs = true
    }

    BackHandler(enabled = showInputs, onBack = closeInputs)
    GlassPanel(TestTags.QUICK_CONTROLS, modifier) {
        val current = prefs ?: return@GlassPanel
        if (showInputs) {
            InputsList(inputs, onPick = {
                closeInputs()
                open(inputIntent(it))
            })
        } else {
            ControlsHome(
                state = ControlsState(current.clock24, current.sleepAtMillis, inputs.isNotEmpty(), message),
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
                    onWifi = { open(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    onInputs = { showInputs = true },
                    onDisplay = { open(Intent(Settings.ACTION_DISPLAY_SETTINGS)) },
                    onSound = { open(Intent(Settings.ACTION_SOUND_SETTINGS)) },
                    onAndroidSettings = { open(Intent(Settings.ACTION_SETTINGS)) },
                    onLauncherSettings = onOpenSettings,
                ),
            )
        }
    }
}
