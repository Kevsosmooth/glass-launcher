package com.kevin.glasslauncher.weather

import java.time.LocalDate

/** One forecast hour; [hour] is 0-23 in the forecast place's local time, [rainChance] 0-100. */
data class HourForecast(
    val hour: Int,
    val temperature: Int,
    val rainChance: Int,
    val precipitationMm: Double,
    val condition: WeatherCondition,
)

/** One forecast day in the forecast place's calendar. */
data class DayForecast(
    val date: LocalDate,
    val condition: WeatherCondition,
    val high: Int,
    val low: Int,
    val rainChance: Int,
)

/**
 * Everything the weather card shows beyond the corner's temperature: feels-like, humidity (%) and
 * wind (mph with °F, km/h with °C) when sent, the next hours from now and the next days from today.
 */
data class WeatherDetails(
    val feelsLike: Int?,
    val humidity: Int?,
    val wind: Int?,
    val hours: List<HourForecast>,
    val days: List<DayForecast>,
)
