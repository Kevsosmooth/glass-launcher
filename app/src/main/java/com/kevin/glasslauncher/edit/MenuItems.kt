package com.kevin.glasslauncher.edit

import com.kevin.glasslauncher.apps.AppEntry

enum class MenuAction { Move, RemoveFromDock, AddToDock, Hide, AppInfo, Uninstall }

/** One menu row; a disabled row shows [slot] as the reason ("Dock full", "In dock"). */
data class MenuItem(val action: MenuAction, val enabled: Boolean = true, val slot: DockSlot? = null)

/** The long-pressed tile: which app, whether it is the dock copy, and where it sits. */
class MenuTarget(val app: AppEntry, val inDock: Boolean, val tile: TileHandle)

/** Dock tile: Move, Remove, Hide, App info, Uninstall. Grid tile: Add to dock (if free), Hide, ... */
fun menuItems(inDock: Boolean, slot: DockSlot): List<MenuItem> {
    val tail = listOf(MenuItem(MenuAction.Hide), MenuItem(MenuAction.AppInfo), MenuItem(MenuAction.Uninstall))
    if (inDock) return listOf(MenuItem(MenuAction.Move), MenuItem(MenuAction.RemoveFromDock)) + tail
    val add = MenuItem(MenuAction.AddToDock, enabled = slot == DockSlot.Free, slot = slot.takeIf { it != DockSlot.Free })
    return listOf(add) + tail
}
