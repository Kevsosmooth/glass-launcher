package com.kevin.glasslauncher.settings.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colours, shapes and text styles shared by Settings and Quick Controls. */
object PanelStyle {
    val FocusedSurface = Color.White
    val FocusedText = Color(0xFF111111)
    val Text = Color.White
    val SecondaryText = Color(0xCCFFFFFF)
    val SwitchOn = Color(0xFF34C759)
    val SwitchOff = Color(0x66FFFFFF)
    val SwitchOffFocused = Color(0x40000000)
    val CodeSurface = Color(0xFF1C1F26)

    val PanelCorner = 24.dp
    val RowCorner = 12.dp
    val RowShape = RoundedCornerShape(RowCorner)
    val CodeShape = RoundedCornerShape(8.dp)

    private val GlassShadow = Shadow(Color(0x80000000), Offset(0f, 1f), 3f)

    val Title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, shadow = GlassShadow)
    val Header = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, shadow = GlassShadow)
    val Row = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, shadow = GlassShadow)
    val Value = TextStyle(fontSize = 13.sp, shadow = GlassShadow)
    val Body = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, shadow = GlassShadow)
    val Code = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)

    /** [style] in [color], without the glass text shadow when drawn on a focused white surface. */
    fun on(style: TextStyle, color: Color, focused: Boolean): TextStyle =
        if (focused) style.copy(color = color, shadow = null) else style.copy(color = color)
}
