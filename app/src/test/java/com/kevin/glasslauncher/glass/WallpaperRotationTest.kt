package com.kevin.glasslauncher.glass

import com.kevin.glasslauncher.data.Rotation
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperRotationTest {
    private val ids = WallpaperCatalog.painted.map { it.id }
    private val ny = ZoneId.of("America/New_York")
    private val hour = 3_600_000L

    private fun millis(local: String, zone: ZoneId) =
        LocalDateTime.parse(local).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun offShowsTheSelectedWallpaper() {
        for (id in ids) assertEquals(id, wallpaperFor(id, Rotation.Off, 123_456_789L, ny))
        assertNull(nextChangeMillis(Rotation.Off, 123_456_789L, ny))
    }

    @Test
    fun unknownIdFallsBackToTheFirst() {
        assertEquals(ids[0], wallpaperFor("gone", Rotation.Off, 0L, ny))
    }

    @Test
    fun hourlyStepsOncePerHourFromTheSelectedOne() {
        val start = ids.indexOf("ocean")
        for (h in 0L until ids.size * 2L) {
            val expected = ids[((start + h) % ids.size).toInt()]
            assertEquals(expected, wallpaperFor("ocean", Rotation.Hourly, h * hour, ny))
            assertEquals(expected, wallpaperFor("ocean", Rotation.Hourly, h * hour + hour - 1, ny))
        }
    }

    @Test
    fun photosRotateAmongPhotosAndPaintedAmongPainted() {
        val photoIds = WallpaperCatalog.photos.map { it.id }
        val start = photoIds.indexOf("sea-sunset")
        for (h in 0L until photoIds.size * 2L) {
            val id = wallpaperFor("sea-sunset", Rotation.Hourly, h * hour, ny)
            assertEquals(photoIds[((start + h) % photoIds.size).toInt()], id)
        }
        for (h in 0L until 30L) assertTrue(wallpaperFor("dusk", Rotation.Hourly, h * hour, ny) in ids)
    }

    @Test
    fun hourlyNextChangeIsTheNextWholeHour() {
        assertEquals(hour, nextChangeMillis(Rotation.Hourly, 0L, ny))
        assertEquals(5 * hour, nextChangeMillis(Rotation.Hourly, 4 * hour + 1, ny))
        assertEquals(5 * hour, nextChangeMillis(Rotation.Hourly, 5 * hour - 1, ny))
    }

    @Test
    fun dailyChangesAtLocalMidnight() {
        val before = millis("2026-10-17T23:59:59", ny)
        val after = millis("2026-10-18T00:00:00", ny)
        val a = wallpaperFor("aurora", Rotation.Daily, before, ny)
        val b = wallpaperFor("aurora", Rotation.Daily, after, ny)
        assertEquals(ids[(ids.indexOf(a) + 1) % ids.size], b)
        assertEquals(a, wallpaperFor("aurora", Rotation.Daily, millis("2026-10-17T00:00:00", ny), ny))
        assertEquals(after, nextChangeMillis(Rotation.Daily, before, ny))
    }

    @Test
    fun dailyUsesTheLocalDayOfTheGivenZone() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val since = millis("2026-10-17T08:00:00", ny) // Oct 17 in both zones
        val t = millis("2026-10-17T12:00:00", ny) // already Oct 18 in Tokyo
        assertEquals("aurora", wallpaperFor("aurora", Rotation.Daily, t, ny, since))
        assertEquals(ids[(ids.indexOf("aurora") + 1) % ids.size], wallpaperFor("aurora", Rotation.Daily, t, tokyo, since))
    }

    @Test
    fun springForwardDayIs23Hours() {
        val dayStart = millis("2026-03-08T00:00:00", ny)
        val noon = millis("2026-03-08T12:00:00", ny)
        val next = checkNotNull(nextChangeMillis(Rotation.Daily, noon, ny))
        assertEquals(millis("2026-03-09T00:00:00", ny), next)
        assertEquals(23 * hour, next - dayStart)
        val today = wallpaperFor("aurora", Rotation.Daily, noon, ny)
        assertEquals(ids[(ids.indexOf(today) + 1) % ids.size], wallpaperFor("aurora", Rotation.Daily, next, ny))
        assertEquals(today, wallpaperFor("aurora", Rotation.Daily, next - 1, ny))
    }

    @Test
    fun fallBackDayIs25Hours() {
        val dayStart = millis("2026-11-01T00:00:00", ny)
        val next = checkNotNull(nextChangeMillis(Rotation.Daily, dayStart, ny))
        assertEquals(25 * hour, next - dayStart)
        val today = wallpaperFor("aurora", Rotation.Daily, dayStart, ny)
        assertEquals(today, wallpaperFor("aurora", Rotation.Daily, next - 1, ny))
        assertEquals(ids[(ids.indexOf(today) + 1) % ids.size], wallpaperFor("aurora", Rotation.Daily, next, ny))
    }

    @Test
    fun aFreshPickShowsUntilTheNextBoundary() {
        val picked = millis("2026-10-17T15:20:00", ny)
        for (id in ids) {
            assertEquals(id, wallpaperFor(id, Rotation.Daily, picked, ny, sinceMillis = picked))
            assertEquals(id, wallpaperFor(id, Rotation.Hourly, picked + 30 * 60_000L, ny, sinceMillis = picked))
        }
        val tomorrow = millis("2026-10-18T00:00:00", ny)
        assertEquals(ids[1], wallpaperFor(ids[0], Rotation.Daily, tomorrow, ny, sinceMillis = picked))
        assertEquals(ids[1], wallpaperFor(ids[0], Rotation.Hourly, millis("2026-10-17T16:00:00", ny), ny, sinceMillis = picked))
    }
}
