package com.kevin.glasslauncher.weather

/**
 * The condition for a WMO weather interpretation code (what Open-Meteo's weather_code holds).
 * Mainly clear counts as clear; unknown codes show as cloudy.
 */
fun weatherCondition(code: Int, isDay: Boolean): WeatherCondition = when (code) {
    0, 1 -> if (isDay) WeatherCondition.ClearDay else WeatherCondition.ClearNight
    2 -> WeatherCondition.PartlyCloudy
    3 -> WeatherCondition.Cloudy
    45, 48 -> WeatherCondition.Fog
    in 51..67, in 80..82 -> WeatherCondition.Rain
    in 71..77, 85, 86 -> WeatherCondition.Snow
    in 95..99 -> WeatherCondition.Thunder
    else -> WeatherCondition.Cloudy
}
