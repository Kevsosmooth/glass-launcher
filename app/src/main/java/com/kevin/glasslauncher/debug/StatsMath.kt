package com.kevin.glasslauncher.debug

private const val UTIME_FIELD = 11 // utime is field 14 of /proc/<pid>/stat; this counts from field 3
private const val STIME_FIELD = 12
private const val BYTES_PER_KB = 1024.0
private const val MILLIS_PER_SECOND = 1000.0

/**
 * User + system CPU time in clock ticks from a /proc/<pid>/stat line, or null if it does not parse.
 * The process name (field 2) may hold spaces, so fields are counted after its closing ')'.
 */
fun parseCpuTicks(stat: String): Long? {
    val close = stat.lastIndexOf(')')
    if (close < 0) return null
    val fields = stat.substring(close + 1).trim().split(' ')
    val user = fields.getOrNull(UTIME_FIELD)?.toLongOrNull() ?: return null
    val system = fields.getOrNull(STIME_FIELD)?.toLongOrNull() ?: return null
    return user + system
}

/**
 * Share of the whole device's CPU ([cores] cores) a process used between two tick readings
 * [elapsedMs] apart, 0..100; null when a reading or the clock rate is missing.
 */
fun cpuPercent(before: Long?, after: Long?, elapsedMs: Long, ticksPerSecond: Long, cores: Int): Float? {
    if (before == null || after == null || elapsedMs <= 0 || ticksPerSecond <= 0 || cores <= 0) return null
    val busySeconds = (after - before).coerceAtLeast(0) / ticksPerSecond.toDouble()
    val percent = busySeconds / (elapsedMs / MILLIS_PER_SECOND) * 100 / cores
    return percent.coerceIn(0.0, 100.0).toFloat()
}

/**
 * KB/s between two byte counters [elapsedMs] apart; null when either is negative
 * (TrafficStats.UNSUPPORTED is -1) or no time passed.
 */
fun kbPerSecond(before: Long, after: Long, elapsedMs: Long): Double? {
    if (before < 0 || after < 0 || elapsedMs <= 0) return null
    return (after - before).coerceAtLeast(0) / BYTES_PER_KB / (elapsedMs / MILLIS_PER_SECOND)
}
