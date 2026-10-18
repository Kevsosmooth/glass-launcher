package com.kevin.glasslauncher.homebutton

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * True when the Home button already opens Glass: Android's default home app is Glass (no other
 * launcher left, or the user picked it), or the Home button helper is on.
 */
fun homeOpensGlass(context: Context): Boolean {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val target = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    return target?.activityInfo?.packageName == context.packageName || HomeButtonService.isOn(context)
}
