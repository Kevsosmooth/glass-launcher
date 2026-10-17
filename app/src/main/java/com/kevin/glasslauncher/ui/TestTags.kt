package com.kevin.glasslauncher.ui

/**
 * Compose test tags. LauncherRoot sets testTagsAsResourceId, so these show up as resource-id in
 * `uiautomator dump`; tools/check_layout.py measures the screen through them. Renaming one
 * breaks the layout check: change both together.
 */
object TestTags {
    const val HOME = "home"
    const val STATUS_PILL = "status_pill"
    const val TOP_SHELF = "top_shelf"
    const val SHELF_TILE = "shelf_tile"
    const val DOCK = "dock"
    const val DOCK_TILE = "dock_tile"
    const val GRID_TILE = "grid_tile"
    const val APP_MENU = "app_menu"
    const val SETTINGS = "settings"
    const val QUICK_CONTROLS = "quick_controls"
}
