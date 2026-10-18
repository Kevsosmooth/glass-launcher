package com.kevin.glasslauncher.update

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import org.robolectric.Shadows.shadowOf

const val INSTALLER_PACKAGE = "com.android.packageinstaller"
const val INSTALLER_ACTIVITY = "com.android.packageinstaller.InstallStart"

/** PackageInstaller's "confirm this install" intent as Android sends it. */
fun confirmIntent(): Intent = Intent("android.content.pm.action.CONFIRM_INSTALL").setPackage(INSTALLER_PACKAGE)

/** Makes [confirmIntent] resolve to the installer activity, a system app or not. */
fun registerConfirmScreen(context: Context, system: Boolean) {
    val application = ApplicationInfo().apply {
        packageName = INSTALLER_PACKAGE
        flags = if (system) ApplicationInfo.FLAG_SYSTEM else 0
    }
    val resolve = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            packageName = INSTALLER_PACKAGE
            name = INSTALLER_ACTIVITY
            applicationInfo = application
        }
    }
    shadowOf(context.packageManager).addResolveInfoForIntent(confirmIntent(), resolve)
}
