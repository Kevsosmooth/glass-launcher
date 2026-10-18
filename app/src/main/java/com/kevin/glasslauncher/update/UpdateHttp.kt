package com.kevin.glasslauncher.update

import com.kevin.glasslauncher.debug.NetLog
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** The only hosts Glass talks to: the GitHub API, release pages, and where release files live. */
val UPDATE_HOSTS = setOf(
    "api.github.com",
    "github.com",
    "objects.githubusercontent.com",
    "release-assets.githubusercontent.com",
)

/** Redirects followed per request (GitHub uses one or two for release files). */
const val MAX_REDIRECTS = 5

private const val TIMEOUT_MS = 10_000
private const val HTTPS_PORT = 443

/** Opens a connection; tests pass fakes. Only ever called with allow-listed HTTPS URLs. */
fun interface ConnectionFactory {
    fun open(url: URL): HttpURLConnection
}

/** The real factory: TLS connections only. */
val HttpsConnections = ConnectionFactory { url ->
    url.openConnection() as? HttpsURLConnection ?: throw UpdateException(FailReason.Blocked)
}

/** A 2xx response body, counted as it is read so the request log knows the size. */
class Body internal constructor(val stream: InputStream, val contentLength: Long)

/**
 * GET over HTTPS to [UPDATE_HOSTS] only. Redirects are followed by hand (at most [MAX_REDIRECTS]),
 * and every hop must stay on HTTPS and on the allow-list. Every request lands in [NetLog] with its
 * host and path only. Failures throw [UpdateException]; nothing here crashes the app.
 */
class UpdateHttp(private val userAgent: String, private val connections: ConnectionFactory = HttpsConnections) {

    /** Fetches [url] and hands the body to [read]; the connection is closed afterwards. */
    fun <T> get(url: String, accept: String, read: (Body) -> T): T {
        var current = allowedUrl(url) ?: throw UpdateException(FailReason.Blocked)
        repeat(MAX_REDIRECTS + 1) {
            when (val hop = fetch(current, accept, read)) {
                is Hop.Done -> return hop.value
                is Hop.Redirect -> current = hop.to
            }
        }
        throw UpdateException(FailReason.Blocked)
    }

    private sealed interface Hop<out T> {
        class Done<T>(val value: T) : Hop<T>
        class Redirect(val to: URL) : Hop<Nothing>
    }

    private fun <T> fetch(url: URL, accept: String, read: (Body) -> T): Hop<T> {
        val started = System.nanoTime()
        val logUrl = url.forLog()
        var status: Int? = null
        var counter: CountingStream? = null
        try {
            val conn = connections.open(url)
            try {
                conn.configure(accept)
                val code = conn.responseCode
                status = code
                if (code in 300..399) {
                    val next = conn.getHeaderField("Location")?.let { resolve(url, it) }
                    NetLog.record("GET", logUrl, code, 0, elapsedMs(started))
                    return Hop.Redirect(next ?: throw UpdateException(FailReason.Blocked))
                }
                if (code !in 200..299) throw UpdateException(FailReason.Server)
                val counted = CountingStream(conn.inputStream).also { counter = it }
                val value = read(Body(counted, conn.contentLengthLong))
                NetLog.record("GET", logUrl, code, counted.count, elapsedMs(started))
                return Hop.Done(value)
            } finally {
                conn.disconnect()
            }
        } catch (e: UpdateException) {
            if (status == null || status !in 300..399) {
                NetLog.record("GET", logUrl, status, counter?.count ?: 0, elapsedMs(started), e.reason.name)
            }
            throw e
        } catch (e: IOException) {
            NetLog.record("GET", logUrl, status, counter?.count ?: 0, elapsedMs(started), e.javaClass.simpleName)
            throw UpdateException(FailReason.Network)
        } catch (e: SecurityException) {
            NetLog.record("GET", logUrl, status, 0, elapsedMs(started), e.javaClass.simpleName)
            throw UpdateException(FailReason.Network)
        }
    }

    private fun HttpURLConnection.configure(accept: String) {
        instanceFollowRedirects = false
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        useCaches = false
        requestMethod = "GET"
        setRequestProperty("Accept", accept)
        setRequestProperty("User-Agent", userAgent)
    }
}

/** [text] as a URL Glass may fetch: HTTPS, default port, no user info, host on [UPDATE_HOSTS]. */
fun allowedUrl(text: String): URL? = try {
    URL(text).takeIf(::isAllowed)
} catch (e: MalformedURLException) {
    null
}

private fun resolve(base: URL, location: String): URL? = try {
    URL(base, location).takeIf(::isAllowed)
} catch (e: MalformedURLException) {
    null
}

private fun isAllowed(url: URL): Boolean =
    url.protocol.equals("https", ignoreCase = true) &&
        (url.port == -1 || url.port == HTTPS_PORT) &&
        url.userInfo == null &&
        url.host.lowercase() in UPDATE_HOSTS

/** Host and path only: signed download URLs carry tokens in the query string. */
private fun URL.forLog(): String = "https://$host$path"

private fun elapsedMs(startedNanos: Long): Long = (System.nanoTime() - startedNanos) / 1_000_000

/** Counts bytes read through it. */
private class CountingStream(input: InputStream) : FilterInputStream(input) {
    var count = 0L
        private set

    override fun read(): Int = super.read().also { if (it >= 0) count++ }

    override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) count += it }
}
