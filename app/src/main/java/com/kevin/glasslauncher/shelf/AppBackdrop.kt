package com.kevin.glasslauncher.shelf

import android.graphics.BitmapShader
import android.graphics.Color as AndroidColor
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint as AndroidPaint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
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
private val ScrimFilter = PorterDuffColorFilter(Scrim.toArgb(), PorterDuff.Mode.SRC_ATOP)

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
 * Both crossfade layers, darkened, fading out over the bottom of [size]. Each layer is one shader
 * pass (art masked by the fade, then the scrim as a colour filter) instead of an offscreen layer:
 * a layer per frame, twice with glass on, is what weak TV GPUs (Fire TV) choke on.
 */
private fun DrawScope.drawBackdrop(layers: CrossfadeLayers<LargeArt>, size: Size) {
    if (size.width <= 0f || size.height <= 0f) return
    layers.previous?.let { drawCover(it.soft, layers.previousAlpha, size) }
    layers.current?.let { drawCover(it.soft, layers.currentAlpha, size) }
}

/** [image] scaled to cover [size] (centre crop), darkened by [Scrim] and faded out toward the bottom. */
private fun DrawScope.drawCover(image: ImageBitmap, alpha: Float, size: Size) {
    if (alpha <= 0f) return
    val bitmap = image.asAndroidBitmap()
    val scale = maxOf(size.width / bitmap.width, size.height / bitmap.height)
    val art = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        setLocalMatrix(
            Matrix().apply {
                setScale(scale, scale)
                postTranslate((size.width - bitmap.width * scale) / 2, (size.height - bitmap.height * scale) / 2)
            },
        )
    }
    val fade = LinearGradient(
        0f, 0f, 0f, size.height,
        intArrayOf(AndroidColor.BLACK, AndroidColor.BLACK, AndroidColor.TRANSPARENT),
        floatArrayOf(0f, FADE_START, 1f),
        Shader.TileMode.CLAMP,
    )
    val paint = AndroidPaint(AndroidPaint.FILTER_BITMAP_FLAG).apply {
        shader = ComposeShader(art, fade, PorterDuff.Mode.DST_IN)
        colorFilter = ScrimFilter
        this.alpha = (alpha * 255).roundToInt()
    }
    drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint) }
}

/** Cached art at once; otherwise wait out quick focus skims before rendering. */
internal suspend fun loadLargeArt(app: AppEntry, banners: BannerCache): LargeArt =
    banners.peekLarge(app) ?: run {
        delay(SKIM_DELAY_MILLIS)
        banners.loadLarge(app)
    }
