package com.kevin.glasslauncher.home

import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.orderApps
import com.kevin.glasslauncher.apps.usageFromRecents
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
    val topShelf: Boolean,
    val appBackdrop: Boolean,
)

/**
 * First-run dock: the apps with the most screen time ([usage], millis by package), then leanback
 * (banner) apps, then the rest, in list order. Dev builds only fill seats nothing else can.
 */
fun defaultFavorites(apps: List<AppEntry>, count: Int, usage: Map<String, Long> = emptyMap()): List<String> {
    val (dev, everyday) = apps.partition { it.isDev }
    val used = everyday.filter { (usage[it.packageName] ?: 0L) > 0L }.sortedByDescending { usage[it.packageName] }
    val rest = everyday - used.toSet()
    return (used + rest.filter { it.isLeanback } + rest.filterNot { it.isLeanback } + dev).take(count).map { it.key }
}

/**
 * [usage] is screen time per package; when Android's usage history is off it is empty and recent
 * launches stand in for it.
 */
fun buildHomeState(
    apps: List<AppEntry>,
    prefs: Prefs,
    dockSize: Int = prefs.dockSize,
    usage: Map<String, Long> = emptyMap(),
): HomeState {
    val ranking = usage.ifEmpty { usageFromRecents(prefs.recents) }
    val visible = orderApps(apps.filterNot { it.key in prefs.hidden }, prefs.appOrder, ranking)
    val byKey = visible.associateBy { it.key }
    val favorites = prefs.favorites ?: defaultFavorites(visible, dockSize, ranking)
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
        topShelf = prefs.topShelf,
        appBackdrop = prefs.appBackdrop,
    )
}
