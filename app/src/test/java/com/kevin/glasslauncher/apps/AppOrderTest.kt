package com.kevin.glasslauncher.apps

import com.kevin.glasslauncher.data.AppOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOrderTest {
    private fun app(pkg: String, dev: Boolean = false) = AppEntry(pkg, "$pkg.Main", pkg.substringAfterLast('.'), true, isDev = dev)

    private val apps = listOf(app("a.alpha"), app("b.bravo"), app("x.player.dev", dev = true), app("c.charlie"))

    @Test
    fun alphabeticalKeepsOrderButMovesDevBuildsLast() {
        assertEquals(
            listOf("a.alpha", "b.bravo", "c.charlie", "x.player.dev"),
            orderApps(apps, AppOrder.Alphabetical, emptyMap()).map { it.packageName },
        )
    }

    @Test
    fun mostUsedSortsByScreenTimeWithTiesAlphabetical() {
        val usage = mapOf("c.charlie" to 500L, "b.bravo" to 20L, "x.player.dev" to 9_999L)
        assertEquals(
            listOf("c.charlie", "b.bravo", "a.alpha", "x.player.dev"),
            orderApps(apps, AppOrder.MostUsed, usage).map { it.packageName },
        )
    }

    @Test
    fun devBuildsAreDebuggableTestOnlyOrDevNamed() {
        assertTrue(looksLikeDevBuild("com.example.player.dev", debuggable = false, testOnly = false))
        assertTrue(looksLikeDevBuild("com.example", debuggable = true, testOnly = false))
        assertTrue(looksLikeDevBuild("com.example", debuggable = false, testOnly = true))
        assertFalse(looksLikeDevBuild("com.netflix.ninja", debuggable = false, testOnly = false))
        assertFalse(looksLikeDevBuild("com.example.devices", debuggable = false, testOnly = false))
    }

    @Test
    fun recentLaunchesRankMostRecentFirst() {
        val usage = usageFromRecents(listOf("b.bravo/b.Main", "c.charlie/c.Main", "b.bravo/b.Other"))
        assertEquals(listOf("b.bravo", "c.charlie", "a.alpha", "x.player.dev"), orderApps(apps, AppOrder.MostUsed, usage).map { it.packageName })
    }
}
