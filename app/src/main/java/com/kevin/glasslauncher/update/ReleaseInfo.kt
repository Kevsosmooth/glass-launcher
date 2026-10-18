package com.kevin.glasslauncher.update

import org.json.JSONException
import org.json.JSONObject

/** Name of the APK attached to every Glass release. */
const val APK_ASSET_NAME = "glass-launcher.apk"

private const val SHA256_PREFIX = "sha256:"
private val HEX_64 = Regex("[0-9a-f]{64}")
private val VERSION_TAG = Regex("[vV]?([0-9A-Za-z.+-]+)")

// After RELEASE_DOWNLOAD_PREFIX: "<tag>/glass-launcher.apk", the tag not starting with "." (no "." or ".." hops).
private val DOWNLOAD_PATH = Regex("""[0-9A-Za-z+-][0-9A-Za-z.+-]*/""" + Regex.escape(APK_ASSET_NAME))

/**
 * A GitHub release that carries the Glass APK. [notes] are already plain text
 * ([releaseNotesText]); [size] and [sha256] are null when GitHub does not report them.
 */
data class ReleaseInfo(
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val size: Long?,
    val sha256: String?,
    val publishedAt: String,
)

/**
 * Reads GitHub's "latest release" JSON. Null when it is not valid JSON, the tag is not a plain
 * version label (letters, digits, `.+-`, optional leading v), or there is no [APK_ASSET_NAME] asset
 * whose download URL passes [isReleaseApkUrl]. The text is untrusted: notes are flattened to plain
 * text and the digest is only kept when it is a well-formed SHA-256.
 */
fun parseRelease(json: String): ReleaseInfo? = try {
    val root = JSONObject(json)
    val asset = root.optJSONArray("assets")?.let { assets ->
        (0 until assets.length()).mapNotNull(assets::optJSONObject).firstOrNull { it.text("name") == APK_ASSET_NAME }
    }
    val url = asset?.text("browser_download_url").orEmpty()
    val version = VERSION_TAG.matchEntire(root.text("tag_name"))?.groupValues?.get(1)
    if (asset == null || version == null || !isReleaseApkUrl(url)) {
        null
    } else {
        ReleaseInfo(
            version = version,
            title = root.text("name").ifBlank { version },
            notes = releaseNotesText(root.text("body")),
            apkUrl = url,
            size = asset.optLong("size", -1L).takeIf { it > 0 },
            sha256 = parseDigest(asset.text("digest")),
            publishedAt = root.text("published_at"),
        )
    }
} catch (e: JSONException) {
    null
}

/** True for `<RELEASE_DOWNLOAD_PREFIX><tag>/glass-launcher.apk` with nothing else in the URL. */
fun isReleaseApkUrl(url: String): Boolean =
    url.startsWith(RELEASE_DOWNLOAD_PREFIX) &&
        DOWNLOAD_PATH.matches(url.substring(RELEASE_DOWNLOAD_PREFIX.length)) &&
        allowedUrl(url) != null

/** A string field; "" when missing or JSON null (optString would turn null into "null"). */
private fun JSONObject.text(name: String): String = if (isNull(name)) "" else optString(name)

/** "sha256:<hex>" -> "<hex>" (lowercase); null for anything else. */
internal fun parseDigest(digest: String): String? {
    if (!digest.startsWith(SHA256_PREFIX, ignoreCase = true)) return null
    return digest.substring(SHA256_PREFIX.length).lowercase().takeIf { HEX_64.matches(it) }
}
