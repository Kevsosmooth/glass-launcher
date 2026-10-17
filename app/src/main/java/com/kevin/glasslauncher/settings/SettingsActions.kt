package com.kevin.glasslauncher.settings

import com.kevin.glasslauncher.data.LauncherPrefs

/** Writes this change to [prefs]. */
suspend fun PrefChange.applyTo(prefs: LauncherPrefs) {
    when (this) {
        is PrefChange.SetLook -> prefs.setLook(look)
        is PrefChange.SetRotation -> prefs.setRotation(rotation)
        is PrefChange.SetAppBackdrop -> prefs.setAppBackdrop(on)
        is PrefChange.SetTopShelf -> prefs.setTopShelf(on)
        is PrefChange.SetClock24 -> prefs.setClock24(on)
        is PrefChange.SetDockSize -> prefs.setDockSize(size)
        is PrefChange.SetHud -> prefs.setHud(on)
        is PrefChange.SetWallpaper -> prefs.setWallpaper(id)
        is PrefChange.SetHidden -> prefs.setHidden(keys)
        PrefChange.ResetDock -> prefs.resetFavorites()
    }
}
