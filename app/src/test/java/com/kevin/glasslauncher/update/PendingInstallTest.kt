package com.kevin.glasslauncher.update

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric for a real Intent. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingInstallTest {
    private val start = 1_800_000_000_000L
    private val confirm = Intent("android.content.pm.action.CONFIRM_INSTALL")

    @Test
    fun anInstallGoesStaleAfterTenMinutesOrWhenTheClockGoesBack() {
        assertFalse(isInstallStale(start, start))
        assertFalse(isInstallStale(start, start + INSTALL_TIMEOUT_MS))
        assertTrue(isInstallStale(start, start + INSTALL_TIMEOUT_MS + 1))
        assertTrue(isInstallStale(start, start - 1))
    }

    @Test
    fun returningShowsTheConfirmationAgainUntilItIsStale() {
        val pending = PendingInstall(start, confirm)
        assertEquals(InstallResume.Confirm(confirm), installResume(pending, start + 60_000))
        assertEquals(InstallResume.GiveUp, installResume(pending, start + INSTALL_TIMEOUT_MS + 1))
    }

    @Test
    fun aConfirmationTheUserHasSeenIsNotShownAgain() {
        val pending = PendingInstall(start, confirm, shown = true)
        assertEquals(InstallResume.Wait, installResume(pending, start + 60_000))
        assertEquals(InstallResume.GiveUp, installResume(pending, start + INSTALL_TIMEOUT_MS + 1))
    }

    @Test
    fun withoutAConfirmationYetItWaits() {
        assertEquals(InstallResume.Wait, installResume(PendingInstall(start), start + 60_000))
        assertEquals(InstallResume.GiveUp, installResume(PendingInstall(start), start + INSTALL_TIMEOUT_MS + 1))
    }

    @Test
    fun nothingPendingGivesUp() {
        assertEquals(InstallResume.GiveUp, installResume(null, start))
    }
}
