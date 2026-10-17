package com.kevin.glasslauncher.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalViewConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Click / long press from the Select key. A long press fires on whichever comes first: the hold
 * timeout, or a key repeat (Android flags the first repeat of a held key as a long press, and
 * `adb shell input keyevent --longpress` sends exactly that). The release after a long press does
 * not also click.
 */
class SelectPress(
    private val scope: CoroutineScope,
    private val timeoutMillis: Long,
    private val onClick: () -> Unit,
    private val onLongClick: () -> Unit,
) {
    private var pressed = false
    private var longFired = false
    private var timer: Job? = null

    fun down(repeatCount: Int) {
        if (repeatCount == 0) {
            pressed = true
            longFired = false
            timer?.cancel()
            timer = scope.launch {
                delay(timeoutMillis)
                fireLong()
            }
        } else if (pressed) {
            fireLong()
        }
    }

    fun up() {
        timer?.cancel()
        if (pressed && !longFired) onClick()
        pressed = false
    }

    private fun fireLong() {
        if (longFired) return
        longFired = true
        timer?.cancel()
        onLongClick()
    }
}

private fun Key.isSelect() = this == Key.DirectionCenter || this == Key.Enter || this == Key.NumPadEnter

/** Handles Select on this node with [SelectPress]; touch and accessibility clicks are left to the clickable below. */
@Composable
fun Modifier.selectPress(onClick: () -> Unit, onLongClick: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    val timeout = LocalViewConfiguration.current.longPressTimeoutMillis
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    val press = remember(scope, timeout) { SelectPress(scope, timeout, { click() }, { longClick() }) }
    return onPreviewKeyEvent { e ->
        if (!e.key.isSelect()) return@onPreviewKeyEvent false
        when (e.type) {
            KeyEventType.KeyDown -> press.down(e.nativeKeyEvent.repeatCount)
            KeyEventType.KeyUp -> press.up()
        }
        true
    }
}
