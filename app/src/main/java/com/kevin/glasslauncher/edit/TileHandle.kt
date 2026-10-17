package com.kevin.glasslauncher.edit

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect

/** A long-pressed tile: its unscaled bounds in the root, and how to give it focus back. */
class TileHandle(val bounds: Rect, val focus: FocusRequester)
