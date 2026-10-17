package com.kevin.glasslauncher.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperLayoutTest {
    @Test
    fun sameIdGivesSameLayout() {
        for (info in WallpaperCatalog.painted) {
            assertEquals(WallpaperLayout.blobs(info), WallpaperLayout.blobs(info))
        }
    }

    @Test
    fun oneBlobPerColourInOrder() {
        for (info in WallpaperCatalog.painted) {
            assertEquals(info.colors, WallpaperLayout.blobs(info).map { it.color })
        }
    }

    @Test
    fun blobsStayInsideTheScreenAndRadiusRange() {
        for (info in WallpaperCatalog.painted) {
            for (b in WallpaperLayout.blobs(info)) {
                assertTrue(b.x in WallpaperLayout.MIN_POS..WallpaperLayout.MAX_POS)
                assertTrue(b.y in WallpaperLayout.MIN_POS..WallpaperLayout.MAX_POS)
                assertTrue(b.radius in WallpaperLayout.MIN_RADIUS..WallpaperLayout.MAX_RADIUS)
            }
        }
    }

    @Test
    fun blobsAreSpreadOut() {
        for (info in WallpaperCatalog.painted) {
            val blobs = WallpaperLayout.blobs(info)
            for (i in blobs.indices) for (j in i + 1 until blobs.size) {
                val d = WallpaperLayout.distance(blobs[i].x, blobs[i].y, blobs[j].x, blobs[j].y)
                assertTrue("${info.id}: blobs $i and $j only $d apart", d > 0.12f)
            }
        }
    }

    @Test
    fun differentIdsGiveDifferentLayouts() {
        val colors = listOf(0xFF2F6BFF, 0xFFFF5A8A, 0xFF19C6A8)
        val a = WallpaperLayout.blobs(WallpaperInfo.Painted("a", "A", 0xFF000000, colors))
        val b = WallpaperLayout.blobs(WallpaperInfo.Painted("b", "B", 0xFF000000, colors))
        assertNotEquals(a, b)
    }

    @Test
    fun distanceAccountsForScreenAspect() {
        assertEquals(1f, WallpaperLayout.distance(0f, 0f, 1f, 0f), 1e-6f)
        assertEquals(9f / 16f, WallpaperLayout.distance(0f, 0f, 0f, 1f), 1e-6f)
    }
}
