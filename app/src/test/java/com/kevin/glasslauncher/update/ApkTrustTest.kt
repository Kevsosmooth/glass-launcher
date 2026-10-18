package com.kevin.glasslauncher.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkTrustTest {
    private val ours = setOf(sha256Hex("glass release key".toByteArray()))
    private val installed = ApkIdentity("com.kevin.glasslauncher", 2, ours)

    private fun problem(candidate: ApkIdentity?) = upgradeProblem(candidate, installed, checkCertificates = true)

    @Test
    fun sameAppNewerVersionSameKeyIsAllowed() {
        assertNull(problem(installed.copy(versionCode = 3)))
    }

    @Test
    fun anotherPackageIsRefused() {
        assertEquals(FailReason.NotTrusted, problem(installed.copy(packageName = "com.evil.app", versionCode = 3)))
    }

    @Test
    fun sameOrLowerVersionIsRefused() {
        assertEquals(FailReason.NotNewer, problem(installed))
        assertEquals(FailReason.NotNewer, problem(installed.copy(versionCode = 1)))
    }

    @Test
    fun differentOrExtraCertificateIsRefused() {
        val other = setOf(sha256Hex("someone else".toByteArray()))
        assertEquals(FailReason.NotTrusted, problem(installed.copy(versionCode = 3, certSha256 = other)))
        assertEquals(FailReason.NotTrusted, problem(installed.copy(versionCode = 3, certSha256 = ours + other)))
        assertEquals(FailReason.NotTrusted, problem(installed.copy(versionCode = 3, certSha256 = emptySet())))
    }

    @Test
    fun unknownInstalledCertificatesTrustNothing() {
        val blind = installed.copy(certSha256 = emptySet())
        assertEquals(FailReason.NotTrusted, upgradeProblem(blind.copy(versionCode = 3), blind, checkCertificates = true))
    }

    @Test
    fun unreadableApkIsCorrupt() {
        assertEquals(FailReason.Corrupt, problem(null))
        assertEquals(FailReason.Corrupt, upgradeProblem(null, installed, checkCertificates = false))
    }

    @Test
    fun withoutCertificatesPackageAndVersionAreStillChecked() {
        val noCerts = installed.copy(versionCode = 3, certSha256 = emptySet())
        assertNull(upgradeProblem(noCerts, installed, checkCertificates = false))
        assertEquals(FailReason.NotTrusted, upgradeProblem(noCerts.copy(packageName = "com.evil.app"), installed, checkCertificates = false))
        assertEquals(FailReason.NotNewer, upgradeProblem(noCerts.copy(versionCode = 2), installed, checkCertificates = false))
    }

    @Test
    fun apkCertificatesAreReadFromAndroid11On() {
        assertFalse(archiveCertificatesReadable(28))
        assertFalse(archiveCertificatesReadable(29))
        assertTrue(archiveCertificatesReadable(30))
        assertTrue(archiveCertificatesReadable(36))
    }

    @Test
    fun certificateHashIsLowercaseHexSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc".toByteArray()))
    }
}
