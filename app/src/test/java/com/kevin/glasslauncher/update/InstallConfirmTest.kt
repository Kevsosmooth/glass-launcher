package com.kevin.glasslauncher.update

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InstallConfirmTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun aSystemInstallerScreenIsPinnedAndLosesUriGrants() {
        registerConfirmScreen(app, system = true)
        val confirm = confirmIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)

        val safe = systemConfirmIntent(app.packageManager, confirm)

        assertEquals(ComponentName(INSTALLER_PACKAGE, INSTALLER_ACTIVITY), safe?.component)
        assertEquals(0, (safe?.flags ?: -1) and GRANTS)
    }

    @Test
    fun anOrdinaryAppIsRefused() {
        registerConfirmScreen(app, system = false)
        assertNull(systemConfirmIntent(app.packageManager, confirmIntent()))
    }

    @Test
    fun nothingToHandleItIsRefused() {
        assertNull(systemConfirmIntent(app.packageManager, confirmIntent()))
    }

    private companion object {
        const val GRANTS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }
}
