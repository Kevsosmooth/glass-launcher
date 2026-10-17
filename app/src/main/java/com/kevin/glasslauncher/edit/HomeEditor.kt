package com.kevin.glasslauncher.edit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kevin.glasslauncher.apps.AppEntry

/**
 * Edit-mode state: the open tile menu and the dock tile being moved. While moving, the dock shows
 * [order]; after a drop it keeps showing it until the saved favourites come back from prefs, so
 * the dock never flashes the old order.
 */
@Stable
class HomeEditor {
    var menu by mutableStateOf<MenuTarget?>(null)
        private set
    var moving by mutableStateOf<String?>(null)
        private set
    private var order by mutableStateOf<List<String>?>(null)

    fun openMenu(target: MenuTarget) {
        menu = target
    }

    fun closeMenu() {
        menu = null
    }

    fun startMove(key: String, dock: List<String>) {
        order = dock
        moving = key
    }

    /** Swaps the moving tile with its neighbour ([delta] -1 left, +1 right). */
    fun step(delta: Int) {
        val key = moving ?: return
        order = order?.let { moveInDock(it, key, delta) }
    }

    /** Ends the move; returns the order to save, or null if nothing was moving. */
    fun drop(): List<String>? {
        if (moving == null) return null
        moving = null
        return order
    }

    /**
     * Call when the dock from prefs changes: forgets [order] once the saved one has arrived, or
     * once the dock holds other apps (something else changed it, so the pending order is moot).
     */
    fun sync(dock: List<String>) {
        val pending = order ?: return
        if (moving == null && (pending == dock || pending.toSet() != dock.toSet())) order = null
    }

    /** [dock] in the edited order; apps added or removed meanwhile are kept or dropped. */
    fun arrange(dock: List<AppEntry>): List<AppEntry> {
        val keys = order ?: return dock
        val byKey = dock.associateBy { it.key }
        return keys.mapNotNull { byKey[it] } + dock.filter { it.key !in keys }
    }
}
