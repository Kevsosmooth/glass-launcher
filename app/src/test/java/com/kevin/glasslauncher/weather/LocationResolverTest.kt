package com.kevin.glasslauncher.weather

import com.kevin.glasslauncher.data.City
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocationResolverTest {
    private val now = 1_800_000_000_000L
    private val newYork = TimeZone.getTimeZone("America/New_York")
    private var networkAsked = false
    private var ipAsked = false

    private fun resolver(network: Coordinates?, geo: IpGeo?) = LocationResolver(
        network = { networkAsked = true; network },
        ipGeo = { ipAsked = true; geo },
        deviceZone = { newYork },
        clock = { now },
    )

    private val brooklyn = IpGeo("Brooklyn", 40.65, -73.95, "America/New_York")

    @Test
    fun pickedCityWinsWithoutAskingAnything() = runBlocking {
        val city = City("Paris, Île-de-France, France", 48.85, 2.35)
        val result = resolver(Coordinates(1.0, 2.0), brooklyn).resolve(city, useDeviceLocation = true)
        assertEquals(Resolution.Found(Located(city.name, 48.85, 2.35, LocationSource.Picked, now)), result)
        assertFalse(networkAsked)
        assertFalse(ipAsked)
    }

    @Test
    fun networkLocationBeatsTheInternetLookupAndIsRounded() = runBlocking {
        val result = resolver(Coordinates(40.712776, -74.005974), brooklyn).resolve(null, useDeviceLocation = true)
        assertEquals(Resolution.Found(Located(null, 40.71, -74.01, LocationSource.Network, now)), result)
        assertFalse(ipAsked)
    }

    @Test
    fun internetLookupIsUsedWhenItsZoneMatches() = runBlocking {
        val result = resolver(null, brooklyn).resolve(null, useDeviceLocation = true)
        assertEquals(Resolution.Found(Located("Brooklyn", 40.65, -73.95, LocationSource.Internet, now)), result)
    }

    @Test
    fun deviceLocationIsSkippedUntilTheUserOptsIn() = runBlocking {
        // The fake network locator answers, as it would with the permission granted.
        val result = resolver(Coordinates(40.712776, -74.005974), brooklyn).resolve(null, useDeviceLocation = false)
        assertEquals(Resolution.Found(Located("Brooklyn", 40.65, -73.95, LocationSource.Internet, now)), result)
        assertFalse(networkAsked)
    }

    @Test
    fun internetLookupInAnotherZoneLooksWrong() = runBlocking {
        val amsterdam = IpGeo("Amsterdam", 52.37, 4.89, "Europe/Amsterdam")
        assertEquals(Resolution.LooksWrong, resolver(null, amsterdam).resolve(null, useDeviceLocation = false))
    }

    @Test
    fun nothingFoundWhenEveryLookupFails() = runBlocking {
        assertEquals(Resolution.NotFound, resolver(null, null).resolve(null, useDeviceLocation = true))
    }
}
