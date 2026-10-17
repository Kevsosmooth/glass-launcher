package com.kevin.glasslauncher.controls

/** Sleep timer choices in minutes; Select on the tile walks through them, then back to off. */
val SleepTimerPresets: List<Int> = listOf(15, 30, 60, 90)

const val MINUTE_MILLIS = 60_000L

/** Whole minutes left before [sleepAtMillis] (rounded up); 0 when the timer is off or has passed. */
fun remainingMinutes(sleepAtMillis: Long, nowMillis: Long): Int =
    if (sleepAtMillis <= nowMillis) 0 else ((sleepAtMillis - nowMillis + MINUTE_MILLIS - 1) / MINUTE_MILLIS).toInt()

/** The setting after one with [remaining] minutes left: the first longer preset, else 0 (off). */
fun nextTimerMinutes(remaining: Int): Int = SleepTimerPresets.firstOrNull { it > remaining } ?: 0

/** Wall-clock time a [minutes] timer started at [nowMillis] fires; 0 for off. */
fun sleepAtFor(minutes: Int, nowMillis: Long): Long = if (minutes <= 0) 0L else nowMillis + minutes * MINUTE_MILLIS

/** Milliseconds until [remainingMinutes] next drops by one; only meaningful while the timer runs. */
fun millisToNextMinuteDrop(sleepAtMillis: Long, nowMillis: Long): Long =
    (sleepAtMillis - nowMillis - 1).mod(MINUTE_MILLIS) + 1
