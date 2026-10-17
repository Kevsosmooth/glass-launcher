package com.kevin.glasslauncher.controls

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.kevin.glasslauncher.data.LauncherPrefs

/**
 * Sets the sleep timer to [minutes] (0 = off): one inexact, idle-allowed alarm to
 * [SleepTimerReceiver] (no exact-alarm permission needed), and its fire time in prefs.
 */
suspend fun setSleepTimer(context: Context, prefs: LauncherPrefs, minutes: Int, nowMillis: Long = System.currentTimeMillis()) {
    val alarms = context.getSystemService(AlarmManager::class.java)
    val operation = timerIntent(context)
    val at = sleepAtFor(minutes, nowMillis)
    if (at == 0L) {
        alarms.cancel(operation)
    } else {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
    }
    prefs.setSleepAt(at)
}

// One request code, so a new timer replaces the old alarm instead of adding a second one.
private fun timerIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context,
    0,
    Intent(context, SleepTimerReceiver::class.java),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)
