package com.kevin.glasslauncher.home

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.apps.TileSize
import com.kevin.glasslauncher.motion.Motion

/**
 * Home dimensions at 960x540 dp, derived in [LayoutMath][homeMetrics] so a focused tile can never
 * cover a neighbour or poke out of the dock. With the current Motion tokens a 124 x 69.75 dp tile
 * peaks at 1.157x (1.15 plus spring overshoot) and, with its 16 dp shadow, reaches 18.8 dp past
 * each side, 15.3 dp above and 19.3 dp below. Adding the 6 dp margin gives a 25 dp column gap,
 * 26 dp row gap, 25/22/26 dp dock padding (side/top/bottom) and 45.5 dp grid side padding; the
 * dock slab starts 20.5 dp from the screen edge and its columns line up with the grid's.
 */
object HomeLayout {
    const val COLUMNS = 6
    const val SCREEN_WIDTH_DP = 960f
    const val FOCUS_MARGIN_DP = 6f

    /** What a focused tile can grow to; tests check the layout against it. */
    val Focus = FocusGeometry(
        tileWidth = TileSize.WIDTH_DP,
        tileHeight = TileSize.HEIGHT_DP,
        focusScale = Motion.FOCUS_SCALE,
        focusDamping = Motion.focusSpring.dampingRatio,
        tiltDegrees = Motion.TILT_DEGREES,
        wiggleDegrees = Motion.WIGGLE_DEGREES,
        cameraDistance = Motion.CAMERA_DISTANCE,
        elevation = Motion.LIFT_ELEVATION.value,
    )
    val Footprint = focusFootprint(Focus)
    val Metrics = homeMetrics(SCREEN_WIDTH_DP, COLUMNS, TileSize.WIDTH_DP, Footprint, FOCUS_MARGIN_DP)

    val SidePadding = Metrics.sidePadding.dp
    val TileGap = Metrics.columnGap.dp
    val RowGap = Metrics.rowGap.dp
    val DockPadSide = Metrics.dockPadSide.dp
    val DockPadTop = Metrics.dockPadTop.dp
    val DockPadBottom = Metrics.dockPadBottom.dp

    /** Top item height with the top shelf (pill row + shelf), and with just the pill. */
    val TopArea = 236.dp
    val TopAreaCompact = 120.dp
    val BottomPadding = 48.dp
    val TileCorner = 12.dp
    val TileShape = RoundedCornerShape(TileCorner)

    /** Concentric with the tile corners. */
    val DockCorner = TileCorner + DockPadTop
    val StatusInset = 28.dp
}
