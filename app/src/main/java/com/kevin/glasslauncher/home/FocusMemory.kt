package com.kevin.glasslauncher.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Which home tile or control a panel was opened from, so closing it puts focus back where the user was
 * (focusRestorer does not reach tiles nested in lazy rows). Saved when the panel opens: once the
 * focused node goes away, Compose may hand focus to another tile on its own.
 */
class FocusMemory {
    private var last: FocusRequester? = null
    private var saved: FocusRequester? = null

    fun focused(tile: FocusRequester) {
        last = tile
    }

    /** A tile left the screen; forget it. */
    fun gone(tile: FocusRequester) {
        if (last === tile) last = null
        if (saved === tile) saved = null
    }

    /** A panel opened: keep the tile focus came from. */
    fun save() {
        saved = last
    }

    /** Home closed the panel; the dock takes focus instead. */
    fun discard() {
        saved = null
    }

    /** Focuses the saved tile; false if there is none or it would not take focus. */
    fun restore(): Boolean {
        val tile = saved ?: return false
        saved = null
        return tile.requestFocus()
    }
}

/** Marks the home control [focus] belongs to, so closing a panel it opened puts focus back on it. */
fun Modifier.rememberedBy(memory: FocusMemory, focus: FocusRequester): Modifier =
    onFocusChanged { if (it.isFocused) memory.focused(focus) }.focusRequester(focus)
