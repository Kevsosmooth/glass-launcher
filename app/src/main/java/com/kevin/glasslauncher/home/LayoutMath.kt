package com.kevin.glasslauncher.home

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** Skia's ambient shadow blurs elevation / 2 around the outline (SkShadowUtils AmbientBlurRadius). */
private const val SHADOW_SPREAD_PER_ELEVATION = 0.5f

/** The spot shadow sits below the tile: the light is above the screen. */
private const val SHADOW_DROP_PER_ELEVATION = 0.25f

/** A layer cameraDistance of 1 puts the camera 72 dp away (72 px per inch at density 1, or more). */
private const val CAMERA_DP_PER_UNIT = 72f

/** Everything that makes a focused tile bigger than its layout bounds. Lengths in dp. */
data class FocusGeometry(
    val tileWidth: Float,
    val tileHeight: Float,
    val focusScale: Float,
    val focusDamping: Float,
    val tiltDegrees: Float,
    val wiggleDegrees: Float,
    val cameraDistance: Float,
    val elevation: Float,
)

/** How far (dp) a focused tile, with its shadow, reaches past its unscaled bounds on each side. */
data class Footprint(val side: Float, val top: Float, val bottom: Float)

/**
 * Derived home spacing in dp. [columnGap] equals [dockPadSide], so dock tiles sit on the grid
 * columns while the slab reaches [dockPadSide] past the outer columns.
 */
data class HomeMetrics(
    val tileWidth: Float,
    val columnGap: Float,
    val rowGap: Float,
    val sidePadding: Float,
    val dockPadSide: Float,
    val dockPadTop: Float,
    val dockPadBottom: Float,
) {
    /** Screen edge to the dock slab. */
    val dockInset: Float get() = sidePadding - dockPadSide
}

/** Peak overshoot of an under-damped spring released from rest (0 when critically damped). */
fun springOvershoot(dampingRatio: Float): Float {
    if (dampingRatio >= 1f) return 0f
    return exp(-dampingRatio * PI.toFloat() / sqrt(1f - dampingRatio * dampingRatio))
}

/**
 * Worst case footprint: peak spring scale, then the larger of the D-pad tilt (perspective) and
 * the edit wiggle (they never run together: moving a tile keeps focus on it), then the shadow.
 */
fun focusFootprint(g: FocusGeometry): Footprint {
    val scale = 1f + (g.focusScale - 1f) * (1f + springOvershoot(g.focusDamping))
    val a = g.tileWidth * scale / 2
    val b = g.tileHeight * scale / 2
    val tilt = tiltGrowth(a, b, g.tiltDegrees, g.cameraDistance * CAMERA_DP_PER_UNIT)
    val wiggle = wiggleGrowth(a, b, g.wiggleDegrees)
    val turnX = max(tilt.first, wiggle.first)
    val turnY = max(tilt.second, wiggle.second)
    val spread = g.elevation * SHADOW_SPREAD_PER_ELEVATION
    val sideBase = a - g.tileWidth / 2 + turnX + spread
    val topBase = b - g.tileHeight / 2 + turnY + spread
    return Footprint(sideBase, topBase, topBase + g.elevation * SHADOW_DROP_PER_ELEVATION)
}

/** Growth (x, y) of a half-size a x b plate tilted toward a camera [distance] away, either axis. */
private fun tiltGrowth(a: Float, b: Float, degrees: Float, distance: Float): Pair<Float, Float> {
    val rad = degrees * PI.toFloat() / 180f
    val nearY = distance / (distance - a * sin(rad))
    val nearX = distance / (distance - b * sin(rad))
    val x = max(a * cos(rad) * nearY - a, a * nearX - a)
    val y = max(b * nearY - b, b * cos(rad) * nearX - b)
    return max(0f, x) to max(0f, y)
}

/** Growth (x, y) of the bounding box of a half-size a x b rectangle rotated in plane. */
private fun wiggleGrowth(a: Float, b: Float, degrees: Float): Pair<Float, Float> {
    val rad = degrees * PI.toFloat() / 180f
    val x = a * cos(rad) + b * sin(rad) - a
    val y = a * sin(rad) + b * cos(rad) - b
    return max(0f, x) to max(0f, y)
}

/**
 * Spacing from [footprint] plus [margin] of clear space, rounded up to whole dp. Every column
 * gap and dock edge leaves [margin] between a focused tile's footprint and its neighbour or the
 * slab edge; the leftover width is split into equal side paddings. Fails if nothing fits.
 */
fun homeMetrics(screenWidth: Float, columns: Int, tileWidth: Float, footprint: Footprint, margin: Float): HomeMetrics {
    val gap = ceil(footprint.side + margin)
    val rowGap = ceil(max(footprint.top, footprint.bottom) + margin)
    val sidePadding = (screenWidth - columns * tileWidth - (columns - 1) * gap) / 2
    val metrics = HomeMetrics(
        tileWidth = tileWidth,
        columnGap = gap,
        rowGap = rowGap,
        sidePadding = sidePadding,
        dockPadSide = gap,
        dockPadTop = ceil(footprint.top + margin),
        dockPadBottom = ceil(footprint.bottom + margin),
    )
    require(metrics.dockInset >= 0f) { "Dock slab does not fit $screenWidth dp: $metrics" }
    return metrics
}
