package com.kevin.glasslauncher.glass

import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.Rotation
import java.time.ZoneOffset
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperScheduleTest {
    private val ids = WallpaperCatalog.painted.map { it.id }
    private val hour = 3_600_000L

    @Test
    fun repeatedPrefsWithTheSameChoiceEmitOnce() = runBlocking {
        val prefs = flowOf(
            Prefs(wallpaperId = "ocean", rotation = Rotation.Off),
            Prefs(wallpaperId = "ocean", rotation = Rotation.Off, clock24 = true),
            Prefs(wallpaperId = "ocean", rotation = Rotation.Off, hud = true),
        )
        val out = effectiveWallpaperIds(prefs, emptyFlow(), { 0L }, { ZoneOffset.UTC }).toList()
        assertEquals(listOf("ocean"), out)
    }

    @Test
    fun hourlyWakesAtEachBoundary() = runBlocking {
        // Each read of the clock lands 1 ms before the next boundary, so every delay is 1 ms.
        var h = 0L
        val now = { (h++) * hour + hour - 1 }
        val prefs = flowOf(Prefs(wallpaperId = "aurora", rotation = Rotation.Hourly))
        val out = effectiveWallpaperIds(prefs, emptyFlow(), now, { ZoneOffset.UTC }).take(3).toList()
        assertEquals(ids.take(3), out)
    }

    @Test
    fun clockJumpReevaluatesWithoutDuplicates() = runBlocking {
        val prefs = flowOf(Prefs(wallpaperId = "dusk", rotation = Rotation.Off))
        val out = effectiveWallpaperIds(prefs, flowOf(Unit, Unit), { 0L }, { ZoneOffset.UTC }).toList()
        assertEquals(listOf("dusk"), out)
    }
}
