package com.kevin.glasslauncher.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.AppContainer

/** The weather card over home (LauncherRoot's Overlay.Weather), fed from the app's weather and prefs. */
@Composable
fun WeatherOverlay(container: AppContainer, onClose: () -> Unit) {
    val weather by container.weather.now.collectAsStateWithLifecycle()
    val place by container.weather.place.collectAsStateWithLifecycle()
    val prefs by container.prefs.prefs.collectAsStateWithLifecycle(initialValue = null)
    val name = prefs?.weatherCity?.name ?: place.autoName
    WeatherCard(WeatherCardState(weather, name, prefs?.clock24 ?: false), onClose)
}
