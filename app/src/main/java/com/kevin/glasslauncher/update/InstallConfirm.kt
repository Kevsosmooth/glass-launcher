package com.kevin.glasslauncher.update

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

private const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or
    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

/**
 * PackageInstaller's "confirm this install" intent, made safe to start: no URI grant flags, no
 * selector, and pinned to the activity it resolves to, which must belong to a system app. Null when
 * it resolves to nothing or to an ordinary app.
 */
fun systemConfirmIntent(pm: PackageManager, confirm: Intent): Intent? {
    val safe = Intent(confirm).apply {
        removeFlags(GRANT_FLAGS)
        selector = null
    }
    val activity = pm.resolveDefault(safe)?.activityInfo ?: return null
    if (activity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) return null
    return safe.setComponent(ComponentName(activity.packageName, activity.name))
}

private fun PackageManager.resolveDefault(intent: Intent): ResolveInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
