package com.kevin.glasslauncher.edit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** System App info page for [packageName]; false if this device has none. */
fun openAppInfo(context: Context, packageName: String): Boolean =
    startSystemPage(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(packageName)))

/** System uninstall prompt for [packageName] (REQUEST_DELETE_PACKAGES is in the manifest). */
fun requestUninstall(context: Context, packageName: String): Boolean =
    startSystemPage(context, Intent(Intent.ACTION_DELETE, packageUri(packageName)))

private fun packageUri(packageName: String): Uri = Uri.fromParts("package", packageName, null)

private fun startSystemPage(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
}
