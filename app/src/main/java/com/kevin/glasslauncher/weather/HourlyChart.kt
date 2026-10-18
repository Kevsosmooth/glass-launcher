package com.kevin.glasslauncher.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LabelSlot = 16.dp
private val BarArea = 56.dp
private val BarWidth = 14.dp
private val ColumnGlyph = 20.dp

/**
 * The next hours, one equal-width column each so the chart always fits the card: rain chance
 * label (10% and up), a bar as tall as the chance, the hour's glyph, temperature and hour.
 */
@Composable
fun HourlyChart(columns: List<ChartColumn>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        columns.forEach { HourColumn(it, Modifier.weight(1f)) }
    }
}

@Composable
private fun HourColumn(column: ChartColumn, modifier: Modifier) {
    val glyph = WeatherGlyphs.forCondition(column.condition)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.height(LabelSlot), contentAlignment = Alignment.BottomCenter) {
            column.chanceLabel?.let { BasicText(it, style = WeatherCardStyle.SmallRain, maxLines = 1, softWrap = false) }
        }
        Canvas(Modifier.width(BarWidth).height(BarArea)) {
            val radius = CornerRadius(size.width / 2)
            drawRoundRect(WeatherCardStyle.BarTrack, cornerRadius = radius)
            val filled = size.height * column.chance.coerceIn(0f, 1f)
            if (filled > 0f) {
                drawRoundRect(
                    WeatherCardStyle.Rain,
                    topLeft = Offset(0f, size.height - filled),
                    size = Size(size.width, filled),
                    cornerRadius = radius,
                )
            }
        }
        Canvas(Modifier.size(ColumnGlyph)) { glyph(Color.White) }
        BasicText(column.temperature, style = WeatherCardStyle.ColumnTemperature, maxLines = 1, softWrap = false)
        BasicText(column.hourLabel, style = WeatherCardStyle.Small, maxLines = 1, softWrap = false)
    }
}
