package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.TempUnit

const val REFRESH_MS = 30 * 60_000L
const val RETRY_MS = 5 * 60_000L
const val LOCATION_MAX_AGE_MS = 6 * 60 * 60_000L
const val SHOW_MAX_AGE_MS = 3 * 60 * 60_000L

/** Pure timing rules for the weather loop; every time is wall-clock millis. */
object WeatherTiming {
    /**
     * True when the location must be looked up again: none yet, a picked city is set (free, no
     * network) or was just cleared, the TV's location is no longer allowed, or the automatic one
     * is [LOCATION_MAX_AGE_MS] old.
     */
    fun locationDue(cached: Located?, pickedSet: Boolean, deviceAllowed: Boolean, now: Long): Boolean = when {
        pickedSet -> true
        cached == null -> true
        !usableAutomatic(cached, deviceAllowed) -> true
        else -> age(cached.atMillis, now) >= LOCATION_MAX_AGE_MS
    }

    /** True for an automatic location that may still be used: not a picked city, and from the TV only if allowed. */
    fun usableAutomatic(cached: Located, deviceAllowed: Boolean): Boolean = when (cached.source) {
        LocationSource.Picked -> false
        LocationSource.Network -> deviceAllowed
        LocationSource.Internet -> true
    }

    /** True when no automatic lookup failed in the last [REFRESH_MS], so the services are not hammered. */
    fun retryDue(failedAt: Long?, now: Long): Boolean = failedAt == null || age(failedAt, now) >= REFRESH_MS

    /** True when [last] is missing, for another place or unit, or [REFRESH_MS] old. */
    fun weatherDue(last: WeatherNow?, located: Located, unit: TempUnit, now: Long): Boolean =
        last == null || !sameSpot(last, located, unit) || age(last.atMillis, now) >= REFRESH_MS

    /** [last] if it may be shown for [located] in [unit]: under [SHOW_MAX_AGE_MS] old. */
    fun showable(last: WeatherNow?, located: Located?, unit: TempUnit, now: Long): WeatherNow? =
        last?.takeIf { located != null && sameSpot(it, located, unit) && age(it.atMillis, now) < SHOW_MAX_AGE_MS }

    /** How long to wait before the next check: until [last] is due, or [RETRY_MS] after a failure. */
    fun nextCheckIn(last: WeatherNow?, located: Located?, unit: TempUnit, now: Long): Long {
        if (last == null || located == null || !sameSpot(last, located, unit)) return RETRY_MS
        return (REFRESH_MS - age(last.atMillis, now)).coerceIn(RETRY_MS, REFRESH_MS)
    }

    private fun sameSpot(w: WeatherNow, located: Located, unit: TempUnit) =
        w.unit == unit && w.latitude == located.latitude && w.longitude == located.longitude

    // A clock set backwards gives a negative age; treat it as old so the data refreshes.
    private fun age(at: Long, now: Long): Long = if (now < at) Long.MAX_VALUE else now - at
}
