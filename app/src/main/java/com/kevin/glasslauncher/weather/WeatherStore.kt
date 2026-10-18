package com.kevin.glasslauncher.weather

import android.content.Context
import android.content.SharedPreferences
import com.kevin.glasslauncher.data.TempUnit

/** Where the last location and weather survive a restart. */
interface WeatherStore {
    fun loadLocation(): Located?
    fun saveLocation(located: Located)
    fun loadWeather(): WeatherNow?
    fun saveWeather(fetched: FetchedWeather)
}

/**
 * [WeatherStore] in its own small SharedPreferences file: a cache, not a setting, so it stays out
 * of the settings DataStore (and its backups) and never wakes the home screen's prefs flow. The
 * weather is kept as the service's reply (a few KB) and parsed again on load.
 */
class SharedPrefsWeatherStore(context: Context) : WeatherStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    override fun loadLocation(): Located? {
        val source = LocationSource.entries.firstOrNull { it.name == prefs.getString(LOC_SOURCE, null) } ?: return null
        val lat = prefs.getString(LOC_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(LOC_LON, null)?.toDoubleOrNull() ?: return null
        return Located(prefs.getString(LOC_NAME, null), lat, lon, source, prefs.getLong(LOC_AT, 0L))
    }

    override fun saveLocation(located: Located) {
        prefs.edit()
            .putString(LOC_NAME, located.name)
            .putString(LOC_LAT, located.latitude.toString())
            .putString(LOC_LON, located.longitude.toString())
            .putString(LOC_SOURCE, located.source.name)
            .putLong(LOC_AT, located.atMillis)
            .apply()
    }

    override fun loadWeather(): WeatherNow? {
        val json = prefs.getString(WX_JSON, null) ?: return null
        val unit = TempUnit.entries.firstOrNull { it.name == prefs.getString(WX_UNIT, null) } ?: return null
        val lat = prefs.getString(WX_LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(WX_LON, null)?.toDoubleOrNull() ?: return null
        return parseForecast(json, unit, prefs.getLong(WX_AT, 0L), lat, lon)
    }

    override fun saveWeather(fetched: FetchedWeather) {
        val weather = fetched.weather
        prefs.edit()
            .putString(WX_JSON, fetched.json)
            .putString(WX_UNIT, weather.unit.name)
            .putLong(WX_AT, weather.atMillis)
            .putString(WX_LAT, weather.latitude.toString())
            .putString(WX_LON, weather.longitude.toString())
            .apply()
    }

    private companion object {
        const val LOC_NAME = "loc_name"
        const val LOC_LAT = "loc_lat"
        const val LOC_LON = "loc_lon"
        const val LOC_SOURCE = "loc_source"
        const val LOC_AT = "loc_at"
        const val WX_JSON = "wx_json"
        const val WX_UNIT = "wx_unit"
        const val WX_AT = "wx_at"
        const val WX_LAT = "wx_lat"
        const val WX_LON = "wx_lon"
    }
}
