package com.kevin.glasslauncher.update

/** "v0.3.0-beta.1+build" -> numbers [0, 3, 0], pre-release ["beta", "1"]; build metadata is ignored. */
private class Version(val numbers: List<Int>, val pre: List<String>)

private fun parseVersion(text: String): Version? {
    val bare = text.trim().removePrefix("v").removePrefix("V").substringBefore('+')
    val core = bare.substringBefore('-')
    val pre = if ('-' in bare) bare.substringAfter('-').split('.') else emptyList()
    val numbers = core.split('.').map { it.toIntOrNull() ?: return null }
    return Version(numbers, pre)
}

/**
 * True when [remote] is a later version than [current]: "v0.3" > "0.2.0", "0.3.0" > "0.3.0-beta".
 * Missing parts count as 0. Unreadable versions are never newer (Glass will not offer them).
 */
fun isNewer(remote: String, current: String): Boolean {
    val r = parseVersion(remote) ?: return false
    val c = parseVersion(current) ?: return false
    val length = maxOf(r.numbers.size, c.numbers.size)
    for (i in 0 until length) {
        val diff = r.numbers.getOrElse(i) { 0 }.compareTo(c.numbers.getOrElse(i) { 0 })
        if (diff != 0) return diff > 0
    }
    return comparePre(r.pre, c.pre) > 0
}

/** Semver pre-release order: none beats any; numeric parts compare as numbers and sort first. */
private fun comparePre(a: List<String>, b: List<String>): Int {
    if (a.isEmpty() && b.isEmpty()) return 0
    if (a.isEmpty()) return 1
    if (b.isEmpty()) return -1
    for (i in 0 until minOf(a.size, b.size)) {
        val diff = compareIdentifier(a[i], b[i])
        if (diff != 0) return diff
    }
    return a.size.compareTo(b.size)
}

private fun compareIdentifier(a: String, b: String): Int {
    val x = a.toIntOrNull()
    val y = b.toIntOrNull()
    return when {
        x != null && y != null -> x.compareTo(y)
        x != null -> -1
        y != null -> 1
        else -> a.compareTo(b)
    }
}
