package com.kevin.glasslauncher.shelf

import com.kevin.glasslauncher.apps.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShelfModelTest {
    private fun app(i: Int) = AppEntry("p$i", "A$i", "App $i", isLeanback = true)
    private val apps = (1..10).map(::app)

    @Test
    fun recentRowKeepsTheMostRecentSix() {
        assertEquals(apps.take(6), shelfRecents(apps))
        assertEquals(apps.take(2), shelfRecents(apps.take(2)))
        assertEquals(emptyList<AppEntry>(), shelfRecents(emptyList()))
    }

    @Test
    fun dockFocusWinsOverTheRecentRow() {
        val shown = apps.take(6)
        assertEquals(apps[9], featuredApp(apps[9], apps[0], shown))
    }

    @Test
    fun focusedRecentTileIsFeaturedWhileItIsOnTheRow() {
        val shown = apps.take(6)
        assertEquals(apps[2], featuredApp(null, apps[2], shown))
        assertNull(featuredApp(null, apps[7], shown))
    }

    @Test
    fun nothingFocusedFeaturesNothing() {
        assertNull(featuredApp(null, null, apps.take(6)))
    }
}
