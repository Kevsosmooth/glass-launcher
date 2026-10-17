package com.kevin.glasslauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentsTest {
    @Test
    fun pushMovesToFrontWithoutDuplicates() {
        assertEquals(listOf("b", "a", "c"), pushRecent(listOf("a", "b", "c"), "b"))
    }

    @Test
    fun pushCapsLength() {
        assertEquals(listOf("x", "a"), pushRecent(listOf("a", "b"), "x", max = 2))
    }
}
