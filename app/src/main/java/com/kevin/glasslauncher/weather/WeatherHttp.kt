package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.debug.NetLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TIMEOUT_MS = 10_000
private const val MAX_BYTES = 256 * 1024

/** The only hosts weather code talks to. */
val WeatherHosts = setOf("api.open-meteo.com", "geocoding-api.open-meteo.com", "get.geojs.io")

/** A GET that returns the body, or null on any failure (failures are quiet by design). */
fun interface HttpGet {
    suspend fun get(url: String): String?
}

/** True for a plain https URL on an allow-listed host (no user info, default port). */
fun isAllowedUrl(url: String): Boolean {
    val uri = try {
        URI(url)
    } catch (e: URISyntaxException) {
        return false
    }
    return uri.scheme == "https" && uri.rawUserInfo == null && uri.port == -1 && uri.host in WeatherHosts
}

/**
 * HTTPS GETs to [WeatherHosts] only, with 10 s timeouts and no redirects (a 3xx is a failure), a
 * size cap, and our User-Agent. Every request, failed or not, goes to [NetLog].
 */
class WeatherHttp(versionName: String) : HttpGet {
    private val userAgent = "GlassLauncher/$versionName"

    override suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        fun log(status: Int?, bytes: Long, error: String?) =
            NetLog.record("GET", url, status, bytes, (System.nanoTime() - started) / 1_000_000, error)

        if (!isAllowedUrl(url)) {
            log(null, 0, "blocked")
            return@withContext null
        }
        // The scheme is https (checked above), so this is always an HttpsURLConnection.
        val connection = try {
            URL(url).openConnection() as HttpsURLConnection
        } catch (e: IOException) {
            log(null, 0, e.javaClass.simpleName)
            return@withContext null
        }
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status !in 200..299) {
                log(status, 0, null)
                return@withContext null
            }
            val bytes = connection.inputStream.use { readCapped(it) }
            if (bytes == null) {
                log(status, MAX_BYTES.toLong(), "too large")
                return@withContext null
            }
            log(status, bytes.size.toLong(), null)
            bytes.toString(Charsets.UTF_8)
        } catch (e: IOException) {
            log(null, 0, e.javaClass.simpleName)
            null
        } finally {
            connection.disconnect()
        }
    }
}

/** The whole stream, or null if it is longer than [MAX_BYTES]. */
private fun readCapped(input: InputStream): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    while (true) {
        val n = input.read(buffer)
        if (n < 0) return out.toByteArray()
        if (out.size() + n > MAX_BYTES) return null
        out.write(buffer, 0, n)
    }
}
