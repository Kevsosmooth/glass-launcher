package com.kevin.glasslauncher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsCodecTest {
    @Test
    fun roundTripKeepsOrder() {
        val list = listOf("b/b.Main", "a/a.Main", "c/c.Tv")
        assertEquals(list, decodeList(encodeList(list)))
    }

    @Test
    fun emptyRoundTrip() {
        assertEquals(emptyList<String>(), decodeList(encodeList(emptyList())))
        assertEquals(emptyList<String>(), decodeList(""))
    }
}
