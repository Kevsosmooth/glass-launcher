package com.kevin.glasslauncher.apps

/** One launchable activity. [isLeanback] apps have a TV banner; the rest get an icon tile. */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
) {
    val key: String get() = "$packageName/$activityName"
}
