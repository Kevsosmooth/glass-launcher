package com.kevin.glasslauncher.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.settings.ui.Glyph
import com.kevin.glasslauncher.settings.ui.PanelStyle

/** Height of a short tile; the big square tile spans two of them plus the gap. */
val ControlTileHeight = 56.dp

private val CapsuleCorner = ControlTileHeight / 2
private val SquareCorner = 26.dp
private val TileGlyph = 20.dp
private val SquareGlyph = 30.dp

/** Glass until focused, then solid white (tvOS); reports focus to [onFocus]. */
private fun Modifier.tileSurface(corner: Dp, focused: Boolean, onFocus: (Boolean) -> Unit, onClick: () -> Unit): Modifier =
    onFocusChanged { onFocus(it.isFocused) }
        .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
        .then(if (focused) Modifier.background(PanelStyle.FocusedSurface, RoundedCornerShape(corner)) else Modifier.glass(corner))

/** Short capsule tile: glyph, then a title and an optional state line (network name, time left). */
@Composable
fun ControlTile(title: String, value: String?, glyph: Glyph, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val main = if (focused) PanelStyle.FocusedText else PanelStyle.Text
    val secondary = if (focused) PanelStyle.FocusedText else PanelStyle.SecondaryText
    Row(
        modifier
            .height(ControlTileHeight)
            .tileSurface(CapsuleCorner, focused, { focused = it }, onClick)
            .padding(start = 16.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(TileGlyph)) { glyph(main) }
        Column {
            BasicText(title, style = PanelStyle.on(PanelStyle.Row.copy(fontSize = 14.sp), main, focused), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value != null) {
                BasicText(
                    value,
                    style = PanelStyle.on(PanelStyle.Value.copy(fontSize = 12.sp), secondary, focused),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The big square tile (Sleep): a large glyph top left and the title bottom left. */
@Composable
fun SquareTile(title: String, glyph: Glyph, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val main = if (focused) PanelStyle.FocusedText else PanelStyle.Text
    Column(modifier.tileSurface(SquareCorner, focused, { focused = it }, onClick).padding(18.dp)) {
        Canvas(Modifier.size(SquareGlyph)) { glyph(main) }
        Spacer(Modifier.weight(1f))
        BasicText(title, style = PanelStyle.on(PanelStyle.Row, main, focused), maxLines = 1)
    }
}
