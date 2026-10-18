package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.City
import java.util.TimeZone

/** Raw coordinates from Android's own location; the resolver rounds them. */
data class Coordinates(val latitude: Double, val longitude: Double)

/** Android's network (Wi-Fi) location, or null when it is off, not allowed or unknown. */
fun interface NetworkLocator {
    suspend fun locate(): Coordinates?
}

/** What a resolve found: a location, a lookup that looked like a VPN, or nothing. */
sealed interface Resolution {
    data class Found(val located: Located) : Resolution
    data object LooksWrong : Resolution
    data object NotFound : Resolution
}

/**
 * Picks the weather location: the user's city, else Android's network location (a VPN does not
 * change it) but only when the user opted in, else the internet address lookup, unless its time
 * zone disagrees with the TV's (then it is most likely a VPN exit and is not used).
 */
class LocationResolver(
    private val network: NetworkLocator,
    private val ipGeo: suspend () -> IpGeo?,
    private val deviceZone: () -> TimeZone = TimeZone::getDefault,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun resolve(picked: City?, useDeviceLocation: Boolean): Resolution {
        val now = clock()
        if (picked != null) {
            return found(picked.name, picked.latitude, picked.longitude, LocationSource.Picked, now)
        }
        if (useDeviceLocation) network.locate()?.let { return found(null, it.latitude, it.longitude, LocationSource.Network, now) }
        val geo = ipGeo() ?: return Resolution.NotFound
        if (!lookupZoneMatches(geo.timezone, deviceZone(), now)) return Resolution.LooksWrong
        return found(geo.city, geo.latitude, geo.longitude, LocationSource.Internet, now)
    }

    private fun found(name: String?, lat: Double, lon: Double, source: LocationSource, now: Long) =
        Resolution.Found(Located(name, roundCoord(lat), roundCoord(lon), source, now))
}
