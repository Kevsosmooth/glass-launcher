package com.kevin.glasslauncher.glass

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.data.Look
import kotlin.math.roundToInt

// Liquid Glass: a light, vivid frost with a bright rim all the way round (strongest on top, a
// softer catch-light along the bottom), specular glints where light hits the top-left and
// bottom-right corners, and a faint inner glow, so edges read as curved glass.
private val GlassShade = Color(0x2E000000)
private val GlassTint = Color(0x29FFFFFF)
private val RimTop = Color(0xB3FFFFFF)
private val RimSide = Color(0x1AFFFFFF)
private val RimBottom = Color(0x59FFFFFF)
private val GlowTop = Color(0x29FFFFFF)
private val GlowBottom = Color(0x14FFFFFF)
private val GlowWidth = 8.dp
private val GlintLead = Color(0xE6FFFFFF)
private val GlintTrail = Color(0x99FFFFFF)
private val Vibrance = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.35f) })
private val SolidPanel = Color(0xFF1E2129)
private val SolidEdge = Color(0x33FFFFFF)
private val SoftShadow = Color(0x26000000)
// Full outline of a solid-look control sitting on another surface; the top rim is drawn over it.
private val NestedEdge = Color(0x33FFFFFF)
private val ShadowDrop = 4.dp

private fun glowBrush(top: Float, bottom: Float) = Brush.verticalGradient(
    0f to GlowTop, 0.3f to Color.Transparent, 0.75f to Color.Transparent, 1f to GlowBottom,
    startY = top, endY = bottom,
)

private fun rimBrush(top: Float, bottom: Float) = Brush.verticalGradient(
    0f to RimTop, 0.5f to RimSide, 1f to RimBottom,
    startY = top, endY = bottom,
)

/** A rim glint fading out from the corner at [corner]; reaches [reach] px along the edges. */
private fun glintBrush(corner: Offset, reach: Float, color: Color) =
    Brush.radialGradient(0f to color, 1f to Color.Transparent, center = corner, radius = reach)

/**
 * A layer drawn over the wallpaper that glass surfaces must show too (the app backdrop), so they
 * blur what is really behind them. Draws in root coordinates.
 */
fun interface GlassUnderlay {
    fun DrawScope.drawInRoot()
}

/**
 * What every glass surface needs: the blurred wallpaper (null while it loads), the Look, and any
 * [underlay] between the wallpaper and the surface.
 */
class GlassContext(val wallpaper: Wallpaper?, val look: Look, val underlay: GlassUnderlay? = null)

val LocalGlass = staticCompositionLocalOf { GlassContext(null, Look.Glass) }

/**
 * Full-screen sharp wallpaper, crossfading when it changes; the window background shows until
 * the first one has loaded.
 */
@Composable
fun WallpaperBackground(glass: GlassContext, modifier: Modifier = Modifier) {
    val layers = rememberCrossfade(glass.wallpaper)
    Spacer(
        modifier.fillMaxSize().drawBehind {
            val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
            layers.previous?.let { drawWallpaper(it, dst, layers.previousAlpha) }
            layers.current?.let { drawWallpaper(it, dst, layers.currentAlpha) }
        },
    )
}

private fun DrawScope.drawWallpaper(wp: Wallpaper, dst: IntSize, alpha: Float) {
    if (alpha <= 0f) return
    drawImage(wp.sharp, dstSize = dst, alpha = alpha, filterQuality = FilterQuality.Low)
}

/**
 * Frosted glass without RenderEffect: draws the region of the pre-blurred wallpaper that sits
 * behind this surface with a little extra colour, then shade, tint, an inner glow and a bright
 * rim all the way round. [outsetX]/[outsetY] grow the slab beyond the layout bounds (room for
 * focused tiles). Solid look: opaque dark panel with a 1 dp top edge. [nested] is for buttons and
 * tiles on a glass panel: no drop shadow (it reads as the bottom being cut off on small shapes).
 */
fun Modifier.glass(corner: Dp, outsetX: Dp = 0.dp, outsetY: Dp = 0.dp, nested: Boolean = false): Modifier =
    this then GlassElement(corner, outsetX, outsetY, nested)

private data class GlassElement(val corner: Dp, val outsetX: Dp, val outsetY: Dp, val nested: Boolean) :
    ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(corner, outsetX, outsetY, nested)

    override fun update(node: GlassNode) {
        node.corner = corner
        node.outsetX = outsetX
        node.outsetY = outsetY
        node.nested = nested
        node.invalidateDraw()
    }
}

private class GlassNode(var corner: Dp, var outsetX: Dp, var outsetY: Dp, var nested: Boolean) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode {
    private var origin = Offset.Zero
    private val path = Path()

    // Position changes (scrolling) only re-draw; nothing recomposes.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val p = coordinates.positionInRoot()
        if (p != origin) {
            origin = p
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val glass = currentValueOf(LocalGlass)
        val ox = outsetX.toPx()
        val oy = outsetY.toPx()
        val slab = Size(size.width + 2 * ox, size.height + 2 * oy)
        val topLeft = Offset(-ox, -oy)
        val radius = CornerRadius(corner.toPx())
        if (!nested) drawRoundRect(SoftShadow, topLeft + Offset(0f, ShadowDrop.toPx()), slab, radius)
        path.reset()
        path.addRoundRect(RoundRect(-ox, -oy, size.width + ox, size.height + oy, radius))
        val wp = glass.wallpaper?.takeIf { glass.look == Look.Glass }
        clipPath(path) {
            if (wp != null) {
                val img = wp.blurred
                val src = glassSourceRect(origin.x - ox, origin.y - oy, slab.width, slab.height, wp.downscale, img.width, img.height)
                drawImage(
                    img, IntOffset(src.x, src.y), IntSize(src.w, src.h),
                    dstOffset = IntOffset(-ox.roundToInt(), -oy.roundToInt()),
                    dstSize = IntSize(slab.width.roundToInt(), slab.height.roundToInt()),
                    filterQuality = FilterQuality.Low,
                    colorFilter = Vibrance,
                )
                glass.underlay?.let { layer -> translate(-origin.x, -origin.y) { with(layer) { drawInRoot() } } }
                drawRect(GlassShade, topLeft, slab)
                drawRect(GlassTint, topLeft, slab)
            } else {
                drawRect(SolidPanel, topLeft, slab)
            }
            // Strokes are centred on the outline; the clip keeps their inner half.
            val edge = 1.dp.toPx()
            if (wp != null) {
                val top = -oy
                val bottom = size.height + oy
                drawPath(path, glowBrush(top, bottom), style = Stroke(GlowWidth.toPx()))
                drawPath(path, rimBrush(top, bottom), style = Stroke(2 * edge))
                val reach = minOf(size.width + 2 * ox, bottom - top)
                drawPath(path, glintBrush(Offset(-ox, top), reach, GlintLead), style = Stroke(2 * edge))
                drawPath(path, glintBrush(Offset(size.width + ox, bottom), reach * 0.8f, GlintTrail), style = Stroke(2 * edge))
            } else {
                // Solid look: a 1 dp rim along the top edge and around the top corners only.
                if (nested) drawPath(path, NestedEdge, style = Stroke(2 * edge))
                clipRect(-ox, -oy, size.width + ox, -oy + maxOf(radius.y, edge)) {
                    drawPath(path, SolidEdge, style = Stroke(2 * edge))
                }
            }
        }
        drawContent()
    }
}
