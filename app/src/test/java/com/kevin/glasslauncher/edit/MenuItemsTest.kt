package com.kevin.glasslauncher.edit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuItemsTest {
    @Test
    fun dockTileOffersMoveAndRemove() {
        val actions = menuItems(inDock = true, slot = DockSlot.InDock).map { it.action }
        assertEquals(
            listOf(MenuAction.Move, MenuAction.RemoveFromDock, MenuAction.Hide, MenuAction.AppInfo, MenuAction.Uninstall),
            actions,
        )
    }

    @Test
    fun gridTileOffersAddToDockWhenThereIsRoom() {
        val items = menuItems(inDock = false, slot = DockSlot.Free)
        assertEquals(
            listOf(MenuAction.AddToDock, MenuAction.Hide, MenuAction.AppInfo, MenuAction.Uninstall),
            items.map { it.action },
        )
        assertTrue(items.all { it.enabled && it.slot == null })
    }

    @Test
    fun addToDockIsDisabledWithTheReasonWhenFullOrDocked() {
        for (slot in listOf(DockSlot.Full, DockSlot.InDock)) {
            val add = menuItems(inDock = false, slot = slot).first()
            assertFalse(add.enabled)
            assertEquals(slot, add.slot)
        }
    }
}
