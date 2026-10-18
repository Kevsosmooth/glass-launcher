package com.kevin.glasslauncher.weather

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.resolve

private val DayRowHeight = 24.dp
private val DayName = 56.dp
private val DayGlyph = 18.dp
private val DayChance = 48.dp
private val DayTemperature = 40.dp

/** The next days, one row each: day, glyph, rain chance (10% and up), low, high. */
@Composable
fun DailyForecast(days: List<DayForecast>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(stringResource(R.string.weather_next_days), style = WeatherCardStyle.Label)
        days.forEachIndexed { i, day -> DayRow(dayLabel(i, day.date).resolve(), day) }
    }
}

@Composable
private fun DayRow(label: String, day: DayForecast) {
    val glyph = WeatherGlyphs.forCondition(day.condition)
    Row(Modifier.fillMaxWidth().height(DayRowHeight), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, Modifier.width(DayName), style = WeatherCardStyle.Line, maxLines = 1)
        Canvas(Modifier.size(DayGlyph)) { glyph(Color.White) }
        Box(Modifier.width(DayChance).padding(start = 8.dp)) {
            dayChanceLabel(day)?.let { BasicText(it, style = WeatherCardStyle.SmallRain, maxLines = 1) }
        }
        Spacer(Modifier.weight(1f))
        BasicText(
            temperatureText(day.low), Modifier.width(DayTemperature),
            style = WeatherCardStyle.Detail.copy(textAlign = TextAlign.End), maxLines = 1,
        )
        BasicText(
            temperatureText(day.high), Modifier.width(DayTemperature),
            style = WeatherCardStyle.Line.copy(textAlign = TextAlign.End), maxLines = 1,
        )
    }
}
