package com.kevin.glasslauncher.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.LauncherApp

/**
 * The weather for the status corner, or null (no data yet, or weather off). Collecting it while
 * the screen is started is what runs the refresh loop, so it stops when an app covers Glass.
 * Null outside the launcher app (e.g. a test host), where there is no repository.
 */
@Composable
fun rememberWeatherNow(): WeatherNow? {
    val app = LocalContext.current.applicationContext as? LauncherApp ?: return null
    val weather by app.container.weather.now.collectAsStateWithLifecycle()
    return weather
}
