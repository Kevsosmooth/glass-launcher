package com.kevin.glasslauncher.home

import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.apps.BannerCache
import com.kevin.glasslauncher.apps.TileArt

/** Where tiles get their pictures. The app uses [BannerCache]; layout tests use one with no art. */
interface TileArtSource {
    /** Art already in memory, so a tile can draw it in its first frame. */
    fun peek(app: AppEntry): TileArt?

    suspend fun load(app: AppEntry): TileArt?
}

/** [BannerCache] as a [TileArtSource]. */
class BannerArtSource(private val banners: BannerCache) : TileArtSource {
    override fun peek(app: AppEntry): TileArt? = banners.peek(app)

    override suspend fun load(app: AppEntry): TileArt = banners.load(app)
}
