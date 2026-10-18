package com.kevin.glasslauncher.update

import com.kevin.glasslauncher.debug.NetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateHttpTest {
    private val api = "https://api.github.com/repos/o/r/releases/latest"

    private fun http(routes: Map<String, FakeResponse>) = FakeConnections(routes).let { it to UpdateHttp("Test/1", it) }

    private fun UpdateHttp.text(url: String): String = get(url, "*/*") { it.stream.readBytes().decodeToString() }

    @Test
    fun allowListAcceptsOnlyGitHubOverHttps() {
        assertNotNull(allowedUrl("https://api.github.com/repos"))
        assertNotNull(allowedUrl("https://github.com/o/r/releases/download/v1/glass-launcher.apk"))
        assertNotNull(allowedUrl("https://objects.githubusercontent.com/x"))
        assertNotNull(allowedUrl("https://release-assets.githubusercontent.com/x?sig=1"))
        assertNotNull(allowedUrl("https://GitHub.com:443/x"))
        assertNull(allowedUrl("http://github.com/x"))
        assertNull(allowedUrl("https://evil.com/x"))
        assertNull(allowedUrl("https://github.com.evil.com/x"))
        assertNull(allowedUrl("https://github.com@evil.com/x"))
        assertNull(allowedUrl("https://user@github.com/x"))
        assertNull(allowedUrl("https://github.com:8443/x"))
        assertNull(allowedUrl("ftp://github.com/x"))
        assertNull(allowedUrl("not a url"))
    }

    @Test
    fun startingOffTheListNeverOpensAConnection() {
        val (fake, http) = http(emptyMap())
        assertEquals(FailReason.Blocked, failureOf { http.text("https://example.com/a") })
        assertTrue(fake.opened.isEmpty())
    }

    @Test
    fun followsAllowedRedirectsByHandIncludingRelativeOnes() {
        val (fake, http) = http(
            mapOf(
                api to FakeResponse.redirect("https://github.com/a"),
                "https://github.com/a" to FakeResponse.redirect("/b"),
                "https://github.com/b" to FakeResponse.redirect("https://release-assets.githubusercontent.com/c?token=secret"),
                "https://release-assets.githubusercontent.com/c?token=secret" to FakeResponse.ok("done"),
            ),
        )
        assertEquals("done", http.text(api))
        assertEquals(4, fake.opened.size)
        assertTrue(fake.connections.all { !it.instanceFollowRedirects && it.disconnected })
        assertEquals("Test/1", fake.connections.first().getRequestProperty("User-Agent"))
    }

    @Test
    fun redirectOffTheListOrToPlainHttpIsBlocked() {
        val (offList, httpA) = http(mapOf(api to FakeResponse.redirect("https://evil.com/apk")))
        assertEquals(FailReason.Blocked, failureOf { httpA.text(api) })
        assertEquals(listOf(api), offList.opened)

        val (downgrade, httpB) = http(mapOf(api to FakeResponse.redirect("http://github.com/apk")))
        assertEquals(FailReason.Blocked, failureOf { httpB.text(api) })
        assertEquals(listOf(api), downgrade.opened)

        val (_, httpC) = http(mapOf(api to FakeResponse(302)))
        assertEquals(FailReason.Blocked, failureOf { httpC.text(api) })
    }

    @Test
    fun atMostFiveRedirects() {
        fun chain(hops: Int): Map<String, FakeResponse> =
            (0 until hops).associate { "https://github.com/$it" to FakeResponse.redirect("https://github.com/${it + 1}") } +
                ("https://github.com/$hops" to FakeResponse.ok("end"))

        val (_, five) = http(chain(MAX_REDIRECTS))
        assertEquals("end", five.text("https://github.com/0"))

        val (fake, six) = http(chain(MAX_REDIRECTS + 1))
        assertEquals(FailReason.Blocked, failureOf { six.text("https://github.com/0") })
        assertEquals(MAX_REDIRECTS + 1, fake.opened.size)
    }

    @Test
    fun errorStatusIsAServerFailure() {
        val (_, http) = http(mapOf(api to FakeResponse(403)))
        assertEquals(FailReason.Server, failureOf { http.text(api) })
    }

    @Test
    fun requestLogNeverKeepsQueryStrings() {
        val signed = "https://release-assets.githubusercontent.com/file?token=secret"
        val (_, http) = http(mapOf(signed to FakeResponse.ok("x")))
        http.text(signed)
        val entry = NetLog.entries.value.last()
        assertEquals("release-assets.githubusercontent.com/file", entry.target)
        assertEquals(200, entry.status)
        assertEquals(1L, entry.bytes)
        assertFalse(NetLog.entries.value.any { "secret" in it.target })
    }
}
