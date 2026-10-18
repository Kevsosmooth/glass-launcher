package com.kevin.glasslauncher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.controls.ControlGlyphs
import com.kevin.glasslauncher.settings.ui.RoundButtonSize
import com.kevin.glasslauncher.settings.ui.RoundIconButton
import com.kevin.glasslauncher.weather.WeatherButton
import com.kevin.glasslauncher.weather.WeatherLabel
import com.kevin.glasslauncher.weather.WeatherNow

private val CornerButton = 36.dp
private val TimeStyle = TextStyle(
    color = Color.White,
    fontSize = 17.sp,
    shadow = Shadow(Color(0x66000000), Offset(0f, 1f), 3f),
)

/**
 * The top-right corner, like tvOS: the [weather] (when there is any), the time as plain text,
 * then a small round Quick Controls button. Home and Quick Controls both draw it in the same
 * place; [active] keeps the button white while Quick Controls is open. With [onOpenWeather] the
 * weather is a focusable capsule that opens the weather card; without it, plain text. With
 * [focusMemory], closing the card or Quick Controls puts focus back on the button that opened it.
 */
@Composable
fun StatusCorner(
    clock24: Boolean,
    active: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    weather: WeatherNow? = null,
    onOpenWeather: (() -> Unit)? = null,
    focusMemory: FocusMemory? = null,
) {
    val time by rememberClockText(clock24)
    val weatherFocus = remember { FocusRequester() }
    val buttonFocus = remember { FocusRequester() }
    // A button that leaves the screen (weather turned off) must not be refocused later.
    DisposableEffect(focusMemory) { onDispose { focusMemory?.gone(buttonFocus) } }
    fun remembered(focus: FocusRequester) = focusMemory?.let { Modifier.rememberedBy(it, focus) } ?: Modifier
    // As tall as the weather capsule even without it, so the corner does not shift when Quick
    // Controls draws its own copy (plain weather text) over home's.
    Row(modifier.heightIn(min = RoundButtonSize), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        weather?.let {
            if (onOpenWeather != null) {
                DisposableEffect(focusMemory) { onDispose { focusMemory?.gone(weatherFocus) } }
                WeatherButton(it, TimeStyle, onOpenWeather, remembered(weatherFocus))
            } else {
                WeatherLabel(it, TimeStyle)
            }
        }
        BasicText(time, style = TimeStyle)
        RoundIconButton(
            stringResource(R.string.panels_quick_controls), ControlGlyphs.Toggles, onOpen,
            remembered(buttonFocus), size = CornerButton, active = active,
        )
    }
}
