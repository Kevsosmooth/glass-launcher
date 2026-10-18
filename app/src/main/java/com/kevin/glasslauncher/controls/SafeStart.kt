package com.kevin.glasslauncher.controls

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Android TV's placeholder for settings screens a TV leaves out: it only shows "You don't have an
 * app that can do this" (the Shield routes ACTION_ACCESSIBILITY_SETTINGS there).
 */
internal const val TV_STUB_PACKAGE = "com.google.android.tv.frameworkpackagestubs"

/**
 * Starts [intent] in its own task; false (no crash) when the device has nothing real to handle it.
 * The stub only counts when it is visible to us, so intents it may catch need a manifest query.
 */
fun Context.tryStart(intent: Intent): Boolean {
    val target = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    if (target?.activityInfo?.packageName == TV_STUB_PACKAGE) return false
    return try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
