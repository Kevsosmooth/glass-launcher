package com.kevin.glasslauncher.controls

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Puts the TV to sleep through the force-lock-only device admin [SleepAdminReceiver]. */
class SleepControl(context: Context) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, SleepAdminReceiver::class.java)

    val isAdmin: Boolean get() = dpm.isAdminActive(admin)

    /** Turns the screen off; false when the admin is not granted yet. */
    fun sleepNow(): Boolean {
        if (!isAdmin) return false
        dpm.lockNow()
        return true
    }

    /** The system "activate device admin" screen; often missing on TVs (use the adb command then). */
    fun adminRequest(explanation: String): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
        .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
}
