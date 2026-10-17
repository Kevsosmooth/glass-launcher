package com.kevin.glasslauncher.settings.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.settings.ui.PanelStyle.FocusedText
import com.kevin.glasslauncher.settings.ui.PanelStyle.SecondaryText
import com.kevin.glasslauncher.settings.ui.PanelStyle.Text
import com.kevin.glasslauncher.settings.ui.PanelStyle.on

private val BarHeight = 6.dp
private val TrackIdle = Color(0x40FFFFFF)
private val TrackFocused = Color(0x26000000)

/**
 * Title, value and a filled bar for [fraction] (0-1), e.g. storage used. Focusable only, so long
 * pages still scroll with the D-pad.
 */
@Composable
fun MeterRow(title: String, value: String, fraction: Float, modifier: Modifier = Modifier) {
    PanelRow(onClick = null, modifier) { focused ->
        MeterContent(title, value, fraction, focused)
    }
}

/**
 * A slider in the tvOS style: Left and Right change it by one step ([onStep] gets -1 or +1) while
 * focus stays on the row. Disabled ([onStep] null) it shows the value only; a null [fraction]
 * (level unknown) hides the bar.
 */
@Composable
fun SliderRow(title: String, value: String, fraction: Float?, onStep: ((Int) -> Unit)?, modifier: Modifier = Modifier) {
    val keys = if (onStep == null) {
        Modifier
    } else {
        Modifier.onPreviewKeyEvent { event ->
            val step = when (event.key) {
                Key.DirectionLeft -> -1
                Key.DirectionRight -> 1
                else -> 0
            }
            if (step != 0 && event.type == KeyEventType.KeyDown) onStep(step)
            step != 0
        }
    }
    val semantics = Modifier.semantics {
        progressBarRangeInfo = fraction?.let { ProgressBarRangeInfo(it.coerceIn(0f, 1f), 0f..1f) } ?: ProgressBarRangeInfo.Indeterminate
        if (onStep != null) {
            setProgress { target ->
                onStep(if (target > (fraction ?: 0.5f)) 1 else -1)
                true
            }
        }
    }
    PanelRow(onClick = null, modifier.then(keys).then(semantics)) { focused ->
        MeterContent(title, value, fraction, focused)
    }
}

@Composable
private fun MeterContent(title: String, value: String, fraction: Float?, focused: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            BasicText(title, Modifier.weight(1f), style = on(PanelStyle.Row, if (focused) FocusedText else Text, focused), maxLines = 1)
            BasicText(value, style = on(PanelStyle.Value, if (focused) FocusedText else SecondaryText, focused), maxLines = 1)
        }
        if (fraction == null) return@Column
        Canvas(Modifier.fillMaxWidth().padding(top = 6.dp).height(BarHeight)) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(if (focused) TrackFocused else TrackIdle, cornerRadius = radius)
            val filled = size.width * fraction.coerceIn(0f, 1f)
            if (filled > 0f) {
                drawRoundRect(if (focused) FocusedText else Color.White, size = Size(filled, size.height), cornerRadius = radius)
            }
        }
    }
}

/** Four rising Wi-Fi strength bars, [bars] of them lit (0-4). */
@Composable
fun SignalBars(bars: Int, focused: Boolean, modifier: Modifier = Modifier) {
    val lit = if (focused) FocusedText else Color.White
    val dim = if (focused) TrackFocused else TrackIdle
    Canvas(modifier.size(18.dp, 14.dp)) {
        val gap = 2.dp.toPx()
        val width = (size.width - 3 * gap) / 4
        for (i in 0 until 4) {
            val height = size.height * (i + 1) / 4
            drawRoundRect(
                if (i < bars) lit else dim,
                topLeft = Offset(i * (width + gap), size.height - height),
                size = Size(width, height),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
    }
}
