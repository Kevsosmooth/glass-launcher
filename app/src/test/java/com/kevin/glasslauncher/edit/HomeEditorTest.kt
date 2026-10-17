package com.kevin.glasslauncher.edit

import com.kevin.glasslauncher.apps.AppEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeEditorTest {
    private fun app(pkg: String) = AppEntry(pkg, "$pkg.Main", pkg, true)
    private val apps = listOf(app("a"), app("b"), app("c"))
    private val keys = apps.map { it.key }

    @Test
    fun movingReordersTheShownDockAndDropReturnsIt() {
        val editor = HomeEditor()
        editor.startMove(keys[0], keys)
        editor.step(1)
        editor.step(1)
        assertEquals(listOf("b", "c", "a"), editor.arrange(apps).map { it.packageName })
        assertEquals(listOf(keys[1], keys[2], keys[0]), editor.drop())
        assertNull(editor.moving)
    }

    @Test
    fun droppedOrderStaysUntilPrefsEchoIt() {
        val editor = HomeEditor()
        editor.startMove(keys[2], keys)
        editor.step(-1)
        val saved = editor.drop()
        editor.sync(keys)
        assertEquals(listOf("a", "c", "b"), editor.arrange(apps).map { it.packageName })
        editor.sync(saved.orEmpty())
        assertEquals(apps, editor.arrange(apps))
    }

    @Test
    fun syncWhileMovingKeepsTheMove() {
        val editor = HomeEditor()
        editor.startMove(keys[0], keys)
        editor.step(1)
        editor.sync(keys)
        assertEquals(listOf("b", "a", "c"), editor.arrange(apps).map { it.packageName })
    }

    @Test
    fun aDockChangedElsewhereDropsThePendingOrder() {
        val editor = HomeEditor()
        editor.startMove(keys[0], keys)
        editor.step(1)
        editor.drop()
        editor.sync(keys + "d/d.Main")
        assertEquals(apps, editor.arrange(apps))
    }

    @Test
    fun dropWithoutAMoveSavesNothing() {
        assertNull(HomeEditor().drop())
    }
}
