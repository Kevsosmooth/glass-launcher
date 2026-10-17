package com.kevin.glasslauncher.system

import android.media.AudioDeviceInfo
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemModelsTest {
    @Test
    fun nearbyKeepsStrongestPerNameAndSkipsConnectedAndHidden() {
        val scan = listOf(
            NearbyNetwork("Cafe", -80, 2437, secure = false),
            NearbyNetwork("Home", -40, 5180, secure = true),
            NearbyNetwork("Neighbor", -70, 2412, secure = true),
            NearbyNetwork("Neighbor", -50, 5200, secure = true),
            NearbyNetwork("", -30, 2412, secure = false),
            NearbyNetwork("apt 2", -70, 2412, secure = true),
        )
        val list = nearbyNetworks(scan, connectedSsid = "Home")
        assertEquals(listOf("Neighbor", "apt 2", "Cafe"), list.map { it.ssid })
        assertEquals(-50, list.first().rssi)
    }

    @Test
    fun securityFromCapabilities() {
        assertTrue(isSecure("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"))
        assertTrue(isSecure("[RSN-SAE-CCMP][ESS]"))
        assertTrue(isSecure("[WPA2-EAP-CCMP][ESS]"))
        assertFalse(isSecure("[ESS]"))
    }

    @Test
    fun deviceKindFromClassThenName() {
        assertEquals(DeviceKind.Audio, deviceKind(0x240404, "WH-1000XM4"))
        assertEquals(DeviceKind.Remote, deviceKind(0x050C, "SHIELD Remote"))
        assertEquals(DeviceKind.Gamepad, deviceKind(0x0508, "Pad"))
        assertEquals(DeviceKind.Keyboard, deviceKind(0x0540, "K380"))
        assertEquals(DeviceKind.Gamepad, deviceKind(0, "Xbox Wireless Controller"))
        assertEquals(DeviceKind.Remote, deviceKind(0, "Bluetooth Remote"))
        assertEquals(DeviceKind.Other, deviceKind(0, "Thing"))
    }

    @Test
    fun devicesSortConnectedFirstThenByName() {
        val sorted = sortDevices(
            listOf(
                PairedDevice("b pad", "1", DeviceKind.Gamepad, connected = false),
                PairedDevice("Remote", "2", DeviceKind.Remote, connected = true),
                PairedDevice("A buds", "3", DeviceKind.Audio, connected = false),
            ),
        )
        assertEquals(listOf("Remote", "A buds", "b pad"), sorted.map { it.name })
    }

    @Test
    fun modesGroupByResolutionLargestFirst() {
        val groups = groupModes(
            listOf(
                Triple(1920, 1080, 60f),
                Triple(3840, 2160, 59.94006f),
                Triple(3840, 2160, 23.976f),
                Triple(3840, 2160, 59.94f),
                Triple(1920, 1080, 50f),
            ),
        )
        assertEquals(listOf(3840 to 2160, 1920 to 1080), groups.map { it.width to it.height })
        assertEquals(listOf(23.98f, 59.94f), groups.first().rates)
        assertEquals(listOf(50f, 60f), groups.last().rates)
    }

    @Test
    fun outputTypesHaveNamesOrAreLeftOut() {
        assertEquals("HDMI", outputTypeName(AudioDeviceInfo.TYPE_HDMI))
        assertEquals("HDMI eARC", outputTypeName(29))
        assertEquals("Bluetooth", outputTypeName(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertNull(outputTypeName(AudioDeviceInfo.TYPE_TELEPHONY))
    }

    @Test
    fun phantomSpeakerHiddenOnlyNextToHdmi() {
        val speaker = AudioOutput("SHIELD", "TV speaker", emptyList(), null)
        val hdmi = AudioOutput("SHIELD", "HDMI", listOf("DTS"), "7.1")
        assertEquals(listOf(hdmi), withoutPhantomSpeaker(listOf(speaker, hdmi)))
        assertEquals(listOf(speaker), withoutPhantomSpeaker(listOf(speaker)))
    }

    @Test
    fun summariesForTheSettingsRows() {
        assertEquals(UiText.Raw("Home"), wifiSummary(NetworkStatus(Connection.Wifi, true, "Home")))
        assertEquals(UiText.Res(R.string.system_wifi_connected), wifiSummary(NetworkStatus(Connection.Wifi, true, null)))
        assertEquals(UiText.Res(R.string.system_ethernet), wifiSummary(NetworkStatus(Connection.Ethernet, false, null)))
        assertEquals(UiText.Res(R.string.system_wifi_off), wifiSummary(NetworkStatus(Connection.None, false, null)))
        assertEquals(UiText.Res(R.string.system_wifi_not_connected), wifiSummary(NetworkStatus(Connection.None, true, null)))
        assertEquals(UiText.Res(R.string.system_muted), soundSummary(SoundState(5, 10, muted = true, fixed = false, outputs = emptyList())))
        assertEquals(
            UiText.Res(R.string.system_volume_value, listOf(50)),
            soundSummary(SoundState(5, 10, muted = false, fixed = false, outputs = emptyList())),
        )
        assertEquals(
            UiText.Res(R.string.system_volume_on_tv),
            soundSummary(SoundState(15, 15, muted = true, fixed = false, outputs = emptyList(), onTv = true)),
        )
    }
}
