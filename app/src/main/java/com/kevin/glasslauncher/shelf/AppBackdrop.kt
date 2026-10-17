package com.kevin.glasslauncher.shelf

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.apps.LargeArt
import com.kevin.glasslauncher.glass.CrossfadeLayers
import com.kevin.glasslauncher.glass.GlassUnderlay
import com.kevin.glasslauncher.glass.rememberCrossfade
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Dark enough that white text on top keeps its contrast over the brightest banner colours.
private val Scrim = Color(0x99000000)

// The art is solid down to here, then fades to nothing at the bottom edge of the bounds.
private const val FADE_START = 0.45f

// Focus skimming across the dock should not render art for every tile it passes.
private const val SKIM_DELAY_MILLIS = 120L

/**
 * The backdrop's current art and where it sits on screen. Glass surfaces draw it through
 * [com.kevin.glasslauncher.glass.LocalGlass] so they blur the backdrop, not the wallpaper under it.
 */
@Stable
class BackdropUnderlay : GlassUnderlay {
    internal var layers by mutableStateOf<CrossfadeLayers<LargeArt>?>(null)
    internal var bounds by mutableStateOf(Rect.Zero)

    override fun DrawScope.drawInRoot() {
        val l = layers ?: return
        translate(bounds.left, bounds.top) { drawBackdrop(l, bounds.size) }
    }
}

/**
 * tvOS-style backdrop: the focused dock app's art, large and soft, crossfading as focus moves.
 * Drawn above the wallpaper, below everything else. It fills [modifier]'s bounds and fades to
 * transparent at their bottom edge: end the bounds at the dock's top edge so the dock shows no
 * seam. null [app] fades it out. Glass surfaces show it through [underlay].
 */
@Composable
fun AppBackdrop(app: AppEntry?, banners: BannerCache, underlay: BackdropUnderlay, modifier: Modifier = Modifier) {
    val art by produceState(app?.let(banners::peekLarge), app) {
        value = app?.let { loadLargeArt(it, banners) }
    }
    val layers = rememberCrossfade(art)
    SideEffect { underlay.layers = layers }
    Spacer(
        modifier
            .onPlaced { underlay.bounds = it.boundsInRoot() }
            .drawBehind { drawBackdrop(layers, size) },
    )
}

/**
 * Both crossfade layers, darkened, fading out over the bottom of [size]. In its own layer so the
 * scrim (SrcAtop) and the fade mask (DstIn) only touch the art, not what is under it.
 */
private fun DrawScope.drawBackdrop(layers: CrossfadeLayers<LargeArt>, size: Size) {
    val previous = layers.previous
    val current = layers.current
    if (previous == null && current == null) return
    val area = Rect(Offset.Zero, size)
    drawIntoCanvas { it.saveLayer(area, Paint()) }
    previous?.let { drawCover(it.soft, layers.previousAlpha, size) }
    current?.let { drawCover(it.soft, layers.currentAlpha, size) }
    drawRect(Scrim, size = size, blendMode = BlendMode.SrcAtop)
    val mask = Brush.verticalGradient(FADE_START to Color.Black, 1f to Color.Transparent, endY = size.height)
    drawRect(mask, size = size, blendMode = BlendMode.DstIn)
    drawIntoCanvas { it.restore() }
}

/** Scales [image] to cover [size] (centre crop), like a full-bleed photo. */
private fun DrawScope.drawCover(image: ImageBitmap, alpha: Float, size: Size) {
    if (alpha <= 0f) return
    val scale = maxOf(size.width / image.width, size.height / image.height)
    val srcW = (size.width / scale).roundToInt().coerceIn(1, image.width)
    val srcH = (size.height / scale).roundToInt().coerceIn(1, image.height)
    drawImage(
        image,
        srcOffset = IntOffset((image.width - srcW) / 2, (image.height - srcH) / 2),
        srcSize = IntSize(srcW, srcH),
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
}

/** Cached art at once; otherwise wait out quick focus skims before rendering. */
internal suspend fun loadLargeArt(app: AppEntry, banners: BannerCache): LargeArt =
    banners.peekLarge(app) ?: run {
        delay(SKIM_DELAY_MILLIS)
        banners.loadLarge(app)
    }
