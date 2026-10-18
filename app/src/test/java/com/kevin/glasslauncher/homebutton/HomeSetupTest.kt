package com.kevin.glasslauncher.homebutton

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.kevin.glasslauncher.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeSetupTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Leaves the TV's own launcher as the only home app. */
    private fun stockLauncherIsHome() {
        val stock = ComponentName("com.google.android.tvlauncher", "com.google.android.tvlauncher.MainActivity")
        shadowOf(app.packageManager).apply {
            removeActivity(ComponentName(app, MainActivity::class.java))
            addActivityIfNotPresent(stock)
            addIntentFilterForActivity(
                stock,
                IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                },
            )
        }
    }

    @Test
    fun glassAsTheHomeAppCounts() {
        assertTrue(homeOpensGlass(app))
    }

    @Test
    fun stockLauncherNeedsSetup() {
        stockLauncherIsHome()
        assertFalse(homeOpensGlass(app))
    }

    @Test
    fun helperOnCounts() {
        stockLauncherIsHome()
        Settings.Secure.putString(
            app.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "${app.packageName}/${HomeButtonService::class.java.name}",
        )
        assertTrue(homeOpensGlass(app))
    }
}
