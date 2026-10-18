package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.data.TempUnit
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

// Every response is untrusted: a missing or wrong-typed field gives null, never an exception.

/** Open-Meteo's forecast "current" block with the card's details, or null if "current" is not there. */
fun parseForecast(json: String, unit: TempUnit, atMillis: Long, latitude: Double, longitude: Double): WeatherNow? {
    val root = parseObject(json) ?: return null
    val current = root.optJSONObject("current") ?: return null
    val temp = (current.opt("temperature_2m") as? Number)?.toDouble()?.takeIf { it.isFinite() } ?: return null
    val code = (current.opt("weather_code") as? Number)?.toInt() ?: return null
    val isDay = (current.opt("is_day") as? Number)?.toInt() != 0
    val details = parseDetails(root, current)
    return WeatherNow(temp.roundToInt(), unit, weatherCondition(code, isDay), atMillis, latitude, longitude, details)
}

/** Open-Meteo geocoding results as cities named "City, region, country"; empty if none matched. */
fun parseCities(json: String): List<City>? {
    val root = parseObject(json) ?: return null
    val results = root.optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).mapNotNull { i ->
        val place = results.optJSONObject(i) ?: return@mapNotNull null
        val name = place.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val lat = (place.opt("latitude") as? Number)?.toDouble() ?: return@mapNotNull null
        val lon = (place.opt("longitude") as? Number)?.toDouble() ?: return@mapNotNull null
        val label = listOf(name, place.optString("admin1"), place.optString("country"))
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        City(label, roundCoord(lat), roundCoord(lon))
    }
}

/** GeoJS's answer for our internet address; it sends the coordinates as strings. */
fun parseIpGeo(json: String): IpGeo? {
    val root = parseObject(json) ?: return null
    val lat = root.optString("latitude").toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: return null
    val lon = root.optString("longitude").toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: return null
    val timezone = root.optString("timezone").takeIf { it.isNotBlank() } ?: return null
    val city = root.optString("city").takeIf { it.isNotBlank() }
    return IpGeo(city, roundCoord(lat), roundCoord(lon), timezone)
}

private fun parseObject(json: String): JSONObject? = try {
    JSONObject(json)
} catch (e: JSONException) {
    null
}
