package com.kevin.glasslauncher.edit

import org.junit.Assert.assertEquals
import org.junit.Test

class DockEditsTest {
    private val dock = listOf("a", "b", "c", "d")

    @Test
    fun moveSwapsWithTheNeighbour() {
        assertEquals(listOf("a", "c", "b", "d"), moveInDock(dock, "b", 1))
        assertEquals(listOf("b", "a", "c", "d"), moveInDock(dock, "b", -1))
    }

    @Test
    fun moveStopsAtTheEnds() {
        assertEquals(dock, moveInDock(dock, "a", -1))
        assertEquals(dock, moveInDock(dock, "d", 1))
    }

    @Test
    fun moveOfAnUnknownKeyChangesNothing() {
        assertEquals(dock, moveInDock(dock, "z", 1))
    }

    @Test
    fun removeDropsOnlyThatKey() {
        assertEquals(listOf("a", "c", "d"), removeFromDock(dock, "b"))
        assertEquals(dock, removeFromDock(dock, "z"))
    }

    @Test
    fun slotSaysWhyAnAppCannotBeAdded() {
        assertEquals(DockSlot.InDock, dockSlot(dock, "a", 4))
        assertEquals(DockSlot.Full, dockSlot(dock, "z", 4))
        assertEquals(DockSlot.Free, dockSlot(dock, "z", 6))
    }

    @Test
    fun addAppendsOnlyWhenThereIsRoom() {
        assertEquals(dock + "z", addToDock(dock, "z", 6))
        assertEquals(dock, addToDock(dock, "z", 4))
        assertEquals(dock, addToDock(dock, "a", 6))
    }
}
