package com.kevin.glasslauncher.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.Glyph
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.RoundIconButton
import com.kevin.glasslauncher.settings.ui.focusOnShow

private val TileGap = 8.dp

/** What the controls page shows. */
data class ControlsState(
    val clock24: Boolean,
    val sleepAtMillis: Long,
    val hasInputs: Boolean,
    val message: ControlsMessage?,
)

/** What each control does; [onSleepTimer] gets the new timer length in minutes (0 = off). */
class ControlsActions(
    val onSleep: () -> Unit,
    val onSleepTimer: (Int) -> Unit,
    val onWifi: () -> Unit,
    val onInputs: () -> Unit,
    val onDisplay: () -> Unit,
    val onSound: () -> Unit,
    val onAndroidSettings: () -> Unit,
    val onLauncherSettings: () -> Unit,
)

/**
 * Compact like tvOS Control Center: clock line, Sleep and Sleep timer tiles, an Inputs tile only
 * when the TV has pass-through inputs, then round buttons (Wi-Fi, Display, Sound, Android settings,
 * Launcher settings) with the focused one's name underneath, and any message. Focus starts on
 * Sleep, or on Inputs after the inputs list closes.
 */
@Composable
fun ColumnScope.ControlsHome(state: ControlsState, focusInputs: Boolean, actions: ControlsActions) {
    val remaining by rememberRemainingMinutes(state.sleepAtMillis)
    val inputsFocused = focusInputs && state.hasInputs
    var caption by remember { mutableStateOf<String?>(null) }
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        ControlsHeader(state.clock24)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            ControlTile(
                stringResource(R.string.panels_sleep), null, ControlGlyphs.Moon, actions.onSleep,
                Modifier.weight(1f).then(if (inputsFocused) Modifier else Modifier.focusOnShow()),
            )
            ControlTile(
                stringResource(R.string.panels_sleep_timer),
                if (remaining > 0) stringResource(R.string.panels_timer_left, remaining) else stringResource(R.string.panels_timer_off),
                ControlGlyphs.Timer,
                { actions.onSleepTimer(nextTimerMinutes(remaining)) },
                Modifier.weight(1f),
            )
        }
        if (state.hasInputs) {
            ControlTile(
                stringResource(R.string.panels_inputs), null, ControlGlyphs.Input, actions.onInputs,
                Modifier.fillMaxWidth().padding(top = TileGap).then(if (inputsFocused) Modifier.focusOnShow() else Modifier),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = TileGap), horizontalArrangement = Arrangement.SpaceBetween) {
            roundButtons(actions).forEach { (label, glyph, onClick) ->
                val name = stringResource(label)
                RoundIconButton(name, glyph, onClick, onFocusChange = { focused ->
                    if (focused) caption = name else if (caption == name) caption = null
                })
            }
        }
        caption?.let {
            BasicText(
                it,
                Modifier.fillMaxWidth().padding(top = 6.dp),
                style = PanelStyle.Value.copy(color = PanelStyle.SecondaryText, textAlign = TextAlign.Center),
                maxLines = 1,
            )
        }
        state.message?.let { ControlsMessageView(it) }
    }
}

private data class RoundButton(val label: Int, val glyph: Glyph, val onClick: () -> Unit)

private fun roundButtons(actions: ControlsActions) = listOf(
    RoundButton(R.string.panels_wifi, ControlGlyphs.Wifi, actions.onWifi),
    RoundButton(R.string.panels_display, ControlGlyphs.Display, actions.onDisplay),
    RoundButton(R.string.panels_sound, ControlGlyphs.Sound, actions.onSound),
    RoundButton(R.string.panels_android_settings, ControlGlyphs.Gear, actions.onAndroidSettings),
    RoundButton(R.string.panels_launcher_settings, ControlGlyphs.Tiles, actions.onLauncherSettings),
)
