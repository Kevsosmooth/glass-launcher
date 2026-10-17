package com.kevin.glasslauncher.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.settings.ui.Glyph
import com.kevin.glasslauncher.settings.ui.PanelStyle

private val TileCorner = 16.dp
private val TileShape = RoundedCornerShape(TileCorner)
private val TileHeight = 52.dp
private val TileGlyph = 20.dp

/** Compact Quick Controls tile: glyph, then title and an optional state line; solid white when focused. */
@Composable
fun ControlTile(title: String, value: String?, glyph: Glyph, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val main = if (focused) PanelStyle.FocusedText else PanelStyle.Text
    val secondary = if (focused) PanelStyle.FocusedText else PanelStyle.SecondaryText
    Row(
        modifier
            .height(TileHeight)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .then(if (focused) Modifier.background(PanelStyle.FocusedSurface, TileShape) else Modifier.glass(TileCorner, nested = true))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(TileGlyph)) { glyph(main) }
        Column {
            BasicText(title, style = PanelStyle.on(PanelStyle.Row.copy(fontSize = 14.sp), main, focused), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value != null) {
                BasicText(value, style = PanelStyle.on(PanelStyle.Value.copy(fontSize = 12.sp), secondary, focused), maxLines = 1)
            }
        }
    }
}
