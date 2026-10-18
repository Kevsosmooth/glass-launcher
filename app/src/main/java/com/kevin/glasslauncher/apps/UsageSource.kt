package com.kevin.glasslauncher.apps

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Screen time per package from Android's usage history. Needs "Usage access" (Settings > Apps >
 * Special app access), which only the user can grant; without it [foregroundTime] is empty and
 * the grid falls back to Glass's own recent launches.
 */
class UsageSource(context: Context) {
    private val app = context.applicationContext
    private val stats = app.getSystemService(UsageStatsManager::class.java)
    private val ops = app.getSystemService(AppOpsManager::class.java)

    val hasAccess: Boolean
        get() {
            val appOps = ops ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
            } else {
                legacyCheck(appOps)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

    /** Foreground millis per package over the last [days] days; empty without usage access. */
    fun foregroundTime(days: Int = 60): Map<String, Long> {
        val usage = stats ?: return emptyMap()
        if (!hasAccess) return emptyMap()
        val now = System.currentTimeMillis()
        return usage.queryAndAggregateUsageStats(now - days * DAY_MILLIS, now)
            .mapValues { it.value.totalTimeInForeground }
            .filterValues { it > 0 }
    }

    /** Android 9 only; the call is deprecated from 10, where the branch above runs instead. */
    private fun legacyCheck(appOps: AppOpsManager): Int =
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
}
