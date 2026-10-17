package com.kevin.glasslauncher.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/** Minutes left on the sleep timer, updated only when the number changes (no per-frame work). */
@Composable
fun rememberRemainingMinutes(sleepAtMillis: Long): State<Int> {
    val left = remember(sleepAtMillis) { mutableIntStateOf(remainingMinutes(sleepAtMillis, System.currentTimeMillis())) }
    LaunchedEffect(sleepAtMillis) {
        while (left.intValue > 0) {
            delay(millisToNextMinuteDrop(sleepAtMillis, System.currentTimeMillis()))
            left.intValue = remainingMinutes(sleepAtMillis, System.currentTimeMillis())
        }
    }
    return left
}
