package com.kevin.glasslauncher.homebutton

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.kevin.glasslauncher.MainActivity
import com.kevin.glasslauncher.controls.tryStart

/**
 * Optional Home button helper for TVs that keep their own home screen (Fire TV). Off until the
 * user turns it on under Accessibility. Two ways in, since Fire OS versions differ in which they
 * allow: the Home key itself when the system shares it (no flash of the stock screen), else the
 * stock home screen coming up, which is covered with Glass right away. It reads no screen content.
 */
class HomeButtonService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && isStockHome(event.packageName, event.className)) {
            openGlass()
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = when (homeKeyVerdict(event.keyCode, event.action)) {
        KeyVerdict.PassThrough -> false
        KeyVerdict.Swallow -> true
        KeyVerdict.OpenGlass -> {
            openGlass()
            true
        }
    }

    override fun onInterrupt() = Unit

    // A Home intent, so Glass treats it like the Home button (a second press scrolls to the top).
    private fun openGlass() {
        tryStart(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClass(this, MainActivity::class.java))
    }

    companion object {
        /** Whether the user has the helper turned on. */
        fun isOn(context: Context): Boolean = isServiceEnabled(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            "${context.packageName}/${HomeButtonService::class.java.name}",
        )
    }
}
