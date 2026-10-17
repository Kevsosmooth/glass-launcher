package com.kevin.glasslauncher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SystemFormatTest {
    @Test
    fun bytesUseDecimalUnits() {
        assertEquals("999 B", formatBytes(999))
        assertEquals("2 KB", formatBytes(1_500))
        assertEquals("512 MB", formatBytes(512_000_000))
        assertEquals("12.3 GB", formatBytes(12_345_000_000))
        assertEquals("128 GB", formatBytes(128_000_000_000))
        assertEquals("2.0 TB", formatBytes(2_000_000_000_000))
    }

    @Test
    fun wifiBandsFromFrequency() {
        assertEquals("2.4 GHz", wifiBand(2437))
        assertEquals("5 GHz", wifiBand(5180))
        assertEquals("6 GHz", wifiBand(5955))
        assertNull(wifiBand(null))
        assertNull(wifiBand(60_000))
    }

    @Test
    fun signalBarsFollowAndroidThresholds() {
        assertEquals(4, signalBars(-40))
        assertEquals(4, signalBars(-55))
        assertEquals(3, signalBars(-60))
        assertEquals(2, signalBars(-70))
        assertEquals(1, signalBars(-85))
        assertEquals(0, signalBars(-95))
    }

    @Test
    fun ssidLosesQuotesAndHiddenPlaceholder() {
        assertEquals("Home 5G", cleanSsid("\"Home 5G\""))
        assertNull(cleanSsid("<unknown ssid>"))
        assertNull(cleanSsid("\"\""))
        assertNull(cleanSsid(null))
    }

    @Test
    fun uptimeIsShort() {
        assertEquals("7 min", formatUptime(7 * 60_000L))
        assertEquals("5 h 12 min", formatUptime((5 * 60 + 12) * 60_000L))
        assertEquals("3 d 4 h", formatUptime(((3 * 24 + 4) * 60 + 5) * 60_000L))
    }

    @Test
    fun resolutionsHaveTvNames() {
        assertEquals("4K", resolutionName(3840, 2160))
        assertEquals("1080p", resolutionName(1920, 1080))
        assertEquals("720p", resolutionName(1280, 720))
        assertEquals("8K", resolutionName(7680, 4320))
        assertEquals("1080p", resolutionName(1080, 1920))
        assertEquals("720 x 480", resolutionName(720, 480))
    }

    @Test
    fun modeNamesDoNotRepeatTheSize() {
        assertEquals("4K (3840 x 2160)", modeName(3840, 2160))
        assertEquals("720 x 480", modeName(720, 480))
    }

    @Test
    fun refreshRatesKeepUsefulDecimals() {
        assertEquals("60 Hz", formatRefresh(60f))
        assertEquals("59.94 Hz", formatRefresh(59.94006f))
        assertEquals("23.98 Hz", formatRefresh(23.976f))
        assertEquals("29.9 Hz", formatRefresh(29.9f))
    }

    @Test
    fun hdrAndAudioFormatNames() {
        assertEquals(listOf("Dolby Vision", "HDR10", "HDR10+"), hdrNames(intArrayOf(4, 2, 1, 2, 9)))
        assertEquals(
            listOf("Dolby Digital", "Dolby Digital Plus", "Dolby Atmos", "DTS"),
            audioFormatNames(intArrayOf(2, 7, 18, 6, 5)),
        )
        assertEquals(emptyList<String>(), audioFormatNames(intArrayOf()))
    }

    @Test
    fun channelLayoutsAndVolume() {
        assertNull(channelLayout(intArrayOf()))
        assertEquals("Stereo", channelLayout(intArrayOf(1, 2)))
        assertEquals("5.1", channelLayout(intArrayOf(2, 6)))
        assertEquals("7.1", channelLayout(intArrayOf(8)))
        assertEquals(0, volumePercent(3, 0))
        assertEquals(50, volumePercent(50, 100))
        assertEquals(47, volumePercent(7, 15))
    }
}
