package com.kevin.glasslauncher.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.kevin.glasslauncher.AppContainer
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.edit.AppMenu
import com.kevin.glasslauncher.edit.EditActions
import com.kevin.glasslauncher.edit.HomeEditor
import com.kevin.glasslauncher.edit.MenuAction
import com.kevin.glasslauncher.edit.MenuTarget
import com.kevin.glasslauncher.edit.TileHandle
import com.kevin.glasslauncher.edit.dockSlot
import com.kevin.glasslauncher.edit.menuItems

/**
 * Joins edit mode to the home screen: opens a tile's menu, runs the picked action, and says
 * where focus goes next ([restore] the tile, or the first dock tile when the tile is gone).
 */
@Stable
class HomeEditHost(val editor: HomeEditor, private val actions: EditActions, private val dockFocus: DockFocus) {
    /** Tile to refocus once the menu has left the screen; HomeScreen clears it with [restored]. */
    var restore by mutableStateOf<FocusRequester?>(null)
        private set

    fun openMenu(app: AppEntry, place: TilePlace, tile: TileHandle) {
        if (editor.moving != null) return
        dockFocus.hold(true)
        editor.openMenu(MenuTarget(app, place == TilePlace.Dock, tile))
    }

    /** Runs [action] on the open menu's app; [dock] is the dock as shown, [dockSize] its capacity. */
    fun pick(action: MenuAction, dock: List<String>, dockSize: Int) {
        val target = editor.menu ?: return
        hideMenu()
        if (actions.pick(action, target, dock, dockSize)) restore = target.tile.focus else dockFocus.wantFirst = true
    }

    fun closeMenu() {
        val target = editor.menu ?: return
        hideMenu()
        restore = target.tile.focus
    }

    /** Ends a move and saves the new dock order. */
    fun drop() {
        editor.drop()?.let(actions::saveDock)
    }

    /** Home or Back on home: close the menu and finish any move. */
    fun reset() {
        if (editor.menu != null) hideMenu()
        drop()
    }

    fun restored() {
        restore = null
    }

    private fun hideMenu() {
        editor.closeMenu()
        dockFocus.hold(false)
    }
}

@Composable
fun rememberHomeEditHost(container: AppContainer, dockFocus: DockFocus): HomeEditHost {
    val context = LocalContext.current
    return remember(container, dockFocus, context) {
        val editor = HomeEditor()
        HomeEditHost(editor, EditActions(editor, container.prefs, container.scope, context), dockFocus)
    }
}

/** The open tile menu, if any; [dock] is the dock as shown. */
@Composable
fun HomeEditHost.Menu(dock: List<String>, dockSize: Int) {
    val target = editor.menu ?: return
    AppMenu(
        target,
        menuItems(target.inDock, dockSlot(dock, target.app.key, dockSize)),
        onPick = { pick(it, dock, dockSize) },
        onClose = { closeMenu() },
    )
}
