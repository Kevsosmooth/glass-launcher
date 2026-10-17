package com.kevin.glasslauncher.glass

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Unit tests run with the app module as the working directory.
class WallpaperCatalogTest {
    @Test
    fun idsAreUnique() {
        val ids = WallpaperCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyPhotoHasItsAssetAndACreditRow() {
        val credits = File("../docs/wallpaper-credits.md").readText()
        for (photo in WallpaperCatalog.photos) {
            assertTrue("missing ${photo.asset}", File("src/main/assets/${photo.asset}").isFile)
            assertTrue("no credit row for ${photo.id}", "| ${photo.id}.jpg |" in credits)
            assertTrue("empty credit for ${photo.id}", photo.credit.isNotBlank())
        }
    }

    @Test
    fun everyBundledPhotoIsInTheCatalog() {
        val listed = WallpaperCatalog.photos.map { it.asset.substringAfter('/') }.toSet()
        val files = File("src/main/assets/wallpapers").list().orEmpty().toSet()
        assertEquals(listed, files)
    }

    @Test
    fun everyCategoryHasAWallpaper() {
        val used = WallpaperCatalog.all.map { it.category }.toSet()
        assertEquals(WallpaperCategory.entries.toSet(), used)
    }
}
