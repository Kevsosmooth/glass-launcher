package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.data.TempUnit
import com.kevin.glasslauncher.settings.UiText
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// The card's words as UiText, so they can be checked without drawing anything.

/** The one-line rain (or snow) summary for the next 12 hours. */
fun rainSummary(outlook: RainOutlook, clock24: Boolean): UiText = when (outlook) {
    RainOutlook.Dry -> UiText.Res(R.string.weather_dry)
    is RainOutlook.Now -> if (outlook.stop == null) {
        UiText.Res(if (outlook.snow) R.string.weather_snow_all else R.string.weather_rain_all)
    } else {
        UiText.Res(if (outlook.snow) R.string.weather_snow_until else R.string.weather_rain_until, listOf(hourLabel(outlook.stop, clock24)))
    }
    is RainOutlook.Later -> if (outlook.stop == null) {
        UiText.Res(if (outlook.snow) R.string.weather_snow_starts else R.string.weather_rain_starts, listOf(hourLabel(outlook.start, clock24)))
    } else {
        UiText.Res(
            if (outlook.snow) R.string.weather_snow_starts_stops else R.string.weather_rain_starts_stops,
            listOf(hourLabel(outlook.start, clock24), hourLabel(outlook.stop, clock24)),
        )
    }
}

/** "Today" for the first day, then short day names ("Mon"). */
fun dayLabel(index: Int, date: LocalDate, locale: Locale = Locale.getDefault()): UiText =
    if (index == 0) UiText.Res(R.string.weather_today) else UiText.Raw(date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale))

/** The header's place: the city part of a picked or found name, else "Your location". */
fun placeLabel(name: String?): UiText =
    name?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }?.let { UiText.Raw(it) } ?: UiText.Res(R.string.weather_your_location)

/** "Feels like 70°", "H 75°", "L 61°": the parts that are known. */
fun feelsLine(details: WeatherDetails): List<UiText> = listOfNotNull(
    details.feelsLike?.let { UiText.Res(R.string.weather_feels_like, listOf(temperatureText(it))) },
    details.days.firstOrNull()?.let { UiText.Res(R.string.weather_high, listOf(temperatureText(it.high))) },
    details.days.firstOrNull()?.let { UiText.Res(R.string.weather_low, listOf(temperatureText(it.low))) },
)

/** "Humidity 40%", "Wind 8 mph" (km/h with °C): the parts that are known. */
fun airLine(details: WeatherDetails, unit: TempUnit): List<UiText> = listOfNotNull(
    details.humidity?.let { UiText.Res(R.string.weather_humidity, listOf(it)) },
    details.wind?.let {
        UiText.Res(if (unit == TempUnit.Fahrenheit) R.string.weather_wind_mph else R.string.weather_wind_kmh, listOf(it))
    },
)
