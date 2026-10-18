package com.kevin.glasslauncher.weather

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.kevin.glasslauncher.settings.ui.Glyph

private const val SUN_RAYS = 8

/** Small solid weather glyphs for the status corner, in the style of ControlGlyphs (no icon library). */
object WeatherGlyphs {
    val Sun: Glyph = { color -> sun(color, center, size.minDimension / 2) }

    val Moon: Glyph = { color ->
        val r = size.minDimension * 0.42f
        val disc = Path().apply { addOval(Rect(center, r)) }
        val crescent = Path().apply {
            fillType = PathFillType.EvenOdd
            addOval(Rect(center, r))
            addOval(Rect(center + Offset(r * 0.55f, -r * 0.4f), r * 0.85f))
        }
        clipPath(disc) { drawPath(crescent, color) }
    }

    val PartlyCloudy: Glyph = { color ->
        translate(-size.width * 0.06f, size.height * 0.1f) {
            // A gap around the cloud keeps the sun behind it readable at 16 dp.
            clipPath(cloudPath(1.15f), ClipOp.Difference) {
                sun(color, Offset(size.width * 0.68f, size.height * 0.26f), size.minDimension * 0.3f)
            }
            drawPath(cloudPath(1f), color)
        }
    }

    val Cloud: Glyph = { color -> drawPath(cloudPath(1f), color) }

    val Fog: Glyph = { color ->
        val w = size.minDimension * 0.1f
        listOf(0.3f, 0.5f, 0.7f).forEachIndexed { i, f ->
            val inset = if (i == 1) 0.08f else 0.18f
            drawLine(color, Offset(size.width * inset, size.height * f), Offset(size.width * (1 - inset), size.height * f), w, StrokeCap.Round)
        }
    }

    val Rain: Glyph = { color ->
        translate(0f, -size.height * 0.14f) { drawPath(cloudPath(1f), color) }
        val w = size.minDimension * 0.08f
        listOf(0.32f, 0.52f, 0.72f).forEach { x ->
            drawLine(color, Offset(size.width * x, size.height * 0.72f), Offset(size.width * (x - 0.06f), size.height * 0.92f), w, StrokeCap.Round)
        }
    }

    val Snow: Glyph = { color ->
        translate(0f, -size.height * 0.14f) { drawPath(cloudPath(1f), color) }
        val r = size.minDimension * 0.05f
        listOf(Offset(0.32f, 0.78f), Offset(0.52f, 0.9f), Offset(0.72f, 0.78f)).forEach { f ->
            drawCircle(color, r, Offset(size.width * f.x, size.height * f.y))
        }
    }

    val Thunder: Glyph = { color ->
        translate(0f, -size.height * 0.14f) { drawPath(cloudPath(1f), color) }
        val bolt = Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.62f)
            lineTo(size.width * 0.4f, size.height * 0.82f)
            lineTo(size.width * 0.52f, size.height * 0.82f)
            lineTo(size.width * 0.44f, size.height * 1f)
            lineTo(size.width * 0.66f, size.height * 0.76f)
            lineTo(size.width * 0.54f, size.height * 0.76f)
            close()
        }
        drawPath(bolt, color)
    }

    fun forCondition(condition: WeatherCondition): Glyph = when (condition) {
        WeatherCondition.ClearDay -> Sun
        WeatherCondition.ClearNight -> Moon
        WeatherCondition.PartlyCloudy -> PartlyCloudy
        WeatherCondition.Cloudy -> Cloud
        WeatherCondition.Fog -> Fog
        WeatherCondition.Rain -> Rain
        WeatherCondition.Snow -> Snow
        WeatherCondition.Thunder -> Thunder
    }
}

/** A disc with short rays around it, [radius] being the rays' outer edge. */
private fun DrawScope.sun(color: Color, at: Offset, radius: Float) {
    drawCircle(color, radius * 0.5f, at)
    val w = radius * 0.16f
    repeat(SUN_RAYS) { i ->
        rotate(i * 360f / SUN_RAYS, at) {
            drawLine(color, at + Offset(0f, -radius * 0.7f), at + Offset(0f, -radius + w / 2), w, StrokeCap.Round)
        }
    }
}

/** A flat-bottomed cloud across the canvas: two bumps on a rounded base, grown by [scale]. */
private fun DrawScope.cloudPath(scale: Float): Path {
    val base = Size(size.width * 0.8f * scale, size.height * 0.3f * scale)
    val baseTop = Offset(center.x - base.width / 2, size.height * 0.62f - base.height / 2)
    return Path().apply {
        addRoundRect(RoundRect(Rect(baseTop, base), CornerRadius(base.height / 2)))
        addOval(Rect(Offset(size.width * 0.36f, size.height * 0.52f), size.minDimension * 0.17f * scale))
        addOval(Rect(Offset(size.width * 0.58f, size.height * 0.44f), size.minDimension * 0.24f * scale))
    }
}
