package com.kevin.glasslauncher.motion

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

enum class Direction { None, Left, Right, Up, Down }

/** Remembers the last D-pad direction (plain field, not state: read once when focus lands). */
class DirectionTracker {
    var last: Direction = Direction.None

    /** Tilt for the newly focused tile: lean toward the direction of travel. */
    fun tiltDegrees(): Pair<Float, Float> = when (last) {
        Direction.Right -> 0f to Motion.TILT_DEGREES
        Direction.Left -> 0f to -Motion.TILT_DEGREES
        Direction.Down -> -Motion.TILT_DEGREES to 0f
        Direction.Up -> Motion.TILT_DEGREES to 0f
        Direction.None -> 0f to 0f
    }
}

fun Direction.toFocusDirection(): FocusDirection = when (this) {
    Direction.Left -> FocusDirection.Left
    Direction.Right -> FocusDirection.Right
    Direction.Up -> FocusDirection.Up
    Direction.Down, Direction.None -> FocusDirection.Down
}

fun Modifier.trackDirection(tracker: DirectionTracker): Modifier = onPreviewKeyEvent { e ->
    if (e.type == KeyEventType.KeyDown) {
        when (e.key) {
            Key.DirectionLeft -> tracker.last = Direction.Left
            Key.DirectionRight -> tracker.last = Direction.Right
            Key.DirectionUp -> tracker.last = Direction.Up
            Key.DirectionDown -> tracker.last = Direction.Down
        }
    }
    false
}
