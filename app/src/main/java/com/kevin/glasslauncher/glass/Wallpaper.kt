package com.kevin.glasslauncher.glass

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.IOException

/** A painted wallpaper. [sharp] fills the screen (drawn scaled); [blurred] is 1/[downscale] of the screen size. */
class Wallpaper(val id: String, val sharp: ImageBitmap, val blurred: ImageBitmap, val downscale: Int)

/**
 * Makes built-in wallpapers: paints soft colour lights on a dark base, or decodes a bundled photo.
 * Call off the main thread.
 */
object WallpaperFactory {
    const val DOWNSCALE = 8
    private const val BLUR_RADIUS = 6

    // Smooth gradients lose nothing at half size, and it saves 3/4 of the memory.
    private const val SHARP_SCALE = 2

    // Gaussian-like falloff: a bright core that fades out without a visible rim.
    private val FalloffStops = floatArrayOf(0f, 0.35f, 0.7f, 1f)
    private val FalloffAlpha = intArrayOf(0xE6, 0x8C, 0x2E, 0x00)
    private const val BOTTOM_SHADE = 0x59000000

    // Photos are brighter than the painted fields: dim them a little so white text keeps its contrast.
    private const val PHOTO_DIM = 0x26000000

    fun create(info: WallpaperInfo, screenWidth: Int, screenHeight: Int, assets: AssetManager): Wallpaper = when (info) {
        is WallpaperInfo.Painted -> painted(info, screenWidth, screenHeight)
        // A missing or broken asset falls back to the default painted wallpaper.
        is WallpaperInfo.Photo -> photo(info, screenWidth, screenHeight, assets)
            ?: painted(WallpaperCatalog.painted.first(), screenWidth, screenHeight)
    }

    private fun painted(info: WallpaperInfo.Painted, screenWidth: Int, screenHeight: Int): Wallpaper {
        val w = screenWidth / SHARP_SCALE
        val h = screenHeight / SHARP_SCALE
        val sharp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        paint(Canvas(sharp), info, w, h)
        return finish(info.id, sharp, screenWidth, screenHeight)
    }

    // Photos keep their full 1920x1080: detail shows at half size, where gradients did not.
    private fun photo(info: WallpaperInfo.Photo, screenWidth: Int, screenHeight: Int, assets: AssetManager): Wallpaper? {
        val options = BitmapFactory.Options().apply { inMutable = true }
        val photo = try {
            assets.open(info.asset).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: IOException) {
            null
        } ?: return null
        val canvas = Canvas(photo)
        canvas.drawColor(PHOTO_DIM)
        shadeBottom(canvas, photo.width, photo.height, Paint(Paint.DITHER_FLAG))
        return finish(info.id, photo, screenWidth, screenHeight)
    }

    /** Blurred copy for glass, then both move to the GPU; nothing reads their pixels on the CPU. */
    private fun finish(id: String, sharp: Bitmap, screenWidth: Int, screenHeight: Int): Wallpaper {
        val blurred = toGpu(blur(sharp, screenWidth / DOWNSCALE, screenHeight / DOWNSCALE))
        return Wallpaper(id, toGpu(sharp).asImageBitmap(), blurred.asImageBitmap(), DOWNSCALE)
    }

    private fun toGpu(bitmap: Bitmap): Bitmap {
        val gpu = bitmap.copy(Bitmap.Config.HARDWARE, false) ?: return bitmap
        bitmap.recycle()
        return gpu
    }

    private fun blur(source: Bitmap, width: Int, height: Int): Bitmap {
        val small = Bitmap.createScaledBitmap(source, width, height, true)
        val px = IntArray(width * height)
        small.getPixels(px, 0, width, 0, 0, width, height)
        small.setPixels(BoxBlur.blur(px, width, height, BLUR_RADIUS), 0, width, 0, 0, width, height)
        return small
    }

    private fun paint(canvas: Canvas, info: WallpaperInfo.Painted, w: Int, h: Int) {
        canvas.drawColor(info.base.toInt())
        // Dithering breaks up the 8-bit steps that wide, dark gradients would otherwise show.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        for (blob in WallpaperLayout.blobs(info)) {
            val cx = blob.x * w
            val cy = blob.y * h
            val r = blob.radius * w
            val rgb = blob.color.toInt() and 0x00FFFFFF
            val colors = IntArray(FalloffAlpha.size) { (FalloffAlpha[it] shl 24) or rgb }
            paint.shader = RadialGradient(cx, cy, r, colors, FalloffStops, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, paint)
        }
        shadeBottom(canvas, w, h, paint)
    }

    /** Darker toward the bottom, where the dock and grid labels sit. */
    private fun shadeBottom(canvas: Canvas, w: Int, h: Int, paint: Paint) {
        paint.shader = LinearGradient(0f, h * 0.45f, 0f, h.toFloat(), 0, BOTTOM_SHADE, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }
}
