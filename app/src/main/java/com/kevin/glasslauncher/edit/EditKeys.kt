package com.kevin.glasslauncher.edit

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

private fun KeyEvent.isSelect() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

/**
 * Swallows Select until it is pressed afresh. A long press opens the menu while Select is still
 * held; its key repeats and its release must not also pick the first menu item.
 */
class SelectGuard {
    private var armed = false

    /** True if [event] must be swallowed. */
    fun swallow(event: KeyEvent): Boolean {
        if (!event.isSelect()) return false
        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) armed = true
        return !armed
    }
}

/** Ignores a held-over Select (see [SelectGuard]) for everything inside. */
fun Modifier.selectGuard(guard: SelectGuard): Modifier = onPreviewKeyEvent { guard.swallow(it) }

/**
 * Move mode on the dock: Left/Right call [onStep] (-1/+1), Up/Down are swallowed so focus stays
 * on the moving tile, and a fresh Select drops it. Does nothing while [active] is false.
 */
fun Modifier.moveKeys(active: Boolean, guard: SelectGuard, onStep: (Int) -> Unit, onDrop: () -> Unit): Modifier =
    if (!active) {
        this
    } else {
        onPreviewKeyEvent { e ->
            val down = e.type == KeyEventType.KeyDown
            when {
                e.isSelect() -> {
                    if (!guard.swallow(e) && !down) onDrop()
                    true
                }
                e.key == Key.DirectionLeft -> true.also { if (down) onStep(-1) }
                e.key == Key.DirectionRight -> true.also { if (down) onStep(1) }
                else -> e.key == Key.DirectionUp || e.key == Key.DirectionDown
            }
        }
    }
