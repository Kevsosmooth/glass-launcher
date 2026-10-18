package com.kevin.glasslauncher.weather

import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone

/**
 * True when the internet address lookup's [lookupZone] (an IANA id such as "America/New_York")
 * has the same UTC offset right now as the TV's [device] zone. A VPN usually exits in another
 * time zone; comparing the current offsets (not the ids) keeps neighbouring zones and DST changes
 * fine. An unknown zone id counts as a mismatch.
 */
fun lookupZoneMatches(lookupZone: String, device: TimeZone, nowMillis: Long): Boolean {
    val zone = try {
        ZoneId.of(lookupZone)
    } catch (e: DateTimeException) {
        return false
    }
    val lookupOffsetMillis = zone.rules.getOffset(Instant.ofEpochMilli(nowMillis)).totalSeconds * 1000L
    return lookupOffsetMillis == device.getOffset(nowMillis).toLong()
}
