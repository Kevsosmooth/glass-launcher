package com.kevin.glasslauncher.update

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionCompareTest {
    @Test
    fun table() {
        val cases = listOf(
            Triple("0.3.0", "0.2.0", true),
            Triple("v0.3.0", "0.2.0", true),
            Triple("V1.0", "0.9.9", true),
            Triple("0.3", "0.2.0", true),
            Triple("0.2.1", "0.2", true),
            Triple("0.2.0", "0.2.0", false),
            Triple("v0.2", "0.2.0", false),
            Triple("0.2.0", "0.3.0", false),
            Triple("0.10.0", "0.9.0", true),
            Triple("1.0.0", "0.99.99", true),
            Triple("0.3.0-beta", "0.3.0", false),
            Triple("0.3.0", "0.3.0-beta", true),
            Triple("0.3.0-beta.2", "0.3.0-beta.1", true),
            Triple("0.3.0-beta.10", "0.3.0-beta.9", true),
            Triple("0.3.0-rc", "0.3.0-beta", true),
            Triple("0.3.0-beta", "0.2.0", true),
            Triple("0.3.0+build5", "0.3.0", false),
            Triple("latest", "0.2.0", false),
            Triple("", "0.2.0", false),
            Triple("0.3.x", "0.2.0", false),
            Triple("0.3.0", "dev", false),
        )
        for ((remote, current, newer) in cases) {
            assertEquals("$remote vs $current", newer, isNewer(remote, current))
        }
    }
}
