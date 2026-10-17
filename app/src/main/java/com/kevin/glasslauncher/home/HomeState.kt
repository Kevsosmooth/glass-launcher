package com.kevin.glasslauncher.home

import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.data.Prefs

/** What the home screen draws. [recents] excludes hidden and uninstalled apps. */
data class HomeState(
    val dock: List<AppEntry>,
    val dockSize: Int,
    val grid: List<AppEntry>,
    val recents: List<AppEntry>,
    val look: Look,
    val clock24: Boolean,
    val hud: Boolean,
    val topShelf: Boolean,
    val appBackdrop: Boolean,
)

/** First-run dock: leanback (banner) apps first, in list order. */
fun defaultFavorites(apps: List<AppEntry>, count: Int): List<String> =
    (apps.filter { it.isLeanback } + apps.filterNot { it.isLeanback }).take(count).map { it.key }

fun buildHomeState(apps: List<AppEntry>, prefs: Prefs, dockSize: Int = prefs.dockSize): HomeState {
    val visible = apps.filterNot { it.key in prefs.hidden }
    val byKey = visible.associateBy { it.key }
    val favorites = prefs.favorites ?: defaultFavorites(visible, dockSize)
    val dock = favorites.mapNotNull { byKey[it] }.take(dockSize)
    val inDock = dock.mapTo(HashSet()) { it.key }
    return HomeState(
        dock = dock,
        dockSize = dockSize,
        // As on tvOS, an app sits in the dock or in the grid, never both.
        grid = visible.filterNot { it.key in inDock },
        recents = prefs.recents.mapNotNull { byKey[it] },
        look = prefs.look,
        clock24 = prefs.clock24,
        hud = prefs.hud,
        topShelf = prefs.topShelf,
        appBackdrop = prefs.appBackdrop,
    )
}
