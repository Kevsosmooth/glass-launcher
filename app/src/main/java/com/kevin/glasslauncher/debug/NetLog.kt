package com.kevin.glasslauncher.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val CAPACITY = 8

/**
 * One network request. [target] is host + path only: the scheme, user info, query string and
 * fragment are dropped before it is stored. [status] is null when the request failed with [error].
 */
data class NetEntry(
    val method: String,
    val target: String,
    val status: Int?,
    val error: String?,
    val bytes: Long,
    val millis: Long,
    val atMillis: Long,
)

/** The last [CAPACITY] requests Glass made, newest last, for the Stats overlay. Thread-safe. */
object NetLog {
    private val log = MutableStateFlow<List<NetEntry>>(emptyList())

    val entries: StateFlow<List<NetEntry>> = log.asStateFlow()

    fun record(method: String, url: String, status: Int?, bytes: Long, millis: Long, error: String? = null) {
        val entry = NetEntry(method.uppercase(), hostAndPath(url), status, error, bytes, millis, System.currentTimeMillis())
        log.update { (it + entry).takeLast(CAPACITY) }
    }
}

/** "https://user@host:443/a/b?q=1#f" -> "host:443/a/b"; never keeps query strings or credentials. */
fun hostAndPath(url: String): String {
    val bare = url.substringBefore('#').substringBefore('?').substringAfter("://")
    val slash = bare.indexOf('/')
    val authority = if (slash < 0) bare else bare.substring(0, slash)
    val path = if (slash < 0) "" else bare.substring(slash)
    return authority.substringAfterLast('@') + path
}
