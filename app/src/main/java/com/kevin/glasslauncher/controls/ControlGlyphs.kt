package com.kevin.glasslauncher.controls

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import com.kevin.glasslauncher.settings.ui.Glyph

private const val GEAR_TEETH = 8

/** Simple line glyphs for Quick Controls (foundation only, no icon library). */
object ControlGlyphs {
    val Moon: Glyph = { color ->
        val r = size.minDimension / 2
        val disc = Path().apply { addOval(Rect(center, r)) }
        val crescent = Path().apply {
            fillType = PathFillType.EvenOdd
            addOval(Rect(center, r))
            addOval(Rect(center + Offset(r * 0.55f, -r * 0.4f), r * 0.85f))
        }
        // Even-odd cuts the bite out; the clip drops the part of the bite circle outside the disc.
        clipPath(disc) { drawPath(crescent, color) }
    }

    val Timer: Glyph = { color ->
        val r = size.minDimension / 2
        val w = r * 0.18f
        drawCircle(color, r - w / 2, center, style = Stroke(w))
        drawLine(color, center, center + Offset(0f, -r * 0.55f), w, StrokeCap.Round)
        drawLine(color, center, center + Offset(r * 0.4f, 0f), w, StrokeCap.Round)
    }

    val Wifi: Glyph = { color ->
        val w = size.minDimension * 0.1f
        val dot = Offset(center.x, size.height * 0.85f)
        listOf(0.3f, 0.55f, 0.8f).forEach { f ->
            val r = size.minDimension * f
            drawArc(color, 225f, 90f, false, dot - Offset(r, r), Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Round))
        }
        drawCircle(color, w, dot)
    }

    val Input: Glyph = { color ->
        val w = size.minDimension * 0.09f
        val body = Size(size.width * 0.8f, size.height * 0.5f)
        val topLeft = Offset((size.width - body.width) / 2, size.height * 0.2f)
        drawRoundRect(color, topLeft, body, CornerRadius(w), style = Stroke(w))
        drawLine(color, Offset(center.x, topLeft.y + body.height), Offset(center.x, size.height * 0.92f), w, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.3f, size.height * 0.92f), Offset(size.width * 0.7f, size.height * 0.92f), w, StrokeCap.Round)
    }

    val Display: Glyph = { color ->
        val w = size.minDimension * 0.09f
        val screen = Size(size.width * 0.9f, size.height * 0.62f)
        drawRoundRect(color, Offset(size.width * 0.05f, size.height * 0.08f), screen, CornerRadius(w), style = Stroke(w))
        drawLine(color, Offset(size.width * 0.3f, size.height * 0.88f), Offset(size.width * 0.7f, size.height * 0.88f), w, StrokeCap.Round)
    }

    val Sound: Glyph = { color ->
        val w = size.minDimension * 0.09f
        val speaker = Path().apply {
            moveTo(size.width * 0.08f, size.height * 0.36f)
            lineTo(size.width * 0.28f, size.height * 0.36f)
            lineTo(size.width * 0.5f, size.height * 0.14f)
            lineTo(size.width * 0.5f, size.height * 0.86f)
            lineTo(size.width * 0.28f, size.height * 0.64f)
            lineTo(size.width * 0.08f, size.height * 0.64f)
            close()
        }
        drawPath(speaker, color)
        val origin = Offset(size.width * 0.5f, center.y)
        listOf(0.22f, 0.4f).forEach { f ->
            val r = size.minDimension * f
            drawArc(color, -50f, 100f, false, origin - Offset(r, r), Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Round))
        }
    }

    val Gear: Glyph = { color ->
        val r = size.minDimension / 2
        val tooth = Size(r * 0.3f, r * 0.42f)
        repeat(GEAR_TEETH) { i ->
            rotate(i * 360f / GEAR_TEETH) {
                drawRect(color, Offset(center.x - tooth.width / 2, center.y - r), tooth)
            }
        }
        drawCircle(color, radius = r * 0.52f, style = Stroke(width = r * 0.36f))
    }

    /** Four rounded squares: the launcher's own settings. */
    val Tiles: Glyph = { color ->
        val gap = size.minDimension * 0.14f
        val cell = (size.minDimension - gap) / 2
        for (row in 0..1) {
            for (col in 0..1) {
                drawRoundRect(color, Offset(col * (cell + gap), row * (cell + gap)), Size(cell, cell), CornerRadius(cell * 0.25f))
            }
        }
    }
}
