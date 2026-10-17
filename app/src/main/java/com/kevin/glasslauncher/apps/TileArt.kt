package com.kevin.glasslauncher.apps

import androidx.compose.ui.graphics.ImageBitmap

/** Tile size at the 960 dp-wide layout (6 columns); art is rendered once at this size. */
object TileSize {
    const val WIDTH_DP = 124f
    const val HEIGHT_DP = WIDTH_DP * 9f / 16f
}

/** Shelf card / backdrop art size at the 960 dp-wide layout. */
object LargeArtSize {
    const val WIDTH_DP = 256f
    const val HEIGHT_DP = WIDTH_DP * 9f / 16f
}

/** Pre-rendered tile picture: an app banner, or an icon centred on a tint ([showLabel] true). */
class TileArt(val image: ImageBitmap, val showLabel: Boolean)

/**
 * Shelf-size art: [image] is the sharp banner (or icon tile) at [LargeArtSize]; [soft] is a tiny
 * blurred copy that the app backdrop stretches over the screen.
 */
class LargeArt(val image: ImageBitmap, val soft: ImageBitmap)
