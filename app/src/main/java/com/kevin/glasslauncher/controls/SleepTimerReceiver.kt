package com.kevin.glasslauncher.controls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kevin.glasslauncher.LauncherApp
import kotlinx.coroutines.launch

/** AlarmManager target for the sleep timer: sleeps the TV (if the admin is granted), clears the timer. */
class SleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SleepControl(context).sleepNow()
        val container = (context.applicationContext as LauncherApp).container
        val pending = goAsync()
        container.scope.launch {
            try {
                container.prefs.setSleepAt(0L)
            } finally {
                pending.finish()
            }
        }
    }
}
