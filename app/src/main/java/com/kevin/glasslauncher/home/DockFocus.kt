package com.kevin.glasslauncher.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kevin.glasslauncher.apps.AppEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

/** Fast D-pad sweeps across the dock settle before the shelf and backdrop follow. */
const val SHELF_DEBOUNCE_MS = 150L

/**
 * Dock focus bookkeeping. [wantFirst] asks the dock to focus its first tile (start, Home, Back,
 * after a tile left the dock). [shelfApp] is the focused dock app, null when focus is elsewhere,
 * updated [SHELF_DEBOUNCE_MS] after focus stops moving; raw reports never recompose anything.
 */
@Stable
class DockFocus {
    var wantFirst by mutableStateOf(true)
    var shelfApp by mutableStateOf<AppEntry?>(null)
        private set
    private val focused = MutableStateFlow<AppEntry?>(null)
    private var held = false

    /** A dock tile gained focus ([app]) or focus left the dock (null). */
    fun report(app: AppEntry?) {
        if (!held) focused.value = app
    }

    /** While a tile menu is open the shelf keeps the app the menu was opened from. */
    fun hold(on: Boolean) {
        held = on
    }

    /** Runs for as long as home is shown. */
    suspend fun debounce() {
        focused.collectLatest {
            delay(SHELF_DEBOUNCE_MS)
            shelfApp = it
        }
    }
}
