package com.kevin.glasslauncher.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.time.LocalTime
import kotlinx.coroutines.delay

fun clockText(time: LocalTime, use24h: Boolean): String = if (use24h) {
    "%02d:%02d".format(time.hour, time.minute)
} else {
    val h = time.hour % 12
    "%d:%02d".format(if (h == 0) 12 else h, time.minute)
}

fun millisToNextMinute(nowMillis: Long): Long = 60_000L - nowMillis % 60_000L

/** Clock text that changes once a minute; no frames are drawn between ticks. */
@Composable
fun rememberClockText(use24h: Boolean): State<String> {
    val text = remember(use24h) { mutableStateOf(clockText(LocalTime.now(), use24h)) }
    LaunchedEffect(use24h) {
        while (true) {
            delay(millisToNextMinute(System.currentTimeMillis()))
            text.value = clockText(LocalTime.now(), use24h)
        }
    }
    return text
}
