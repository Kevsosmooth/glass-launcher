package com.kevin.glasslauncher.glass

import kotlin.math.roundToInt

data class SrcRect(val x: Int, val y: Int, val w: Int, val h: Int)

/** Region of the downscaled blurred wallpaper that sits behind a surface at these screen bounds. */
fun glassSourceRect(
    left: Float, top: Float, width: Float, height: Float,
    downscale: Int, bmpW: Int, bmpH: Int,
): SrcRect {
    val x = (left / downscale).toInt().coerceIn(0, bmpW - 1)
    val y = (top / downscale).toInt().coerceIn(0, bmpH - 1)
    val w = (width / downscale).roundToInt().coerceIn(1, bmpW - x)
    val h = (height / downscale).roundToInt().coerceIn(1, bmpH - y)
    return SrcRect(x, y, w, h)
}
