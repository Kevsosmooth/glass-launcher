package com.kevin.glasslauncher.home

import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.data.AppOrder
import com.kevin.glasslauncher.data.Look
import com.kevin.glasslauncher.data.Prefs
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStateTest {
    private fun app(pkg: String, leanback: Boolean = true) = AppEntry(pkg, "$pkg.Main", pkg, leanback)
    private val apps = listOf(app("a"), app("b", false), app("c"), app("d"), app("e"), app("f"), app("g"), app("h"))
    private val defaults = Prefs(favorites = null, hidden = emptySet(), look = Look.Glass, clock24 = false, hud = false)

    @Test
    fun defaultFavoritesPreferLeanbackApps() {
        assertEquals(listOf("a", "c", "d"), defaultFavorites(apps, 3).map { it.substringBefore('/') })
    }

    @Test
    fun noSavedFavoritesUsesDefaults() {
        val s = buildHomeState(apps, defaults, dockSize = 6)
        assertEquals(listOf("a", "c", "d", "e", "f", "g"), s.dock.map { it.packageName })
    }

    @Test
    fun savedFavoritesKeepOrderAndSkipUninstalled() {
        val prefs = defaults.copy(favorites = listOf(app("h").key, "gone/gone.Main", app("b", false).key))
        val s = buildHomeState(apps, prefs, dockSize = 6)
        assertEquals(listOf("h", "b"), s.dock.map { it.packageName })
    }

    @Test
    fun dockIsCappedAtDockSize() {
        val prefs = defaults.copy(favorites = apps.map { it.key })
        assertEquals(4, buildHomeState(apps, prefs, dockSize = 4).dock.size)
    }

    @Test
    fun gridHasVisibleAppsNotInTheDock() {
        val prefs = defaults.copy(hidden = setOf(app("c").key))
        val s = buildHomeState(apps, prefs, dockSize = 6)
        assertEquals(listOf("a", "d", "e", "f", "g", "h"), s.dock.map { it.packageName })
        assertEquals(listOf("b"), s.grid.map { it.packageName })
        assertEquals(false, s.grid.any { it.packageName == "c" })
        assertEquals(false, s.dock.any { it.packageName == "c" })
    }

    @Test
    fun lookAndClockPassThrough() {
        val s = buildHomeState(apps, defaults.copy(look = Look.Solid, clock24 = true), 6)
        assertEquals(Look.Solid, s.look)
        assertEquals(true, s.clock24)
    }

    @Test
    fun recentsSkipHiddenAndUninstalled() {
        val prefs = defaults.copy(recents = listOf(app("d").key, "gone/gone.Main", app("c").key), hidden = setOf(app("c").key))
        assertEquals(listOf("d"), buildHomeState(apps, prefs).recents.map { it.packageName })
    }

    @Test
    fun dockSizeComesFromPrefs() {
        assertEquals(4, buildHomeState(apps, defaults.copy(dockSize = 4)).dock.size)
    }

    @Test
    fun mostUsedAppsFillTheDefaultDockAndDevBuildsSinkToTheEnd() {
        val withDev = apps + AppEntry("z.app.dev", "z.Main", "Dev", true, isDev = true)
        val usage = mapOf("g" to 900L, "b" to 500L, "z.app.dev" to 9_999L)
        val s = buildHomeState(withDev, defaults, dockSize = 4, usage = usage)
        assertEquals(listOf("g", "b", "a", "c"), s.dock.map { it.packageName })
        assertEquals("z.app.dev", s.grid.last().packageName)
    }

    @Test
    fun recentLaunchesRankAppsWhenUsageHistoryIsOff() {
        val prefs = defaults.copy(favorites = emptyList(), recents = listOf("h/h.Main", "e/e.Main"))
        assertEquals(listOf("h", "e", "a"), buildHomeState(apps, prefs).grid.take(3).map { it.packageName })
    }

    @Test
    fun alphabeticalIgnoresUsage() {
        val prefs = defaults.copy(favorites = emptyList(), appOrder = AppOrder.Alphabetical)
        val s = buildHomeState(apps, prefs, usage = mapOf("h" to 100L))
        assertEquals(apps.map { it.packageName }, s.grid.map { it.packageName })
    }
}
