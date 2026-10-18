package com.kevin.glasslauncher.settings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.glass.glass

val RoundButtonSize = 44.dp
private const val GLYPH_SHARE = 0.45f

/** Draws a glyph in the given colour, filling the canvas size. */
typealias Glyph = DrawScope.(Color) -> Unit

/**
 * Round glass button with a drawn glyph and no caption, like tvOS Control Center; white when
 * focused, or while what it opens is showing ([active]). [label] is its accessibility name;
 * [onFocusChange] lets the caller caption the focused one.
 */
@Composable
fun RoundIconButton(
    label: String,
    glyph: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = RoundButtonSize,
    active: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val white = focused || active
    val tint = if (white) PanelStyle.FocusedText else PanelStyle.Text
    Box(
        modifier
            .size(size)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .semantics { contentDescription = label }
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .then(
                if (white) {
                    Modifier.background(PanelStyle.FocusedSurface, CircleShape)
                } else {
                    Modifier.glass(size / 2, nested = true)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * GLYPH_SHARE)) { glyph(tint) }
    }
}
