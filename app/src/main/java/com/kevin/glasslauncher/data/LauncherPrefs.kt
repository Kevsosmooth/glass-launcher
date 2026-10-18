package com.kevin.glasslauncher.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")

private object Keys {
    val favorites = stringPreferencesKey("favorites")
    val hidden = stringSetPreferencesKey("hidden")
    val look = stringPreferencesKey("look")
    val clock24 = booleanPreferencesKey("clock24")
    val hud = booleanPreferencesKey("hud")
    val dockSize = intPreferencesKey("dock_size")
    val wallpaperId = stringPreferencesKey("wallpaper")
    val rotation = stringPreferencesKey("rotation")
    val rotationSince = longPreferencesKey("rotation_since")
    val topShelf = booleanPreferencesKey("top_shelf")
    val appBackdrop = booleanPreferencesKey("app_backdrop")
    val recents = stringPreferencesKey("recents")
    val sleepAt = longPreferencesKey("sleep_at")
    val appOrder = stringPreferencesKey("app_order")
    val homeSetupSeen = booleanPreferencesKey("home_setup_seen")
    val weatherOn = booleanPreferencesKey("weather_on")
    val tempUnit = stringPreferencesKey("temp_unit")
    val cityName = stringPreferencesKey("weather_city_name")
    val cityLat = doublePreferencesKey("weather_city_lat")
    val cityLon = doublePreferencesKey("weather_city_lon")
    val weatherDeviceLocation = booleanPreferencesKey("weather_device_location")
}

/** DataStore-backed [Prefs]. Every setter is a single atomic edit. */
class LauncherPrefs(private val context: Context) {
    val prefs: Flow<Prefs> = context.dataStore.data.map { p ->
        val d = Prefs()
        Prefs(
            favorites = p[Keys.favorites]?.let(::decodeList),
            hidden = p[Keys.hidden] ?: d.hidden,
            look = Look.entries.firstOrNull { it.name == p[Keys.look] } ?: d.look,
            clock24 = p[Keys.clock24] ?: d.clock24,
            hud = p[Keys.hud] ?: d.hud,
            dockSize = (p[Keys.dockSize] ?: d.dockSize).coerceIn(MIN_DOCK_SIZE, MAX_DOCK_SIZE),
            wallpaperId = p[Keys.wallpaperId] ?: d.wallpaperId,
            rotation = Rotation.entries.firstOrNull { it.name == p[Keys.rotation] } ?: d.rotation,
            rotationSinceMillis = p[Keys.rotationSince] ?: d.rotationSinceMillis,
            topShelf = p[Keys.topShelf] ?: d.topShelf,
            appBackdrop = p[Keys.appBackdrop] ?: d.appBackdrop,
            recents = p[Keys.recents]?.let(::decodeList) ?: d.recents,
            sleepAtMillis = p[Keys.sleepAt] ?: d.sleepAtMillis,
            appOrder = AppOrder.entries.firstOrNull { it.name == p[Keys.appOrder] } ?: d.appOrder,
            homeSetupSeen = p[Keys.homeSetupSeen] ?: d.homeSetupSeen,
            weatherOn = p[Keys.weatherOn] ?: d.weatherOn,
            tempUnit = TempUnit.entries.firstOrNull { it.name == p[Keys.tempUnit] } ?: d.tempUnit,
            weatherCity = readCity(p),
            weatherUseDeviceLocation = p[Keys.weatherDeviceLocation] ?: d.weatherUseDeviceLocation,
        )
    }.distinctUntilChanged()

    suspend fun setFavorites(keys: List<String>) = edit { it[Keys.favorites] = encodeList(keys) }
    suspend fun resetFavorites() = edit { it.remove(Keys.favorites) }
    suspend fun setHidden(keys: Set<String>) = edit { it[Keys.hidden] = keys }
    suspend fun setLook(look: Look) = edit { it[Keys.look] = look.name }
    suspend fun setClock24(on: Boolean) = edit { it[Keys.clock24] = on }
    suspend fun setHud(on: Boolean) = edit { it[Keys.hud] = on }
    suspend fun setDockSize(size: Int) = edit { it[Keys.dockSize] = size.coerceIn(MIN_DOCK_SIZE, MAX_DOCK_SIZE) }
    suspend fun setWallpaper(id: String) = edit {
        it[Keys.wallpaperId] = id
        it[Keys.rotationSince] = System.currentTimeMillis()
    }
    suspend fun setRotation(rotation: Rotation) = edit {
        it[Keys.rotation] = rotation.name
        it[Keys.rotationSince] = System.currentTimeMillis()
    }
    suspend fun setTopShelf(on: Boolean) = edit { it[Keys.topShelf] = on }
    suspend fun setAppBackdrop(on: Boolean) = edit { it[Keys.appBackdrop] = on }
    suspend fun setSleepAt(millis: Long) = edit { it[Keys.sleepAt] = millis }
    suspend fun setAppOrder(order: AppOrder) = edit { it[Keys.appOrder] = order.name }
    suspend fun setHomeSetupSeen() = edit { it[Keys.homeSetupSeen] = true }
    suspend fun setWeatherOn(on: Boolean) = edit { it[Keys.weatherOn] = on }
    suspend fun setTempUnit(unit: TempUnit) = edit { it[Keys.tempUnit] = unit.name }
    suspend fun setWeatherCity(city: City?) = edit { writeCity(it, city) }
    suspend fun setWeatherUseDeviceLocation(on: Boolean) = edit { it[Keys.weatherDeviceLocation] = on }

    /** Writes every non-null field of [patch] in one edit (one recomposition, no half-applied state). */
    suspend fun restore(patch: PrefsPatch) = edit { p ->
        patch.look?.let { p[Keys.look] = it.name }
        patch.wallpaperId?.let { p[Keys.wallpaperId] = it }
        patch.rotation?.let { p[Keys.rotation] = it.name }
        if (patch.wallpaperId != null || patch.rotation != null) p[Keys.rotationSince] = System.currentTimeMillis()
        patch.appBackdrop?.let { p[Keys.appBackdrop] = it }
        patch.topShelf?.let { p[Keys.topShelf] = it }
        patch.clock24?.let { p[Keys.clock24] = it }
        patch.dockSize?.let { p[Keys.dockSize] = it.coerceIn(MIN_DOCK_SIZE, MAX_DOCK_SIZE) }
        patch.favorites?.let { p[Keys.favorites] = encodeList(it) }
        patch.hidden?.let { p[Keys.hidden] = it }
        patch.hud?.let { p[Keys.hud] = it }
        patch.appOrder?.let { p[Keys.appOrder] = it.name }
        patch.weatherOn?.let { p[Keys.weatherOn] = it }
        patch.tempUnit?.let { p[Keys.tempUnit] = it.name }
        patch.weatherCity?.let { writeCity(p, it) }
        patch.weatherUseDeviceLocation?.let { p[Keys.weatherDeviceLocation] = it }
    }

    suspend fun recordLaunch(key: String) = edit {
        it[Keys.recents] = encodeList(pushRecent(it[Keys.recents]?.let(::decodeList).orEmpty(), key))
    }

    private fun readCity(p: Preferences): City? {
        val name = p[Keys.cityName] ?: return null
        val lat = p[Keys.cityLat] ?: return null
        val lon = p[Keys.cityLon] ?: return null
        return City(name, lat, lon)
    }

    /** Null removes the city (automatic location); all three keys change together. */
    private fun writeCity(p: MutablePreferences, city: City?) {
        if (city == null) {
            p.remove(Keys.cityName)
            p.remove(Keys.cityLat)
            p.remove(Keys.cityLon)
        } else {
            p[Keys.cityName] = city.name
            p[Keys.cityLat] = city.latitude
            p[Keys.cityLon] = city.longitude
        }
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
