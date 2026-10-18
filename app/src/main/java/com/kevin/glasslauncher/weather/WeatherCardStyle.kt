package com.kevin.glasslauncher.weather

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.settings.ui.PanelStyle

/** Colours and text styles of the weather card, on top of the panels' [PanelStyle]. */
internal object WeatherCardStyle {
    /** Rain chance: a translucent blue-white, so the bars read as rain without a loud colour. */
    val Rain = Color(0xB3CFE2FF)
    val BarTrack = Color(0x1FFFFFFF)

    val Place = PanelStyle.Title.copy(color = PanelStyle.Text)
    val BigTemperature = PanelStyle.Title.copy(color = PanelStyle.Text, fontSize = 52.sp, lineHeight = 56.sp)
    val Line = PanelStyle.Row.copy(color = PanelStyle.Text)
    val Detail = PanelStyle.Value.copy(color = PanelStyle.SecondaryText)
    val Label = PanelStyle.Header.copy(color = PanelStyle.SecondaryText)
    val Small = PanelStyle.Value.copy(fontSize = 12.sp, color = PanelStyle.SecondaryText, textAlign = TextAlign.Center)
    val SmallRain = Small.copy(color = Rain)
    val ColumnTemperature = PanelStyle.Row.copy(fontSize = 14.sp, color = PanelStyle.Text, textAlign = TextAlign.Center)
}
