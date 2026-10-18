package com.kevin.glasslauncher.apps

import com.kevin.glasslauncher.data.AppOrder

private val DEV_SUFFIXES = listOf(".dev", ".debug", ".beta", ".test")

/** Debug or test-only builds, or packages named like one (com.example.app.dev). */
fun looksLikeDevBuild(packageName: String, debuggable: Boolean, testOnly: Boolean): Boolean =
    debuggable || testOnly || DEV_SUFFIXES.any { packageName.endsWith(it) }

/**
 * Grid order. [apps] arrive A to Z; [MostUsed] puts the apps with the most screen time ([usage],
 * millis by package) first, ties staying A to Z. Dev builds always go last, so test installs
 * never push everyday apps down.
 */
fun orderApps(apps: List<AppEntry>, order: AppOrder, usage: Map<String, Long>): List<AppEntry> {
    val (dev, everyday) = apps.partition { it.isDev }
    val sorted = when (order) {
        AppOrder.Alphabetical -> everyday
        AppOrder.MostUsed -> everyday.sortedByDescending { usage[it.packageName] ?: 0L }
    }
    return sorted + dev
}

/**
 * Usage to sort by when Android's usage history is off: recent launches (most recent first) rank
 * above everything else, in that order.
 */
fun usageFromRecents(recents: List<String>): Map<String, Long> =
    recents.map { it.substringBefore('/') }.distinct().withIndex().associate { (i, pkg) -> pkg to (recents.size - i).toLong() }
