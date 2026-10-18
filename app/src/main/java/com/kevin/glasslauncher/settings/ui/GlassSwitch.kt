package com.kevin.glasslauncher.settings.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.motion.Motion

private val TrackWidth = 40.dp
private val TrackHeight = 24.dp
private val KnobInset = 2.5.dp

// How much wider the knob gets halfway through the slide, as a share of its diameter.
private const val KNOB_STRETCH = 0.45f

// How see-through the knob gets halfway (Liquid Glass turns the knob into a clear lens mid-move).
private const val KNOB_CLEAR = 0.55f

// On an unfocused row the track is glass and "on" only tints it, so the wallpaper shows through.
private const val GLASS_ON_TINT = 0.6f
private val KnobShade = Color(0x33000000)

/**
 * On/off switch in the tvOS 26 style: the knob springs across, stretching into a pill and turning
 * into clear glass with a bright rim while it moves, and the track colour blends on the way.
 * Unfocused, the track is a glass capsule tinted green when on; on the white focused row it is
 * solid so it stays readable.
 * The spring is read only in the draw phase, so a toggle redraws without recomposing the row.
 */
@Composable
fun GlassSwitch(checked: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (checked) 1f else 0f, Motion.switchSpring, label = "switch")
    val off = if (focused) PanelStyle.SwitchOffFocused else Color.Transparent
    val on = if (focused) PanelStyle.SwitchOn else PanelStyle.SwitchOn.copy(alpha = GLASS_ON_TINT)
    val track = if (focused) Modifier else Modifier.glass(TrackHeight / 2, nested = true)
    Canvas(modifier.size(TrackWidth, TrackHeight).then(track)) {
        val p = progress.coerceIn(0f, 1f)
        // 0 at either end, 1 halfway: drives the stretch and the clear-glass look.
        val travel = 4f * p * (1f - p)
        val radius = size.height / 2
        drawRoundRect(lerp(off, on, p), cornerRadius = CornerRadius(radius))

        val inset = KnobInset.toPx()
        val knob = size.height - 2 * inset
        val width = knob * (1f + KNOB_STRETCH * travel)
        val left = inset + p * (size.width - 2 * inset - width)
        val knobSize = Size(width, knob)
        val topLeft = Offset(left, inset)
        val corner = CornerRadius(knob / 2)
        drawRoundRect(KnobShade, topLeft + Offset(0f, 1.dp.toPx()), knobSize, corner)
        drawRoundRect(Color.White.copy(alpha = 1f - KNOB_CLEAR * travel), topLeft, knobSize, corner)
        if (travel > 0f) {
            drawRoundRect(Color.White.copy(alpha = travel), topLeft, knobSize, corner, style = Stroke(1.5.dp.toPx()))
        }
    }
}
