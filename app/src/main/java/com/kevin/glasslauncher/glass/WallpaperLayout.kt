package com.kevin.glasslauncher.glass

import kotlin.math.hypot
import kotlin.random.Random

/** One soft colour light. [x], [y] are fractions of the width/height, [radius] a fraction of the width. */
data class Blob(val x: Float, val y: Float, val radius: Float, val color: Long)

/**
 * Deterministic blob layout for a wallpaper: the same id always paints the same picture.
 * Best-candidate sampling spreads the lights over the screen instead of letting them clump.
 */
object WallpaperLayout {
    const val MIN_POS = 0.06f
    const val MAX_POS = 0.94f
    const val MIN_RADIUS = 0.30f
    const val MAX_RADIUS = 0.44f
    private const val CANDIDATES = 16

    // Distances are measured in width units, so y is scaled by the 16:9 screen aspect.
    private const val ASPECT_Y = 9f / 16f

    fun blobs(info: WallpaperInfo.Painted): List<Blob> {
        val random = Random(seedOf(info.id))
        val placed = ArrayList<Blob>(info.colors.size)
        for (color in info.colors) {
            var bestX = 0f
            var bestY = 0f
            var bestDist = -1f
            repeat(CANDIDATES) {
                val x = random.between(MIN_POS, MAX_POS)
                val y = random.between(MIN_POS, MAX_POS)
                val d = placed.minOfOrNull { distance(it.x, it.y, x, y) } ?: Float.MAX_VALUE
                if (d > bestDist) {
                    bestDist = d
                    bestX = x
                    bestY = y
                }
            }
            placed += Blob(bestX, bestY, random.between(MIN_RADIUS, MAX_RADIUS), color)
        }
        return placed
    }

    /** Screen-space distance between two blob centres, in width units. */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        hypot(x1 - x2, (y1 - y2) * ASPECT_Y)

    // String.hashCode is specified by the language, so the seed is stable across devices and runs.
    private fun seedOf(id: String): Int = id.hashCode()

    private fun Random.between(min: Float, max: Float): Float = min + nextFloat() * (max - min)
}
