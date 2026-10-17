package com.kevin.glasslauncher.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Budgets (Glass on, all motion on): p50 < 8 ms, p99 < 16.7 ms, janky < 1%. */
@RunWith(AndroidJUnit4::class)
class NavigationBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun dockAndGrid() = measure {
        dockSweep()
        gridSweep()
    }

    @Test
    fun heldDpad() = measure { heldBurst() }

    private fun measure(block: MacrobenchmarkScope.() -> Unit) = rule.measureRepeated(
        packageName = TARGET,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startHome()
        },
        measureBlock = block,
    )
}
