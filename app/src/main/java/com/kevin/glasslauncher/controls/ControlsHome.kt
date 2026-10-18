package com.kevin.glasslauncher.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
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
import com.kevin.glasslauncher.settings.ui.RoundButtonSize
import com.kevin.glasslauncher.settings.ui.RoundIconButton
import com.kevin.glasslauncher.settings.ui.focusOnShow

private val TileGap = 10.dp
private val TileWidth = 136.dp
private const val ROUND_SLOTS = 4

/** Width of the controls grid: two tile columns. */
val ControlsWidth = TileWidth * 2 + TileGap

/** Round buttons sit on four even slots across the grid, however many there are. */
private val RoundGap = (ControlsWidth - RoundButtonSize * ROUND_SLOTS) / (ROUND_SLOTS - 1)

/**
 * What the controls page shows; [wifi] is the network name or connection state (null while it is
 * read), [wallpaper] the current wallpaper's name.
 */
data class ControlsState(
    val sleepAtMillis: Long,
    val wifi: String?,
    val wallpaper: String,
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
    val onWallpaper: () -> Unit,
    val onBluetooth: () -> Unit,
    val onAndroidSettings: () -> Unit,
    val onLauncherSettings: () -> Unit,
)

/**
 * The tvOS Control Center grid: a big square Sleep tile with Wi-Fi (network name) and Sleep timer
 * stacked beside it, Sound and Display below, a wide Wallpaper tile (the quickest way to change
 * the background), then round buttons (Inputs when the TV has
 * pass-through inputs, Bluetooth, Android settings, Launcher settings) with the focused one's name
 * underneath, and any message. Focus starts on Sleep, or on Inputs after the inputs list closes.
 */
@Composable
fun ControlsHome(state: ControlsState, focusInputs: Boolean, actions: ControlsActions, modifier: Modifier = Modifier) {
    val remaining by rememberRemainingMinutes(state.sleepAtMillis)
    val inputsFocused = focusInputs && state.hasInputs
    var caption by remember { mutableStateOf<String?>(null) }
    Column(modifier.width(ControlsWidth), verticalArrangement = Arrangement.spacedBy(TileGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            SquareTile(
                stringResource(R.string.panels_sleep), ControlGlyphs.Power, actions.onSleep,
                Modifier.size(TileWidth, ControlTileHeight * 2 + TileGap).then(if (inputsFocused) Modifier else Modifier.focusOnShow()),
            )
            Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
                ControlTile(stringResource(R.string.panels_wifi), state.wifi, ControlGlyphs.Wifi, actions.onWifi, Modifier.width(TileWidth))
                ControlTile(
                    stringResource(R.string.panels_sleep_timer),
                    if (remaining > 0) stringResource(R.string.panels_timer_left, remaining) else stringResource(R.string.panels_timer_off),
                    ControlGlyphs.Timer,
                    { actions.onSleepTimer(nextTimerMinutes(remaining)) },
                    Modifier.width(TileWidth),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            ControlTile(stringResource(R.string.panels_sound), null, ControlGlyphs.Sound, actions.onSound, Modifier.width(TileWidth))
            ControlTile(stringResource(R.string.panels_display), null, ControlGlyphs.Display, actions.onDisplay, Modifier.width(TileWidth))
        }
        ControlTile(
            stringResource(R.string.panels_wallpaper), state.wallpaper, ControlGlyphs.Photo, actions.onWallpaper,
            Modifier.fillMaxWidth(),
        )
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(RoundGap)) {
            roundButtons(state.hasInputs, actions).forEach { (label, glyph, onClick) ->
                val name = stringResource(label)
                val focusFirst = inputsFocused && label == R.string.panels_inputs
                RoundIconButton(name, glyph, onClick, if (focusFirst) Modifier.focusOnShow() else Modifier, onFocusChange = { focused ->
                    if (focused) caption = name else if (caption == name) caption = null
                })
            }
        }
        BasicText(
            caption.orEmpty(),
            Modifier.fillMaxWidth(),
            style = PanelStyle.Value.copy(color = PanelStyle.Text, textAlign = TextAlign.Center),
            maxLines = 1,
        )
        state.message?.let { ControlsMessageView(it) }
    }
}

private data class RoundButton(val label: Int, val glyph: Glyph, val onClick: () -> Unit)

private fun roundButtons(hasInputs: Boolean, actions: ControlsActions) = listOfNotNull(
    if (hasInputs) RoundButton(R.string.panels_inputs, ControlGlyphs.Input, actions.onInputs) else null,
    RoundButton(R.string.system_bluetooth, ControlGlyphs.Bluetooth, actions.onBluetooth),
    RoundButton(R.string.panels_android_settings, ControlGlyphs.Gear, actions.onAndroidSettings),
    RoundButton(R.string.panels_launcher_settings, ControlGlyphs.Tiles, actions.onLauncherSettings),
)
