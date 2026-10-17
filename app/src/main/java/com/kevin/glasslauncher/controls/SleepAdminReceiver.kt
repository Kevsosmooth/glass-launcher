package com.kevin.glasslauncher.controls

import android.app.admin.DeviceAdminReceiver

/** Device admin with only force-lock, so Sleep can call DevicePolicyManager.lockNow(). Lane `panels`. */
class SleepAdminReceiver : DeviceAdminReceiver()
