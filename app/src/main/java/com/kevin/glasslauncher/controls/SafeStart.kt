package com.kevin.glasslauncher.controls

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** Starts [intent] in its own task; false (no crash) when the device has nothing to handle it. */
fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}
