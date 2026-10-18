package com.kevin.glasslauncher.controls

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SafeStartTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun handle(action: String, pkg: String) {
        val component = ComponentName(pkg, "$pkg.Handler")
        shadowOf(app.packageManager).apply {
            addActivityIfNotPresent(component)
            addIntentFilterForActivity(
                component,
                IntentFilter(action).apply { addCategory(Intent.CATEGORY_DEFAULT) },
            )
        }
    }

    @Test
    fun skipsTheTvStub() {
        handle(Settings.ACTION_ACCESSIBILITY_SETTINGS, TV_STUB_PACKAGE)
        assertFalse(app.tryStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
        assertNull(shadowOf(app).nextStartedActivity)
    }

    @Test
    fun startsARealScreen() {
        handle(Settings.ACTION_ACCESSIBILITY_SETTINGS, "com.android.tv.settings")
        assertTrue(app.tryStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, shadowOf(app).nextStartedActivity.action)
    }
}
