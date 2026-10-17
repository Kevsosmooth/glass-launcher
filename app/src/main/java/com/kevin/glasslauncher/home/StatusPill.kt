package com.kevin.glasslauncher.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.glass.glass

private val PillCorner = 20.dp
private val PillShape = RoundedCornerShape(PillCorner)
private val FocusedText = Color(0xFF111318)
private val PillText = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(Color(0x66000000), Offset(0f, 1f), 3f),
)
private const val GEAR_TEETH = 8

/** Glass pill with the clock and a settings glyph; focused it turns solid white (Apple pattern). */
@Composable
fun StatusPill(clock24: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val time by rememberClockText(clock24)
    var focused by remember { mutableStateOf(false) }
    val content = if (focused) FocusedText else Color.White
    Row(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = null, indication = null, onClick = onOpen)
            .then(if (focused) Modifier.background(Color.White, PillShape) else Modifier.glass(PillCorner))
            .padding(horizontal = 18.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(time, style = if (focused) PillText.copy(color = content, shadow = null) else PillText.copy(color = content))
        Gear(content, Modifier.size(18.dp))
    }
}

@Composable
private fun Gear(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        val tooth = Size(r * 0.3f, r * 0.42f)
        repeat(GEAR_TEETH) { i ->
            rotate(i * 360f / GEAR_TEETH) {
                drawRect(color, Offset(center.x - tooth.width / 2, center.y - r), tooth)
            }
        }
        drawCircle(color, radius = r * 0.52f, style = Stroke(width = r * 0.36f))
    }
}
