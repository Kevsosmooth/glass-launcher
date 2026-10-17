package com.kevin.glasslauncher.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Top-left corner for a [menu]-sized popup beside [tile]: to its right when that fits inside
 * [screen] minus [edge], otherwise to its left; top-aligned with the tile, clamped on screen.
 */
fun menuPosition(tile: Rect, menu: Size, screen: Size, gap: Float, edge: Float): Offset {
    val right = tile.right + gap
    val x = if (right + menu.width <= screen.width - edge) right else tile.left - gap - menu.width
    val maxY = (screen.height - edge - menu.height).coerceAtLeast(edge)
    return Offset(x.coerceIn(edge, (screen.width - edge - menu.width).coerceAtLeast(edge)), tile.top.coerceIn(edge, maxY))
}
