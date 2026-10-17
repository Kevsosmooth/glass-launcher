package com.kevin.glasslauncher.apps

/**
 * Leanback entries win per package; phone-launcher apps without a leanback entry are kept as
 * icon apps. Drops this launcher itself and duplicates, sorted by label ignoring case.
 */
fun mergeApps(leanback: List<AppEntry>, launcher: List<AppEntry>, selfPackage: String): List<AppEntry> {
    val byPackage = LinkedHashMap<String, AppEntry>()
    for (app in leanback) byPackage.putIfAbsent(app.packageName, app)
    for (app in launcher) byPackage.putIfAbsent(app.packageName, app.copy(isLeanback = false))
    byPackage.remove(selfPackage)
    return byPackage.values.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
