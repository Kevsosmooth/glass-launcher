package com.kevin.glasslauncher.debug

import android.content.Intent
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.data.Look
import kotlinx.coroutines.launch

private const val EXTRA_HUD = "hud"
private const val EXTRA_FAKE_APPS = "fake_apps"
private const val EXTRA_LOOK = "look"
private const val EXTRA_WALLPAPER = "wallpaper"
private const val EXTRA_ONLY_PACKAGES = "only_packages"
private const val EXTRA_BACKDROP = "backdrop"
private const val EXTRA_TOP_SHELF = "top_shelf"
private const val EXTRA_RESET_DOCK = "reset_dock"
private const val MAX_FAKE_APPS = 500

/**
 * Debug-build switches via launch extras, e.g.
 * `am start -n com.kevin.glasslauncher/.MainActivity --ez hud true --ei fake_apps 200
 *  --es look Solid --ez backdrop false --ez top_shelf false --es wallpaper ocean --es only_packages com.netflix.ninja,org.videolan.vlc`
 * (an empty only_packages lists every app again). `--ez reset_dock true` drops the pinned dock
 * apps so it falls back to the first-run default.
 * Release builds ignore them (BuildConfig.DEBUG is a constant, so R8 drops this code).
 */
object DebugTools {
    fun applyLaunchExtras(intent: Intent, container: AppContainer) {
        if (!BuildConfig.DEBUG) return
        if (intent.hasExtra(EXTRA_HUD)) {
            val on = intent.getBooleanExtra(EXTRA_HUD, false)
            container.scope.launch { container.prefs.setHud(on) }
        }
        Look.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_LOOK) }?.let {
            container.scope.launch { container.prefs.setLook(it) }
        }
        if (intent.hasExtra(EXTRA_BACKDROP)) {
            val on = intent.getBooleanExtra(EXTRA_BACKDROP, true)
            container.scope.launch { container.prefs.setAppBackdrop(on) }
        }
        if (intent.hasExtra(EXTRA_TOP_SHELF)) {
            val on = intent.getBooleanExtra(EXTRA_TOP_SHELF, true)
            container.scope.launch { container.prefs.setTopShelf(on) }
        }
        if (intent.getBooleanExtra(EXTRA_RESET_DOCK, false)) {
            container.scope.launch { container.prefs.resetFavorites() }
        }
        intent.getStringExtra(EXTRA_WALLPAPER)?.let { container.scope.launch { container.prefs.setWallpaper(it) } }
        intent.getStringExtra(EXTRA_ONLY_PACKAGES)?.let { list ->
            container.apps.onlyPackages = list.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { null }
        }
        if (intent.hasExtra(EXTRA_FAKE_APPS)) {
            container.apps.extraApps = fakeApps(intent.getIntExtra(EXTRA_FAKE_APPS, 0))
        }
    }
}

/** Synthetic icon-tile apps for the 200-app stress case; launching one just shows the error toast. */
fun fakeApps(count: Int): List<AppEntry> = List(count.coerceIn(0, MAX_FAKE_APPS)) { i ->
    AppEntry("fake.app$i", "fake.app$i.Main", "Test app ${i + 1}", isLeanback = false)
}
