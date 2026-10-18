package com.kevin.glasslauncher.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs on Robolectric for Android's real org.json (the plain JVM android.jar only has stubs). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReleaseInfoTest {
    @Test
    fun readsTheApkAssetWithItsDigest() {
        val release = parseRelease(releaseJson())
        assertEquals(
            ReleaseInfo("0.3.0", "Glass v0.3.0", "Fixes\n\u2022 Faster", FIXTURE_APK_URL, 1234L, FIXTURE_SHA, "2026-10-18T12:00:00Z"),
            release,
        )
    }

    @Test
    fun missingOrMalformedDigestMeansNoChecksum() {
        assertNull(parseRelease(releaseJson(digest = null))?.sha256)
        assertNull(parseRelease(releaseJson(digest = "md5:abc"))?.sha256)
        assertNull(parseRelease(releaseJson(digest = "sha256:xyz"))?.sha256)
        assertEquals(FIXTURE_SHA, parseRelease(releaseJson(digest = "SHA256:${FIXTURE_SHA.uppercase()}"))?.sha256)
    }

    @Test
    fun noApkAssetMeansNoRelease() {
        assertNull(parseRelease(releaseJson(assetName = "other.apk")))
        assertNull(parseRelease("""{"tag_name": "v0.3.0", "assets": []}"""))
        assertNull(parseRelease("""{"tag_name": "v0.3.0"}"""))
    }

    @Test
    fun brokenJsonOrMissingTagMeansNoRelease() {
        assertNull(parseRelease("not json"))
        assertNull(parseRelease(releaseJson(tag = "")))
    }

    @Test
    fun nullBodyAndUnknownSizeAreEmpty() {
        val json = releaseJson(size = 0).replace("\"body\": \"## Fixes\\n- Faster\"", "\"body\": null")
        val release = parseRelease(json)
        assertEquals("", release?.notes)
        assertNull(release?.size)
    }

    @Test
    fun tagMustBeAPlainVersionLabel() {
        assertEquals("0.3.0-beta.1+7", parseRelease(releaseJson(tag = "v0.3.0-beta.1+7"))?.version)
        assertEquals("0.3.0", parseRelease(releaseJson(tag = "V0.3.0"))?.version)
        assertNull(parseRelease(releaseJson(tag = "v0.3.0 beta")))
        assertNull(parseRelease(releaseJson(tag = "v0.3.0<b>")))
        assertNull(parseRelease(releaseJson(tag = "v0.3/0")))
        assertNull(parseRelease(releaseJson(tag = " v0.3.0")))
    }

    @Test
    fun apkMustComeFromGlassReleases() {
        fun withUrl(url: String) = parseRelease(releaseJson().replace(FIXTURE_APK_URL, url))
        assertNull(withUrl("https://github.com/someone/else/releases/download/v0.3.0/glass-launcher.apk"))
        assertNull(withUrl("https://objects.githubusercontent.com/glass-launcher.apk"))
        assertNull(withUrl("${RELEASE_DOWNLOAD_PREFIX}../../../../someone/else/releases/download/v1/glass-launcher.apk"))
        assertNull(withUrl("${RELEASE_DOWNLOAD_PREFIX}v0.3.0/glass-launcher.apk?x=1"))
        assertNull(withUrl("http://github.com/Kevsosmooth/glass-launcher/releases/download/v0.3.0/glass-launcher.apk"))
    }
}
