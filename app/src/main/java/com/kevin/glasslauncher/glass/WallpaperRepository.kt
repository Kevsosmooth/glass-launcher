package com.kevin.glasslauncher.glass

import android.content.Context
import com.kevin.glasslauncher.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Follows [prefs] (wallpaper id + rotation) and paints the wallpaper on [scope]'s background
 * dispatcher, only when the wallpaper to show actually changes. The previous wallpaper is just
 * dropped: whatever still draws it (the crossfade) keeps it alive until it lets go, then GC frees it.
 */
class WallpaperRepository(context: Context, scope: CoroutineScope, prefs: Flow<Prefs>) {
    private val _wallpaper = MutableStateFlow<Wallpaper?>(null)
    val wallpaper: StateFlow<Wallpaper?> = _wallpaper.asStateFlow()

    init {
        val metrics = context.resources.displayMetrics
        val assets = context.assets
        scope.launch {
            effectiveWallpaperIds(prefs, clockJumps(context.applicationContext)).collect { id ->
                _wallpaper.value = WallpaperFactory.create(WallpaperCatalog.byId(id), metrics.widthPixels, metrics.heightPixels, assets)
            }
        }
    }
}
