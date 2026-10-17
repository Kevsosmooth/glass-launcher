package com.kevin.glasslauncher

import android.app.Application
import android.content.ComponentCallbacks2
import com.kevin.glasslauncher.apps.AppRepository
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.apps.PackageChangeReceiver
import com.kevin.glasslauncher.data.LauncherPrefs
import com.kevin.glasslauncher.glass.WallpaperRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** App-wide singletons; no DI framework (spec). */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val apps = AppRepository(app, scope)
    val banners = BannerCache(app)
    val prefs = LauncherPrefs(app)
    val wallpaper = WallpaperRepository(app, scope, prefs.prefs)
}

class LauncherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        PackageChangeReceiver.register(this, container.apps, container.banners)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Keep banners while merely hidden (an app is in front) so Back to Home stays instant.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) container.banners.clear()
    }
}
