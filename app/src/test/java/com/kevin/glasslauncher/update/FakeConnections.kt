package com.kevin.glasslauncher.update

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** A canned HTTP answer. */
class FakeResponse(val code: Int, val body: ByteArray = ByteArray(0), val headers: Map<String, String> = emptyMap()) {
    companion object {
        fun ok(body: ByteArray, contentLength: Long? = body.size.toLong()) =
            FakeResponse(200, body, contentLength?.let { mapOf("Content-Length" to it.toString()) }.orEmpty())

        fun ok(text: String) = ok(text.toByteArray())

        fun redirect(location: String) = FakeResponse(302, headers = mapOf("Location" to location))
    }
}

/** Serves [routes] by full URL and records every URL opened; an unknown URL is a 404. */
class FakeConnections(private val routes: Map<String, FakeResponse>) : ConnectionFactory {
    val opened = mutableListOf<String>()
    val connections = mutableListOf<FakeConnection>()

    override fun open(url: URL): HttpURLConnection {
        opened += url.toString()
        return FakeConnection(url, routes[url.toString()] ?: FakeResponse(404)).also { connections += it }
    }
}

class FakeConnection(url: URL, private val response: FakeResponse) : HttpURLConnection(url) {
    var disconnected = false
        private set

    override fun connect() = Unit

    override fun disconnect() {
        disconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun getResponseCode(): Int = response.code

    override fun getHeaderField(name: String): String? = response.headers[name]

    override fun getContentLengthLong(): Long = response.headers["Content-Length"]?.toLong() ?: -1L

    override fun getInputStream(): InputStream = ByteArrayInputStream(response.body)
}
