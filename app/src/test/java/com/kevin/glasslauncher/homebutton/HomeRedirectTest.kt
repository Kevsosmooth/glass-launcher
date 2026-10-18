package com.kevin.glasslauncher.homebutton

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRedirectTest {
    private val service = "com.kevin.glasslauncher/com.kevin.glasslauncher.homebutton.HomeButtonService"

    @Test
    fun stockHomeScreensAreCovered() {
        assertTrue(isStockHome("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.HomeActivity_vNext"))
        assertTrue(isStockHome("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.HomeActivity"))
        assertTrue(isStockHome("com.google.android.apps.tv.launcherx", "com.google.android.apps.tv.launcherx.home.HomeActivity"))
        assertTrue(isStockHome("com.google.android.tvlauncher", "com.google.android.tvlauncher.MainActivity"))
    }

    @Test
    fun otherScreensAndAppsAreLeftAlone() {
        assertFalse(isStockHome("com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.AppsActivity"))
        assertFalse(isStockHome("com.amazon.tv.launcher", "android.widget.FrameLayout"))
        assertFalse(isStockHome("com.amazon.tv.launcher", null))
        assertFalse(isStockHome("com.example.tv", "com.example.tv.HomeActivity"))
        assertFalse(isStockHome(null, "com.amazon.tv.launcher.ui.HomeActivity"))
        assertFalse(isStockHome("com.google.android.tvlauncher", "com.google.android.tvlauncher.appsview.AppsViewActivity"))
        assertFalse(isStockHome("com.example.tv", "com.example.tv.MainActivity"))
    }

    @Test
    fun homeKeyIsTakenWholeAndOpensGlassOnRelease() {
        assertEquals(KeyVerdict.Swallow, homeKeyVerdict(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_DOWN))
        assertEquals(KeyVerdict.OpenGlass, homeKeyVerdict(KeyEvent.KEYCODE_HOME, KeyEvent.ACTION_UP))
        assertEquals(KeyVerdict.PassThrough, homeKeyVerdict(KeyEvent.KEYCODE_BACK, KeyEvent.ACTION_UP))
        assertEquals(KeyVerdict.PassThrough, homeKeyVerdict(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_DOWN))
    }

    @Test
    fun enabledListAcceptsFullAndShortForms() {
        assertTrue(isServiceEnabled(service, service))
        assertTrue(isServiceEnabled("a.b/.X:com.kevin.glasslauncher/.homebutton.HomeButtonService", service))
        assertFalse(isServiceEnabled("com.spocky.projengmenu/.services.Acc", service))
        assertFalse(isServiceEnabled(null, service))
        assertFalse(isServiceEnabled("", service))
        assertFalse(isServiceEnabled("com.kevin.glasslauncher", service))
    }
}
