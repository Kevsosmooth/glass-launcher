package com.kevin.glasslauncher.glass

import androidx.annotation.StringRes
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.data.DEFAULT_WALLPAPER_ID

/** Picker sections, in display order. */
enum class WallpaperCategory(@StringRes val label: Int) {
    Colors(R.string.panels_wallpaper_colors),
    Nature(R.string.panels_wallpaper_nature),
    Ocean(R.string.panels_wallpaper_ocean),
    Night(R.string.panels_wallpaper_night),
    Mountains(R.string.panels_wallpaper_mountains),
    Space(R.string.panels_wallpaper_space),
}

/** A built-in wallpaper: painted at runtime ([Painted]) or a bundled photo ([Photo]). */
sealed class WallpaperInfo(val id: String, val name: String, val category: WallpaperCategory) {
    /** Soft colour fields: [base] is the background colour, [colors] the light blobs, all ARGB. */
    class Painted(id: String, name: String, val base: Long, val colors: List<Long>) :
        WallpaperInfo(id, name, WallpaperCategory.Colors)

    /** A 1920x1080 JPEG in assets/[asset]; [credit] is shown in the picker (licence terms). */
    class Photo(id: String, name: String, category: WallpaperCategory, val asset: String, val credit: String) :
        WallpaperInfo(id, name, category)
}

object WallpaperCatalog {
    val painted: List<WallpaperInfo.Painted> = listOf(
        WallpaperInfo.Painted(DEFAULT_WALLPAPER_ID, "Aurora", 0xFF0B1020, listOf(0xFF2F6BFF, 0xFFFF5A8A, 0xFF19C6A8, 0xFFFFB020, 0xFF8A4DFF)),
        WallpaperInfo.Painted("dusk", "Dusk", 0xFF140C1C, listOf(0xFFFF7A59, 0xFFB44DFF, 0xFFFF3D7F, 0xFF5B3DFF)),
        WallpaperInfo.Painted("ocean", "Ocean", 0xFF04121C, listOf(0xFF0077FF, 0xFF00C2D1, 0xFF1F4BFF, 0xFF00E0A4)),
        WallpaperInfo.Painted("forest", "Forest", 0xFF07140E, listOf(0xFF1FAF6B, 0xFF8BD13F, 0xFF0F7A5C, 0xFFE0C341)),
        WallpaperInfo.Painted("ember", "Ember", 0xFF180A06, listOf(0xFFFF5A1F, 0xFFFFB020, 0xFFE0233D, 0xFFFF8A3D)),
        WallpaperInfo.Painted("graphite", "Graphite", 0xFF0D0F12, listOf(0xFF4A5568, 0xFF2D3748, 0xFF718096, 0xFF1A202C)),
    )

    // Sources and licences: docs/wallpaper-credits.md. CC0 and public domain unless the credit says otherwise.
    val photos: List<WallpaperInfo.Photo> = listOf(
        photo("forest-spruce", "Tall Spruce", WallpaperCategory.Nature, "W.carter, CC0"),
        photo("forest-ferns", "Fern Forest", WallpaperCategory.Nature, "W.carter, CC0"),
        photo("river-valley", "River Valley", WallpaperCategory.Nature, "Wilfredor, CC0"),
        photo("sea-sunset", "Sea Sunset", WallpaperCategory.Ocean, "Ivano Giambattista, CC0"),
        photo("coast-crete", "Cretan Coast", WallpaperCategory.Ocean, "Jebulon, CC0"),
        photo("aurora-igloo", "Polar Night", WallpaperCategory.Night, "Ross Burgener / NOAA, public domain"),
        photo("aurora-alaska", "Night Lights", WallpaperCategory.Night, "U.S. Air Force / Joshua Strang, public domain"),
        photo("hills-fog", "Morning Fog", WallpaperCategory.Mountains, "Greg200906, CC0"),
        photo("alps-haze", "Alpine Haze", WallpaperCategory.Mountains, "Dktue, CC0"),
        photo("earth-limb", "Low Orbit", WallpaperCategory.Space, "NASA / Kayla Barron, public domain"),
        photo("earth-clouds", "Cloud Sea", WallpaperCategory.Space, "NASA / ISS Expedition 34, public domain"),
        photo("carina-nebula", "Carina Nebula", WallpaperCategory.Space, "ESO/T. Preibisch, CC BY 4.0"),
    )

    val all: List<WallpaperInfo> = painted + photos

    fun byId(id: String): WallpaperInfo = all.firstOrNull { it.id == id } ?: all.first()

    private fun photo(id: String, name: String, category: WallpaperCategory, credit: String) =
        WallpaperInfo.Photo(id, name, category, "wallpapers/$id.jpg", credit)
}
