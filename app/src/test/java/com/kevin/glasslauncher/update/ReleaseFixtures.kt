package com.kevin.glasslauncher.update

const val FIXTURE_SHA = "8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4"
const val FIXTURE_APK_URL = "https://github.com/Kevsosmooth/glass-launcher/releases/download/v0.3.0/glass-launcher.apk"

/** GitHub's "latest release" JSON, trimmed to what Glass reads plus a little noise. */
fun releaseJson(
    tag: String = "v0.3.0",
    body: String = "## Fixes\\n- Faster",
    assetName: String = APK_ASSET_NAME,
    digest: String? = "sha256:$FIXTURE_SHA",
    size: Long = 1234,
): String {
    val digestField = digest?.let { "\"digest\": \"$it\"," }.orEmpty()
    return """
        {
          "tag_name": "$tag",
          "name": "Glass $tag",
          "body": "$body",
          "html_url": "https://github.com/Kevsosmooth/glass-launcher/releases/tag/$tag",
          "published_at": "2026-10-18T12:00:00Z",
          "draft": false,
          "assets": [
            { "name": "notes.txt", "size": 5, "browser_download_url": "https://github.com/x/notes.txt" },
            { "name": "$assetName", "size": $size, $digestField
              "browser_download_url": "$FIXTURE_APK_URL" }
          ]
        }
    """.trimIndent()
}
