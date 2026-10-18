package com.kevin.glasslauncher.weather

import android.content.Context
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.data.City
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.TempUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Keeps the loop alive across a quick config change, but stops it soon after an app covers Glass.
private const val STOP_AFTER_MS = 5_000L

/** The weather settings the loop depends on. */
private data class WeatherSettings(val on: Boolean, val unit: TempUnit, val city: City?, val useDevice: Boolean)

/**
 * The weather next to the clock. [now] refreshes every 30 minutes, but only while someone collects
 * it (the home screen with collectAsStateWithLifecycle): nothing runs while an app is in front.
 * The last good weather and location are kept in memory and in [store], so the corner shows at once
 * after a restart; failures keep the old value. [place] is what Settings shows for Automatic.
 */
class WeatherRepository(
    prefs: Flow<Prefs>,
    private val api: WeatherApi,
    private val resolver: LocationResolver,
    private val store: WeatherStore,
    scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val placeState = MutableStateFlow(LocationState(located = store.loadLocation()))
    private var last: WeatherNow? = store.loadWeather()
    private var failedAt: Long? = null
    private var locateNow = false
    private var previous: WeatherSettings? = null

    val place: StateFlow<LocationState> = placeState.asStateFlow()

    val now: StateFlow<WeatherNow?> = channelFlow {
        prefs.map { WeatherSettings(it.weatherOn, it.tempUnit, it.weatherCity, it.weatherUseDeviceLocation) }
            .distinctUntilChanged()
            .collectLatest { settings ->
                // A settings change is the user asking: look the location up again right away.
                failedAt = null
                if (previous?.let { it.useDevice != settings.useDevice } == true) locateNow = true
                previous = settings
                if (!settings.on) {
                    send(null)
                    return@collectLatest
                }
                while (true) {
                    send(showable(settings))
                    refresh(settings)
                    send(showable(settings))
                    delay(WeatherTiming.nextCheckIn(last, placeState.value.located, settings.unit, clock()))
                }
            }
    }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), null)

    /** Cities matching [query] for the Settings search, or null if the search failed. */
    suspend fun searchCities(query: String): List<City>? = api.searchCities(query)

    private fun showable(settings: WeatherSettings): WeatherNow? =
        WeatherTiming.showable(last, placeState.value.located, settings.unit, clock())

    private suspend fun refresh(settings: WeatherSettings) {
        val located = updateLocation(settings.city, settings.useDevice) ?: return
        if (!WeatherTiming.weatherDue(last, located, settings.unit, clock())) return
        val fresh = api.forecast(located.latitude, located.longitude, settings.unit, clock()) ?: return
        last = fresh.weather
        store.saveWeather(fresh)
    }

    /** The location to use now, looked up again when due; a failed lookup keeps an automatic one. */
    private suspend fun updateLocation(picked: City?, useDevice: Boolean): Located? {
        val cached = placeState.value.located
        val now = clock()
        if (!locateNow && !WeatherTiming.locationDue(cached, picked != null, useDevice, now)) return cached
        locateNow = false
        // A city the user has just cleared, or the TV's location once not allowed, must not linger.
        val fallback = cached?.takeIf { WeatherTiming.usableAutomatic(it, useDevice) }
        if (picked == null && !WeatherTiming.retryDue(failedAt, now)) return fallback
        when (val result = resolver.resolve(picked, useDevice)) {
            is Resolution.Found -> {
                failedAt = null
                placeState.value = LocationState(result.located)
                store.saveLocation(result.located)
            }
            Resolution.LooksWrong -> {
                failedAt = now
                placeState.value = LocationState(fallback, looksWrong = true)
            }
            Resolution.NotFound -> {
                failedAt = now
                placeState.value = LocationState(fallback, placeState.value.looksWrong)
            }
        }
        return placeState.value.located
    }

    companion object {
        fun create(context: Context, scope: CoroutineScope, prefs: Flow<Prefs>): WeatherRepository {
            val api = WeatherApi(WeatherHttp(BuildConfig.VERSION_NAME))
            val resolver = LocationResolver(AndroidNetworkLocator(context), api::ipGeo)
            return WeatherRepository(prefs, api, resolver, SharedPrefsWeatherStore(context), scope)
        }
    }
}
