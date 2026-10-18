package com.kevin.glasslauncher.update

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.MessageDigest

class ApkDownloadTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val url = FIXTURE_APK_URL
    private val apk = ByteArray(1000) { (it % 251).toByte() }
    private val apkSha = MessageDigest.getInstance("SHA-256").digest(apk).toHex()

    private fun release(size: Long? = apk.size.toLong(), sha256: String? = apkSha) =
        ReleaseInfo("0.3.0", "0.3.0", "", url, size, sha256, "")

    private fun target(): File = File(tmp.root, "updates/glass-launcher.apk")

    private fun download(response: FakeResponse, release: ReleaseInfo = release(), maxBytes: Long = MAX_APK_BYTES): File {
        val file = target()
        val progress = mutableListOf<Float>()
        downloadApk(UpdateHttp("Test/1", FakeConnections(mapOf(url to response))), release, file, maxBytes) { progress += it }
        assertEquals(1f, progress.last())
        return file
    }

    @Test
    fun matchingDownloadIsKept() {
        val file = download(FakeResponse.ok(apk))
        assertArrayEquals(apk, file.readBytes())
    }

    @Test
    fun noDigestOrSizeFromGitHubStillDownloads() {
        val file = download(FakeResponse.ok(apk), release(size = null, sha256 = null))
        assertArrayEquals(apk, file.readBytes())
    }

    @Test
    fun sha256MismatchFailsAndDeletesTheFile() {
        val other = "0".repeat(64)
        assertEquals(FailReason.Corrupt, failureOf { download(FakeResponse.ok(apk), release(sha256 = other)) })
        assertFalse(target().exists())
    }

    @Test
    fun releaseSizeOverTheCapIsRefusedBeforeDownloading() {
        val fake = FakeConnections(mapOf(url to FakeResponse.ok(apk)))
        val reason = failureOf { downloadApk(UpdateHttp("Test/1", fake), release(size = MAX_APK_BYTES + 1), target()) }
        assertEquals(FailReason.TooLarge, reason)
        assertTrue(fake.opened.isEmpty())
    }

    @Test
    fun announcedLengthOverTheCapIsRefused() {
        val reason = failureOf { download(FakeResponse.ok(apk), release(size = null), maxBytes = 999) }
        assertEquals(FailReason.TooLarge, reason)
        assertFalse(target().exists())
    }

    @Test
    fun bodyOverTheCapWithoutALengthIsCutOff() {
        val reason = failureOf { download(FakeResponse.ok(apk, contentLength = null), release(size = null, sha256 = null), maxBytes = 999) }
        assertEquals(FailReason.TooLarge, reason)
        assertFalse(target().exists())
    }

    @Test
    fun sizeDifferentFromTheReleaseIsCorrupt() {
        assertEquals(FailReason.Corrupt, failureOf { download(FakeResponse.ok(apk), release(size = 999, sha256 = null)) })
        val short = FakeResponse.ok(apk.copyOf(999), contentLength = null)
        assertEquals(FailReason.Corrupt, failureOf { download(short, release(sha256 = null)) })
        assertFalse(target().exists())
    }

    @Test
    fun aUrlOutsideGlassReleasesIsNeverFetched() {
        val other = "https://github.com/someone/else/releases/download/v0.3.0/glass-launcher.apk"
        val fake = FakeConnections(mapOf(other to FakeResponse.ok(apk)))
        val reason = failureOf { downloadApk(UpdateHttp("Test/1", fake), release().copy(apkUrl = other), target()) }
        assertEquals(FailReason.BadRelease, reason)
        assertTrue(fake.opened.isEmpty())
    }

    @Test
    fun aFailedWriteIsStorageNotNetwork() {
        val full = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("No space left on device")
            }
        }
        val body = Body(ByteArrayInputStream(apk), apk.size.toLong())
        assertEquals(FailReason.Storage, failureOf { save(body, full, null, MAX_APK_BYTES) {} })
    }
}
