package com.kevin.glasslauncher.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSourceTest {
    private fun app(pkg: String, label: String, leanback: Boolean, activity: String = ".Main") =
        AppEntry(pkg, pkg + activity, label, leanback)

    @Test
    fun leanbackWinsOverLauncherForSamePackage() {
        val merged = mergeApps(
            leanback = listOf(app("a.tv", "Alpha", true, ".TvMain")),
            launcher = listOf(app("a.tv", "Alpha phone", false)),
            selfPackage = "me",
        )
        assertEquals(listOf(app("a.tv", "Alpha", true, ".TvMain")), merged)
    }

    @Test
    fun launcherOnlyAppsAreKeptAsIconApps() {
        val merged = mergeApps(emptyList(), listOf(app("b.phone", "Beta", false)), "me")
        assertEquals(false, merged.single().isLeanback)
    }

    @Test
    fun selfIsRemovedAndResultSortedByLabelIgnoringCase() {
        val merged = mergeApps(
            leanback = listOf(app("z", "zeta", true), app("me", "Glass", true), app("y", "Beta", true)),
            launcher = listOf(app("x", "alpha", false)),
            selfPackage = "me",
        )
        assertEquals(listOf("alpha", "Beta", "zeta"), merged.map { it.label })
    }

    @Test
    fun duplicateEntriesInOneListCollapse() {
        val merged = mergeApps(listOf(app("a", "A", true), app("a", "A", true)), emptyList(), "me")
        assertEquals(1, merged.size)
    }

    @Test
    fun keyIsPackageSlashActivity() {
        assertEquals("a/a.Main", app("a", "A", true).key)
    }
}
