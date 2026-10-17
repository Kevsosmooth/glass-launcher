package com.kevin.glasslauncher.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuPlacementTest {
    private val screen = Size(960f, 540f)
    private val menu = Size(236f, 260f)

    private fun place(tile: Rect) = menuPosition(tile, menu, screen, gap = 16f, edge = 24f)

    @Test
    fun menuSitsRightOfTheTileWhenItFits() {
        assertEquals(Offset(186f, 200f), place(Rect(46f, 200f, 170f, 270f)))
    }

    @Test
    fun menuFlipsLeftNearTheRightEdge() {
        assertEquals(Offset(554f, 200f), place(Rect(806f, 200f, 930f, 270f)))
    }

    @Test
    fun menuStaysOnScreenVertically() {
        assertEquals(256f, place(Rect(46f, 470f, 170f, 540f)).y, 0f)
        assertEquals(24f, place(Rect(46f, 0f, 170f, 30f)).y, 0f)
    }
}
