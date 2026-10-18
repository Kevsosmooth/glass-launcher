package com.kevin.glasslauncher.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetLogTest {
    @Test
    fun hostAndPathDropsSchemeCredentialsQueryAndFragment() {
        assertEquals("api.github.com/repos/x/releases", hostAndPath("https://api.github.com/repos/x/releases?token=abc#top"))
        assertEquals("host:8080/a", hostAndPath("http://user:pw@host:8080/a?b=c"))
        assertEquals("example.com", hostAndPath("https://example.com?q=1"))
        assertEquals("example.com/p", hostAndPath("example.com/p#frag"))
    }

    @Test
    fun recordStoresTheStrippedTargetAndResult() {
        NetLog.record("get", "https://example.com/latest.json?key=secret", 200, bytes = 2048, millis = 120)
        val e = NetLog.entries.value.last()
        assertEquals("GET", e.method)
        assertEquals("example.com/latest.json", e.target)
        assertEquals(200, e.status)
        assertNull(e.error)
        assertEquals(2048L, e.bytes)
        assertEquals(120L, e.millis)
    }

    @Test
    fun keepsOnlyTheLastEight() {
        repeat(10) { NetLog.record("GET", "https://h/$it", null, 0, 0, error = "Timeout") }
        val targets = NetLog.entries.value.map { it.target }
        assertEquals((2 until 10).map { "h/$it" }, targets)
        assertEquals("Timeout", NetLog.entries.value.last().error)
    }
}
