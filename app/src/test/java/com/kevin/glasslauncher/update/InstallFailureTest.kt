package com.kevin.glasslauncher.update

import android.content.pm.PackageInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstallFailureTest {
    @Test
    fun successIsNoFailure() {
        assertNull(installFailure(PackageInstaller.STATUS_SUCCESS))
    }

    @Test
    fun aDifferentSignerIsNotTrusted() {
        assertEquals(FailReason.NotTrusted, installFailure(PackageInstaller.STATUS_FAILURE_CONFLICT))
        assertEquals(FailReason.NotTrusted, installFailure(PackageInstaller.STATUS_FAILURE_INCOMPATIBLE))
    }

    @Test
    fun otherStatusesKeepTheirMeaning() {
        assertEquals(FailReason.Cancelled, installFailure(PackageInstaller.STATUS_FAILURE_ABORTED))
        assertEquals(FailReason.Storage, installFailure(PackageInstaller.STATUS_FAILURE_STORAGE))
        assertEquals(FailReason.InstallFailed, installFailure(PackageInstaller.STATUS_FAILURE))
        assertEquals(FailReason.InstallFailed, installFailure(PackageInstaller.STATUS_FAILURE_INVALID))
        assertEquals(FailReason.InstallFailed, installFailure(PackageInstaller.STATUS_FAILURE_BLOCKED))
    }
}
