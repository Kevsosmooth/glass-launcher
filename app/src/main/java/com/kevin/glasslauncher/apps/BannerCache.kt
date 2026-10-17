package com.kevin.glasslauncher.apps

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.LruCache
import androidx.compose.ui.graphics.asImageBitmap
import com.kevin.glasslauncher.glass.BoxBlur
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Tiles: 12 MB. Large art: 3 entries of ~0.6 MB at 2x density. Total stays under 16 MB.
private const val MAX_BYTES = 12 * 1024 * 1024
private const val LARGE_ENTRIES = 3
private const val SOFT_DOWNSCALE = 8
private const val SOFT_BLUR_RADIUS = 2
private const val SAMPLE_SIZE = 24

// Shelf art is ~512 px wide: most banners are 320 px at xhdpi, so ask for the app's sharper
// xxhdpi (480 px) asset when it ships one. Much larger assets would alias when scaled down.
private const val LARGE_BANNER_DENSITY = DisplayMetrics.DENSITY_XXHIGH
private const val ICON_HEIGHT_FRACTION = 0.5f
private const val ICON_TOP_FRACTION = 0.12f
private val FallbackTints = intArrayOf(0xFF2B3A67.toInt(), 0xFF5B2A4E.toInt(), 0xFF1F4D45.toInt(), 0xFF4A3B1C.toInt())

/**
 * LRU of tile-sized art plus a small LRU of shelf-size art ([loadLarge]), rendered off the main
 * thread and kept as GPU (hardware) bitmaps.
 */
class BannerCache(context: Context) {
    private val pm = context.packageManager
    private val widthPx: Int
    private val heightPx: Int
    private val largeWidthPx: Int
    private val largeHeightPx: Int
    private val loader = Dispatchers.Default.limitedParallelism(2)
    private val cache = object : LruCache<String, TileArt>(MAX_BYTES) {
        override fun sizeOf(key: String, value: TileArt) = value.image.width * value.image.height * 4
    }
    private val large = LruCache<String, LargeArt>(LARGE_ENTRIES)

    init {
        val density = context.resources.displayMetrics.density
        widthPx = (TileSize.WIDTH_DP * density).roundToInt()
        heightPx = (TileSize.HEIGHT_DP * density).roundToInt()
        largeWidthPx = (LargeArtSize.WIDTH_DP * density).roundToInt()
        largeHeightPx = (LargeArtSize.HEIGHT_DP * density).roundToInt()
    }

    fun peek(app: AppEntry): TileArt? = cache.get(app.key)

    suspend fun load(app: AppEntry): TileArt = cache.get(app.key) ?: withContext(loader) {
        val (bitmap, showLabel) = render(app, widthPx, heightPx)
        TileArt(toGpu(bitmap).asImageBitmap(), showLabel).also { cache.put(app.key, it) }
    }

    fun peekLarge(app: AppEntry): LargeArt? = large.get(app.key)

    /** Shelf-size art for the top shelf card and the app backdrop; only the last few are kept. */
    suspend fun loadLarge(app: AppEntry): LargeArt = large.get(app.key) ?: withContext(loader) {
        val bitmap = render(app, largeWidthPx, largeHeightPx, LARGE_BANNER_DENSITY).first
        val soft = soften(bitmap)
        LargeArt(toGpu(bitmap).asImageBitmap(), soft.asImageBitmap()).also { large.put(app.key, it) }
    }

    fun evict(packageName: String) {
        val prefix = "$packageName/"
        cache.snapshot().keys.filter { it.startsWith(prefix) }.forEach(cache::remove)
        large.snapshot().keys.filter { it.startsWith(prefix) }.forEach(large::remove)
    }

    fun clear() {
        cache.evictAll()
        large.evictAll()
    }

    /** The app's banner (no label needed) or an icon on its dominant tint (label shown: true). */
    private fun render(app: AppEntry, width: Int, height: Int, density: Int = 0): Pair<Bitmap, Boolean> {
        val component = ComponentName(app.packageName, app.activityName)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val banner = if (app.isLeanback) loadBanner(component, density) else null
        val showLabel = if (banner != null) {
            banner.setBounds(0, 0, width, height)
            banner.draw(canvas)
            false
        } else {
            drawIconTile(canvas, loadIcon(component), app, width, height)
            true
        }
        return bitmap to showLabel
    }

    private fun toGpu(bitmap: Bitmap): Bitmap {
        val gpu = bitmap.copy(Bitmap.Config.HARDWARE, false) ?: return bitmap
        bitmap.recycle()
        return gpu
    }

    /** A tiny blurred copy: stretched over the screen it reads as a soft wash of the app's colours. */
    private fun soften(source: Bitmap): Bitmap {
        val w = source.width / SOFT_DOWNSCALE
        val h = source.height / SOFT_DOWNSCALE
        val small = Bitmap.createScaledBitmap(source, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        small.setPixels(BoxBlur.blur(px, w, h, SOFT_BLUR_RADIUS), 0, w, 0, 0, w, h)
        return small
    }

    private fun drawIconTile(canvas: Canvas, icon: Drawable?, app: AppEntry, width: Int, height: Int) {
        val fallback = FallbackTints[Math.floorMod(app.packageName.hashCode(), FallbackTints.size)]
        canvas.drawColor(if (icon != null) dominantColor(sample(icon), fallback) else fallback)
        if (icon == null) return
        val size = (height * ICON_HEIGHT_FRACTION).roundToInt()
        val left = (width - size) / 2
        val top = (height * ICON_TOP_FRACTION).roundToInt()
        icon.setBounds(left, top, left + size, top + size)
        icon.draw(canvas)
    }

    private fun sample(icon: Drawable): IntArray {
        val small = Bitmap.createBitmap(SAMPLE_SIZE, SAMPLE_SIZE, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        icon.draw(Canvas(small))
        val px = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
        small.getPixels(px, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
        small.recycle()
        return px
    }

    /** The banner at the screen's density, or the app's closest one to [density] when it is set. */
    private fun loadBanner(component: ComponentName, density: Int = 0): Drawable? = try {
        (if (density != 0) bannerForDensity(component, density) else null)
            ?: pm.getActivityBanner(component)
            ?: pm.getApplicationBanner(component.packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun bannerForDensity(component: ComponentName, density: Int): Drawable? {
        val activity = pm.getActivityInfo(component, 0)
        val app = activity.applicationInfo
        val id = activity.banner.takeIf { it != 0 } ?: app.banner.takeIf { it != 0 } ?: return null
        return try {
            pm.getResourcesForApplication(app).getDrawableForDensity(id, density, null)
        } catch (e: Resources.NotFoundException) {
            null
        }
    }

    private fun loadIcon(component: ComponentName): Drawable? = try {
        pm.getActivityIcon(component)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
