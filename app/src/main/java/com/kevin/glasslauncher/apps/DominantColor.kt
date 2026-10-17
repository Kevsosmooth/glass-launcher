package com.kevin.glasslauncher.apps

import kotlin.math.pow

/** Tiles stay at or below this luminance so white labels keep at least 4.5:1 contrast. */
const val MAX_TILE_LUMINANCE = 0.18

/**
 * Most common colour among opaque pixels (bucketed to 4 bits per channel), darkened until white
 * text is readable on it. Pure; [pixels] are ARGB.
 */
fun dominantColor(pixels: IntArray, fallback: Int): Int {
    val counts = HashMap<Int, Int>()
    for (p in pixels) {
        if ((p ushr 24) < 128) continue
        val bucket = (((p shr 20) and 0xF) shl 8) or (((p shr 12) and 0xF) shl 4) or ((p shr 4) and 0xF)
        counts[bucket] = (counts[bucket] ?: 0) + 1
    }
    val best = counts.maxByOrNull { it.value }?.key ?: return fallback
    var r = ((best shr 8) and 0xF) * 17
    var g = ((best shr 4) and 0xF) * 17
    var b = (best and 0xF) * 17
    var color = rgb(r, g, b)
    while (relativeLuminance(color) > MAX_TILE_LUMINANCE) {
        r = r * 9 / 10; g = g * 9 / 10; b = b * 9 / 10
        color = rgb(r, g, b)
    }
    return color
}

/** WCAG relative luminance of an ARGB colour (alpha ignored). */
fun relativeLuminance(color: Int): Double {
    fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel((color shr 16) and 0xFF) +
        0.7152 * channel((color shr 8) and 0xFF) +
        0.0722 * channel(color and 0xFF)
}

private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
