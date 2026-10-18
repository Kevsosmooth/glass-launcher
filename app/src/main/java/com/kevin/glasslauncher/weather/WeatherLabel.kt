package com.kevin.glasslauncher.weather

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R

private val GlyphSize = 18.dp
private val GlyphShadow = Color(0x66000000)

/**
 * "[sun] 72°" in [style]; the glyph takes the text's colour and, when the text has a shadow, the
 * same soft drop shadow. [describe] false leaves the spoken description to a parent (the button).
 */
@Composable
fun WeatherLabel(weather: WeatherNow, style: TextStyle, modifier: Modifier = Modifier, describe: Boolean = true) {
    val description = weatherDescription(weather)
    val glyph = WeatherGlyphs.forCondition(weather.condition)
    val shadowed = style.shadow != null
    Row(
        if (describe) modifier.semantics(mergeDescendants = true) { contentDescription = description } else modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(GlyphSize)) {
            if (shadowed) translate(top = 1.dp.toPx()) { glyph(GlyphShadow) }
            glyph(style.color)
        }
        BasicText(temperatureText(weather.temperature), style = style)
    }
}

/** "Partly cloudy, 72°", for screen readers. */
@Composable
fun weatherDescription(weather: WeatherNow): String =
    stringResource(R.string.weather_now_description, stringResource(conditionLabel(weather.condition)), temperatureText(weather.temperature))

@StringRes
fun conditionLabel(condition: WeatherCondition): Int = when (condition) {
    WeatherCondition.ClearDay -> R.string.weather_clear
    WeatherCondition.ClearNight -> R.string.weather_clear
    WeatherCondition.PartlyCloudy -> R.string.weather_partly_cloudy
    WeatherCondition.Cloudy -> R.string.weather_cloudy
    WeatherCondition.Fog -> R.string.weather_fog
    WeatherCondition.Rain -> R.string.weather_rain
    WeatherCondition.Snow -> R.string.weather_snow
    WeatherCondition.Thunder -> R.string.weather_thunder
}
