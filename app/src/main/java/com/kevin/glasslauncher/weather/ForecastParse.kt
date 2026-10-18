package com.kevin.glasslauncher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

const val FORECAST_HOURS = 12
const val FORECAST_DAYS = 5

/**
 * The card's part of an Open-Meteo forecast: extra current values, the hours from the current
 * hour on and the days from today. Null when it has neither hours nor days. Like the rest of the
 * parsing, a missing or wrong-typed value is skipped, never thrown.
 */
internal fun parseDetails(root: JSONObject, current: JSONObject): WeatherDetails? {
    val now = localTime(current.optString("time"))?.truncatedTo(ChronoUnit.HOURS)
    val hours = root.optJSONObject("hourly")?.let { parseHours(it, now) }.orEmpty()
    val days = root.optJSONObject("daily")?.let(::parseDays).orEmpty()
    if (hours.isEmpty() && days.isEmpty()) return null
    return WeatherDetails(
        feelsLike = current.number("apparent_temperature")?.roundToInt(),
        humidity = current.number("relative_humidity_2m")?.roundToInt()?.coerceIn(0, 100),
        wind = current.number("wind_speed_10m")?.roundToInt()?.coerceAtLeast(0),
        hours = hours,
        days = days,
    )
}

// Hours before [from] are dropped, in case the service starts the list earlier than asked.
private fun parseHours(hourly: JSONObject, from: LocalDateTime?): List<HourForecast> {
    val times = hourly.optJSONArray("time") ?: return emptyList()
    val temps = hourly.optJSONArray("temperature_2m") ?: return emptyList()
    val codes = hourly.optJSONArray("weather_code") ?: return emptyList()
    val chances = hourly.optJSONArray("precipitation_probability")
    val amounts = hourly.optJSONArray("precipitation")
    val isDay = hourly.optJSONArray("is_day")
    return (0 until times.length()).mapNotNull { i ->
        val time = localTime(times.optString(i)) ?: return@mapNotNull null
        if (from != null && time.isBefore(from)) return@mapNotNull null
        val temp = temps.numberAt(i) ?: return@mapNotNull null
        val code = codes.numberAt(i)?.toInt() ?: return@mapNotNull null
        HourForecast(
            hour = time.hour,
            temperature = temp.roundToInt(),
            rainChance = chances?.numberAt(i)?.roundToInt()?.coerceIn(0, 100) ?: 0,
            precipitationMm = amounts?.numberAt(i)?.coerceAtLeast(0.0) ?: 0.0,
            condition = weatherCondition(code, isDay?.numberAt(i)?.toInt() != 0),
        )
    }.take(FORECAST_HOURS)
}

private fun parseDays(daily: JSONObject): List<DayForecast> {
    val dates = daily.optJSONArray("time") ?: return emptyList()
    val codes = daily.optJSONArray("weather_code") ?: return emptyList()
    val highs = daily.optJSONArray("temperature_2m_max") ?: return emptyList()
    val lows = daily.optJSONArray("temperature_2m_min") ?: return emptyList()
    val chances = daily.optJSONArray("precipitation_probability_max")
    return (0 until dates.length()).mapNotNull { i ->
        val date = localDate(dates.optString(i)) ?: return@mapNotNull null
        val code = codes.numberAt(i)?.toInt() ?: return@mapNotNull null
        val high = highs.numberAt(i)?.roundToInt() ?: return@mapNotNull null
        val low = lows.numberAt(i)?.roundToInt() ?: return@mapNotNull null
        DayForecast(date, weatherCondition(code, isDay = true), high, low, chances?.numberAt(i)?.roundToInt()?.coerceIn(0, 100) ?: 0)
    }.take(FORECAST_DAYS)
}

private fun JSONObject.number(key: String): Double? = (opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() }

private fun JSONArray.numberAt(i: Int): Double? = (opt(i) as? Number)?.toDouble()?.takeIf { it.isFinite() }

private fun localTime(text: String): LocalDateTime? = try {
    LocalDateTime.parse(text)
} catch (e: DateTimeParseException) {
    null
}

private fun localDate(text: String): LocalDate? = try {
    LocalDate.parse(text)
} catch (e: DateTimeParseException) {
    null
}
