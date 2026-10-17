package com.kevin.glasslauncher.debug

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeAppsTest {
    @Test
    fun countIsClampedAndKeysUnique() {
        assertEquals(0, fakeApps(-5).size)
        assertEquals(500, fakeApps(10_000).size)
        val apps = fakeApps(200)
        assertEquals(200, apps.map { it.key }.toSet().size)
    }
}
