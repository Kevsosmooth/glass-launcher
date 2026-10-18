package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.TempUnit

/** What the sky looks like, as far as the status corner glyph cares. */
enum class WeatherCondition { ClearDay, ClearNight, PartlyCloudy, Cloudy, Fog, Rain, Snow, Thunder }

/**
 * The current weather at ([latitude], [longitude]), fetched at [atMillis] (wall clock).
 * [temperature] is whole degrees in [unit]; [details] is the rest of the forecast for the card.
 */
data class WeatherNow(
    val temperature: Int,
    val unit: TempUnit,
    val condition: WeatherCondition,
    val atMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val details: WeatherDetails? = null,
)

/** Where a location came from, in priority order. */
enum class LocationSource { Picked, Network, Internet }

/**
 * A resolved location with rounded coordinates, found at [atMillis] (wall clock). [name] is null
 * when the source has no place name (Android's network location).
 */
data class Located(
    val name: String?,
    val latitude: Double,
    val longitude: Double,
    val source: LocationSource,
    val atMillis: Long,
)

/** The location used for the weather and whether the internet address lookup looked wrong (VPN). */
data class LocationState(val located: Located? = null, val looksWrong: Boolean = false) {
    /** The automatically found city's name for Settings; null for a picked city or no name. */
    val autoName: String?
        get() = located?.takeIf { it.source != LocationSource.Picked }?.name
}

/** The internet address lookup's answer; coordinates are already rounded. */
data class IpGeo(val city: String?, val latitude: Double, val longitude: Double, val timezone: String)

/** Coordinates rounded to 2 decimals (about 1 km), so nothing sent anywhere is more precise. */
fun roundCoord(value: Double): Double = Math.round(value * 100) / 100.0

/** "72°": whole degrees, no unit letter, like tvOS. */
fun temperatureText(degrees: Int): String = "$degrees°"
