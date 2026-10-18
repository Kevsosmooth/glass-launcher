package com.kevin.glasslauncher.apps

/**
 * One launchable activity. [isLeanback] apps have a TV banner; the rest get an icon tile.
 * [isDev] marks debug and test builds, which sort to the end of the grid.
 */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isLeanback: Boolean,
    val isDev: Boolean = false,
) {
    val key: String get() = "$packageName/$activityName"
}
