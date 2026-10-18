package com.kevin.glasslauncher.update

import android.content.Context
import android.content.SharedPreferences

private const val FILE = "updates"
private const val KEY_LAST_CHECK = "last_check"
private const val KEY_DISMISSED = "dismissed_version"
private const val KEY_VERSION = "release_version"
private const val KEY_TITLE = "release_title"
private const val KEY_NOTES = "release_notes"
private const val KEY_URL = "release_url"
private const val KEY_SIZE = "release_size"
private const val KEY_SHA256 = "release_sha256"
private const val KEY_PUBLISHED = "release_published"

/**
 * The updater's own small store ("updates" SharedPreferences): when GitHub was last asked, the
 * newer release it found (so the card survives a restart without a request), and the version the
 * user said Later to.
 */
class UpdateStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    var lastCheck: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    var dismissedVersion: String?
        get() = prefs.getString(KEY_DISMISSED, null)
        set(value) = prefs.edit().putString(KEY_DISMISSED, value).apply()

    /** The saved release, re-checked with [isReleaseApkUrl] since the file is not trusted more than GitHub is. */
    var release: ReleaseInfo?
        get() {
            val version = prefs.getString(KEY_VERSION, null) ?: return null
            val url = prefs.getString(KEY_URL, null)?.takeIf(::isReleaseApkUrl) ?: return null
            return ReleaseInfo(
                version = version,
                title = prefs.getString(KEY_TITLE, null) ?: version,
                notes = prefs.getString(KEY_NOTES, null).orEmpty(),
                apkUrl = url,
                size = prefs.getLong(KEY_SIZE, -1L).takeIf { it > 0 },
                sha256 = prefs.getString(KEY_SHA256, null),
                publishedAt = prefs.getString(KEY_PUBLISHED, null).orEmpty(),
            )
        }
        set(value) {
            val edit = prefs.edit()
            if (value == null) {
                listOf(KEY_VERSION, KEY_TITLE, KEY_NOTES, KEY_URL, KEY_SIZE, KEY_SHA256, KEY_PUBLISHED).forEach(edit::remove)
            } else {
                edit.putString(KEY_VERSION, value.version)
                    .putString(KEY_TITLE, value.title)
                    .putString(KEY_NOTES, value.notes)
                    .putString(KEY_URL, value.apkUrl)
                    .putLong(KEY_SIZE, value.size ?: -1L)
                    .putString(KEY_SHA256, value.sha256)
                    .putString(KEY_PUBLISHED, value.publishedAt)
            }
            edit.apply()
        }
}
