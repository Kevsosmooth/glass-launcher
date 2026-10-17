package com.kevin.glasslauncher.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val TARGET = "com.kevin.glasslauncher"

// ~4 presses a second, like a person browsing; the burst below stands in for a held D-pad.
private const val PRESS_MS = 250L
private const val START_TIMEOUT_MS = 5_000L

/** Explicit component: the launcher has no phone LAUNCHER entry, so the default intent lookup fails. */
fun homeIntent(): Intent = Intent(Intent.ACTION_MAIN).setComponent(ComponentName(TARGET, "$TARGET.MainActivity"))

fun MacrobenchmarkScope.startHome() {
    startActivityAndWait(homeIntent())
    device.wait(Until.hasObject(By.pkg(TARGET)), START_TIMEOUT_MS)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.press(times: Int, key: () -> Boolean) = repeat(times) {
    key()
    Thread.sleep(PRESS_MS)
}

/** Across the dock and back (D-pad only, never Select). */
fun MacrobenchmarkScope.dockSweep() {
    press(5) { device.pressDPadRight() }
    press(5) { device.pressDPadLeft() }
}

/** Down through the all-apps grid, across, and back up to the dock. */
fun MacrobenchmarkScope.gridSweep() {
    press(6) { device.pressDPadDown() }
    press(5) { device.pressDPadRight() }
    press(5) { device.pressDPadLeft() }
    press(6) { device.pressDPadUp() }
}

/** Back-to-back presses in one shell call: closest adb gets to a held D-pad. */
fun MacrobenchmarkScope.heldBurst() {
    device.executeShellCommand("input keyevent 22 22 22 22 22 21 21 21 21 21 20 20 20 20 20 20 19 19 19 19 19 19")
    device.waitForIdle()
}
