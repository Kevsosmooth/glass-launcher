package com.kevin.glasslauncher.glass

import com.kevin.glasslauncher.data.Rotation
import java.time.Instant
import java.time.ZoneId

private const val HOUR_MILLIS = 3_600_000L

/**
 * The wallpaper to show now. Off = the selected one. Hourly / Daily step through the wallpapers
 * of the selected one's kind (photos or painted colours), starting from it, by whole hours / local days since
 * [sinceMillis] (when the user picked it), so a fresh pick shows until the next boundary.
 * Unknown ids fall back like [WallpaperCatalog.byId]. Pure.
 */
fun wallpaperFor(
    selectedId: String,
    rotation: Rotation,
    nowMillis: Long,
    zone: ZoneId,
    sinceMillis: Long = 0L,
): String {
    val all = rotationPool(WallpaperCatalog.byId(selectedId))
    val start = all.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    val steps = when (rotation) {
        Rotation.Off -> 0L
        Rotation.Hourly -> Math.floorDiv(nowMillis, HOUR_MILLIS) - Math.floorDiv(sinceMillis, HOUR_MILLIS)
        Rotation.Daily -> localEpochDay(nowMillis, zone) - localEpochDay(sinceMillis, zone)
    }
    return all[Math.floorMod(start + Math.floorMod(steps, all.size.toLong()).toInt(), all.size)].id
}

/** Wall-clock time of the next rotation step after [nowMillis], or null when rotation is Off. Pure. */
fun nextChangeMillis(rotation: Rotation, nowMillis: Long, zone: ZoneId): Long? = when (rotation) {
    Rotation.Off -> null
    Rotation.Hourly -> (Math.floorDiv(nowMillis, HOUR_MILLIS) + 1) * HOUR_MILLIS
    // atStartOfDay handles DST: a day is 23 or 25 hours long when the clocks change.
    Rotation.Daily -> Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().plusDays(1)
        .atStartOfDay(zone).toInstant().toEpochMilli()
}

/** Photos rotate among photos, painted wallpapers among painted ones: a pick sets the mood. */
private fun rotationPool(selected: WallpaperInfo): List<WallpaperInfo> = when (selected) {
    is WallpaperInfo.Painted -> WallpaperCatalog.painted
    is WallpaperInfo.Photo -> WallpaperCatalog.photos
}

private fun localEpochDay(nowMillis: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().toEpochDay()
