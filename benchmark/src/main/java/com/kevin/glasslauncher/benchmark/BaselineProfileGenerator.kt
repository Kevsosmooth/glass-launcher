package com.kevin.glasslauncher.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Start + dock and grid sweeps, so the focus/scroll paths are precompiled.
 * Needs API 33+ or a rooted device: run on an emulator, not the Shield (API 30).
 * `./gradlew :app:generateBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = TARGET, includeInStartupProfile = true) {
        pressHome()
        startHome()
        dockSweep()
        gridSweep()
    }
}
