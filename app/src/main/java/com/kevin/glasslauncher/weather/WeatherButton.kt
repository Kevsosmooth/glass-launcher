package com.kevin.glasslauncher.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.RoundButtonSize

private val ButtonCorner = RoundButtonSize / 2

/**
 * The status corner's weather as a glass capsule (44 dp tall) that opens the weather card; solid
 * white with dark text when focused, like the round Quick Controls button.
 */
@Composable
fun WeatherButton(weather: WeatherNow, style: TextStyle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val description = weatherDescription(weather)
    Box(
        modifier
            .height(RoundButtonSize)
            .onFocusChanged { focused = it.isFocused }
            .semantics { contentDescription = description }
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .then(
                if (focused) {
                    Modifier.background(PanelStyle.FocusedSurface, RoundedCornerShape(ButtonCorner))
                } else {
                    Modifier.glass(ButtonCorner, nested = true)
                },
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        WeatherLabel(weather, PanelStyle.on(style, if (focused) PanelStyle.FocusedText else style.color, focused), describe = false)
    }
}
