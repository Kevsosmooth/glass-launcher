package com.kevin.glasslauncher.weather

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.motion.Motion
import com.kevin.glasslauncher.settings.resolve
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.focusOnShow

const val WEATHER_CARD_TAG = "weather_card"

private val CardWidth = 640.dp
private val CardPadding = 24.dp
private val DaysWidth = 280.dp
private val BigGlyph = 40.dp
private const val SEPARATOR = "   "
private const val GROW_FROM = 0.96f

/** What the card shows: the weather (null while loading), the place's name if known, the clock setting. */
data class WeatherCardState(val weather: WeatherNow?, val placeName: String?, val clock24: Boolean)

/**
 * The weather card, centred over home, like tvOS Weather: now (place, temperature, feels like,
 * high and low, humidity, wind) beside the next days, the rain summary, then the hourly chart.
 * The card is one focusable item: Select closes it ([onClose]), Back is LauncherRoot's, and the
 * D-pad cannot move focus out to home.
 */
@Composable
fun WeatherCard(state: WeatherCardState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, Motion.panelSpring) }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .testTag(WEATHER_CARD_TAG)
                .width(CardWidth)
                .graphicsLayer {
                    val t = shown.value.coerceIn(0f, 1f)
                    alpha = t
                    scaleX = GROW_FROM + (1f - GROW_FROM) * t
                    scaleY = scaleX
                }
                .glass(PanelStyle.PanelCorner)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .focusOnShow()
                    .clickable(interactionSource = null, indication = null, onClick = onClose)
                    .padding(CardPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { CardContent(state) }
        }
    }
}

@Composable
private fun ColumnScope.CardContent(state: WeatherCardState) {
    val weather = state.weather
    val details = weather?.details
    if (weather == null || details == null) {
        BasicText(stringResource(if (weather == null) R.string.weather_loading else R.string.weather_unavailable), style = WeatherCardStyle.Line)
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        CurrentWeather(placeLabel(state.placeName).resolve(), weather, details, Modifier.weight(1f))
        if (details.days.isNotEmpty()) DailyForecast(details.days, Modifier.width(DaysWidth))
    }
    BasicText(rainSummary(rainOutlook(details.hours), state.clock24).resolve(), style = WeatherCardStyle.Line)
    if (details.hours.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BasicText(stringResource(R.string.weather_next_hours), style = WeatherCardStyle.Label)
            HourlyChart(chartColumns(details.hours, state.clock24))
        }
    }
}

@Composable
private fun CurrentWeather(place: String, weather: WeatherNow, details: WeatherDetails, modifier: Modifier) {
    val glyph = WeatherGlyphs.forCondition(weather.condition)
    val feels = feelsLine(details).map { it.resolve() }.joinToString(SEPARATOR)
    val air = airLine(details, weather.unit).map { it.resolve() }.joinToString(SEPARATOR)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BasicText(place, style = WeatherCardStyle.Place, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText(temperatureText(weather.temperature), style = WeatherCardStyle.BigTemperature)
            Canvas(Modifier.size(BigGlyph)) { glyph(Color.White) }
        }
        BasicText(stringResource(conditionLabel(weather.condition)), style = WeatherCardStyle.Line)
        if (feels.isNotEmpty()) BasicText(feels, Modifier.padding(top = 4.dp), style = WeatherCardStyle.Detail, maxLines = 1)
        if (air.isNotEmpty()) BasicText(air, style = WeatherCardStyle.Detail, maxLines = 1)
    }
}
