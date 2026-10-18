package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.data.TempUnit
import java.net.URLEncoder
import java.util.Locale

private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
private const val IP_GEO_URL = "https://get.geojs.io/v1/ip/geo.json"
private const val MAX_RESULTS = 8
private const val MAX_QUERY_LENGTH = 100
private const val CURRENT = "temperature_2m,weather_code,is_day,apparent_temperature,relative_humidity_2m,wind_speed_10m"
private const val HOURLY = "temperature_2m,precipitation_probability,precipitation,weather_code,is_day"
private const val DAILY = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"

/**
 * Open-Meteo forecast URL for rounded coordinates (always a dot decimal, whatever the locale): now,
 * the next [FORECAST_HOURS] hours and [FORECAST_DAYS] days in one call, times in the place's own zone.
 */
fun forecastUrl(latitude: Double, longitude: Double, unit: TempUnit): String {
    val fahrenheit = unit == TempUnit.Fahrenheit
    val lat = String.format(Locale.ROOT, "%.2f", roundCoord(latitude))
    val lon = String.format(Locale.ROOT, "%.2f", roundCoord(longitude))
    return "$FORECAST_URL?latitude=$lat&longitude=$lon&current=$CURRENT&hourly=$HOURLY&daily=$DAILY" +
        "&forecast_hours=$FORECAST_HOURS&forecast_days=$FORECAST_DAYS&timezone=auto" +
        "&temperature_unit=${if (fahrenheit) "fahrenheit" else "celsius"}&wind_speed_unit=${if (fahrenheit) "mph" else "kmh"}"
}

/** A parsed forecast and the reply it came from; the cache keeps [json] and reads it back with the same parser. */
data class FetchedWeather(val weather: WeatherNow, val json: String)

/** Open-Meteo city search URL for what the user typed. */
fun citySearchUrl(query: String): String =
    "$GEOCODING_URL?name=${URLEncoder.encode(query.trim().take(MAX_QUERY_LENGTH), "UTF-8")}&count=$MAX_RESULTS"

/** The three weather services; every call returns null on any failure. */
class WeatherApi(private val http: HttpGet) {
    suspend fun forecast(latitude: Double, longitude: Double, unit: TempUnit, nowMillis: Long): FetchedWeather? {
        val json = http.get(forecastUrl(latitude, longitude, unit)) ?: return null
        val weather = parseForecast(json, unit, nowMillis, roundCoord(latitude), roundCoord(longitude)) ?: return null
        return FetchedWeather(weather, json)
    }

    /** Matching cities (possibly none), or null when the search failed. */
    suspend fun searchCities(query: String): List<City>? = http.get(citySearchUrl(query))?.let(::parseCities)

    suspend fun ipGeo(): IpGeo? = http.get(IP_GEO_URL)?.let(::parseIpGeo)
}
