package com.kevin.glasslauncher.system

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.provider.Settings
import java.util.Date
import java.util.TimeZone

data class StorageVolumeInfo(val name: String, val total: Long, val free: Long, val removable: Boolean) {
    val used: Long get() = (total - free).coerceAtLeast(0)
    val usedFraction: Float get() = if (total <= 0) 0f else used.toFloat() / total
}

data class AboutInfo(
    val deviceName: String,
    val model: String,
    val android: String,
    val securityPatch: String,
    val build: String,
    val memoryBytes: Long,
    val uptimeMillis: Long,
    val timeZone: String,
)

/** Storage volumes and device facts for the Storage and About pages. All read-only. */
class DeviceSource(context: Context) {
    private val app = context.applicationContext

    fun storage(): List<StorageVolumeInfo> {
        val internal = statFs(Environment.getDataDirectory().path)?.let { (total, free) ->
            StorageVolumeInfo(INTERNAL_NAME, total, free, removable = false)
        }
        return listOfNotNull(internal) + removable()
    }

    private fun removable(): List<StorageVolumeInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val manager = app.getSystemService(StorageManager::class.java) ?: return emptyList()
        return manager.storageVolumes
            .filter { !it.isPrimary && it.state == Environment.MEDIA_MOUNTED }
            .mapNotNull { volume ->
                val path = volume.directory?.path ?: return@mapNotNull null
                statFs(path)?.let { (total, free) ->
                    StorageVolumeInfo(volume.getDescription(app), total, free, volume.isRemovable)
                }
            }
    }

    private fun statFs(path: String): Pair<Long, Long>? = try {
        StatFs(path).let { it.totalBytes to it.availableBytes }
    } catch (e: IllegalArgumentException) {
        null
    }

    fun about(): AboutInfo {
        val memory = ActivityManager.MemoryInfo()
        app.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memory)
        val model = listOf(Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, Build.MODEL)
            .distinct().joinToString(" ")
        return AboutInfo(
            deviceName = Settings.Global.getString(app.contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL,
            model = model,
            android = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            securityPatch = Build.VERSION.SECURITY_PATCH,
            build = Build.DISPLAY,
            memoryBytes = memory.totalMem,
            uptimeMillis = SystemClock.elapsedRealtime(),
            timeZone = TimeZone.getDefault().let { it.getDisplayName(it.inDaylightTime(Date()), TimeZone.LONG) },
        )
    }

    private companion object {
        const val INTERNAL_NAME = "Internal storage"
    }
}
