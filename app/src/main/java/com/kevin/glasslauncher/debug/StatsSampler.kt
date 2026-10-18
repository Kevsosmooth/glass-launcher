package com.kevin.glasslauncher.debug

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.view.Display
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

private const val FIRST_SAMPLE_MS = 1_000L
private const val SAMPLE_MS = 2_000L
private const val KB_PER_MB = 1024
private const val BYTES_PER_MB = 1024L * 1024L

/** Memory in MB, CPU %, network KB/s (null = n/a) and the display mode, every 2 s. */
data class StatsSample(
    val pssMb: Int,
    val heapUsedMb: Long,
    val heapMaxMb: Long,
    val ramAvailMb: Long,
    val ramTotalMb: Long,
    val lowMemory: Boolean,
    val cpuPercent: Float?,
    val appRxKbs: Double?,
    val appTxKbs: Double?,
    val deviceRxKbs: Double?,
    val deviceTxKbs: Double?,
    val display: DisplayMode?,
)

data class DisplayMode(val width: Int, val height: Int, val refreshHz: Float)

/** Counter readings that only mean something as a delta between two samples. */
private class Counters(
    val atMs: Long,
    val cpuTicks: Long?,
    val appRx: Long,
    val appTx: Long,
    val deviceRx: Long,
    val deviceTx: Long,
)

/**
 * Samples on the IO dispatcher while collected and stops when collection stops, so a hidden
 * overlay costs nothing. The first sample comes after 1 s, since rates need two readings.
 */
fun statsSamples(context: Context, display: Display?): Flow<StatsSample> = flow {
    val probe = StatsProbe(context.applicationContext, display)
    var before = probe.counters()
    delay(FIRST_SAMPLE_MS)
    while (true) {
        val after = probe.counters()
        emit(probe.sample(before, after))
        before = after
        delay(SAMPLE_MS)
    }
}.flowOn(Dispatchers.IO)

private class StatsProbe(context: Context, private val display: Display?) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)
    private val uid = Process.myUid()
    private val cores = Runtime.getRuntime().availableProcessors()
    private val ticksPerSecond = clockTicksPerSecond()

    fun counters() = Counters(
        atMs = SystemClock.elapsedRealtime(),
        cpuTicks = readCpuTicks(),
        appRx = TrafficStats.getUidRxBytes(uid),
        appTx = TrafficStats.getUidTxBytes(uid),
        deviceRx = TrafficStats.getTotalRxBytes(),
        deviceTx = TrafficStats.getTotalTxBytes(),
    )

    fun sample(before: Counters, after: Counters): StatsSample {
        val elapsed = after.atMs - before.atMs
        val memory = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }
        val device = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val runtime = Runtime.getRuntime()
        return StatsSample(
            pssMb = memory.totalPss / KB_PER_MB,
            heapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB,
            heapMaxMb = runtime.maxMemory() / BYTES_PER_MB,
            ramAvailMb = device.availMem / BYTES_PER_MB,
            ramTotalMb = device.totalMem / BYTES_PER_MB,
            lowMemory = device.lowMemory,
            cpuPercent = cpuPercent(before.cpuTicks, after.cpuTicks, elapsed, ticksPerSecond, cores),
            appRxKbs = kbPerSecond(before.appRx, after.appRx, elapsed),
            appTxKbs = kbPerSecond(before.appTx, after.appTx, elapsed),
            deviceRxKbs = kbPerSecond(before.deviceRx, after.deviceRx, elapsed),
            deviceTxKbs = kbPerSecond(before.deviceTx, after.deviceTx, elapsed),
            display = display?.mode?.let { DisplayMode(it.physicalWidth, it.physicalHeight, it.refreshRate) },
        )
    }
}

/** Null (shown as n/a) when /proc/self/stat cannot be read. */
private fun readCpuTicks(): Long? =
    try {
        parseCpuTicks(File("/proc/self/stat").readText())
    } catch (e: IOException) {
        null
    }

/** Kernel clock ticks per second for /proc times; -1 (CPU shown as n/a) if the kernel will not say. */
private fun clockTicksPerSecond(): Long = Os.sysconf(OsConstants._SC_CLK_TCK)
