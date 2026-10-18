package com.kevin.glasslauncher.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Installed launchable apps from PackageManager; [apps] is null until the first load finishes. */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {
    private val pm = context.packageManager
    private val mutex = Mutex()
    private val _apps = MutableStateFlow<List<AppEntry>?>(null)
    val apps: StateFlow<List<AppEntry>?> = _apps.asStateFlow()
    private val usageSource = UsageSource(context)
    private val _usage = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Screen time per package (last 60 days); empty without usage access. Read with every refresh. */
    val usage: StateFlow<Map<String, Long>> = _usage.asStateFlow()

    /** Synthetic entries appended after the real apps (debug stress case). */
    @Volatile
    var extraApps: List<AppEntry> = emptyList()
        set(value) {
            field = value
            refresh()
        }

    /** When set, only these packages are listed (debug: clean screenshots without personal apps). */
    @Volatile
    var onlyPackages: Set<String>? = null
        set(value) {
            field = value
            refresh()
        }

    init {
        refresh()
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                val real = mergeApps(
                    leanback = query(Intent.CATEGORY_LEANBACK_LAUNCHER, isLeanback = true),
                    launcher = query(Intent.CATEGORY_LAUNCHER, isLeanback = false),
                    selfPackage = context.packageName,
                )
                val only = onlyPackages
                _apps.value = (if (only == null) real else real.filter { it.packageName in only }) + extraApps
                _usage.value = usageSource.foregroundTime()
            }
        }
    }

    private fun query(category: String, isLeanback: Boolean): List<AppEntry> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).map { info ->
            val activity = info.activityInfo
            val flags = activity.applicationInfo.flags
            AppEntry(
                activity.packageName,
                activity.name,
                info.loadLabel(pm).toString(),
                isLeanback,
                isDev = looksLikeDevBuild(
                    activity.packageName,
                    debuggable = flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
                    testOnly = flags and ApplicationInfo.FLAG_TEST_ONLY != 0,
                ),
            )
        }

    /** Starts [app]; false if it is gone or refuses to start (caller shows a message). */
    fun launch(from: Context, app: AppEntry): Boolean {
        val category = if (app.isLeanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(category)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            from.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
