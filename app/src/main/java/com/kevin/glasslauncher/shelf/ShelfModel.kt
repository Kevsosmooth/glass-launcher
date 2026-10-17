package com.kevin.glasslauncher.shelf

import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.home.HomeLayout

/** The Recent row holds one row of tiles, as many as the home grid has columns. */
const val MAX_SHELF_RECENTS = HomeLayout.COLUMNS

/** Recent apps shown on the shelf, most recent first. Pure. */
fun shelfRecents(recents: List<AppEntry>): List<AppEntry> = recents.take(MAX_SHELF_RECENTS)

/**
 * The app the shelf features: the focused dock app, else the focused Recent tile (only while it is
 * still on the row), else none. Pure.
 */
fun featuredApp(dockFocus: AppEntry?, shelfFocus: AppEntry?, shown: List<AppEntry>): AppEntry? =
    dockFocus ?: shelfFocus?.takeIf { it in shown }
