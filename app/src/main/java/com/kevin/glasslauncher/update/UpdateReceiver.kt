package com.kevin.glasslauncher.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.kevin.glasslauncher.LauncherApp

/**
 * PackageInstaller's answer for an update session (unexported: only our own PendingIntent reaches
 * it). "Needs the user" hands Android's confirmation screen to the [UpdateController], which checks
 * it ([systemConfirmIntent]) and shows it; every other status goes back to the controller too.
 */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return
        val updates = (context.applicationContext as LauncherApp).container.updates
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) {
            updates.onInstallResult(status)
            return
        }
        updates.onConfirmNeeded(confirmIntent(intent))
    }

    private fun confirmIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        }
}
