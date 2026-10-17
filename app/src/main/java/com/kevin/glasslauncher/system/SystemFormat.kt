package com.kevin.glasslauncher.system

import java.util.Locale
import kotlin.math.roundToInt

// Pure text and mapping helpers for the device pages, kept free of Android so they unit-test plainly.

private const val KB = 1000.0

/** Storage size the way Android Settings shows it (decimal units): "512 MB", "12.3 GB". */
fun formatBytes(bytes: Long): String {
    if (bytes < KB) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes / KB
    var unit = 0
    while (value >= KB && unit < units.lastIndex) {
        value /= KB
        unit++
    }
    val text = if (value >= 100 || unit < 2) value.roundToInt().toString() else String.format(Locale.US, "%.1f", value)
    return "$text ${units[unit]}"
}

/** "2.4 GHz", "5 GHz" or "6 GHz" for a Wi-Fi channel frequency; null when unknown. */
fun wifiBand(frequencyMhz: Int?): String? = when (frequencyMhz) {
    null -> null
    in 2400..2500 -> "2.4 GHz"
    in 4900..5900 -> "5 GHz"
    in 5925..7125 -> "6 GHz"
    else -> null
}

/** Signal strength as 0-4 bars, with the thresholds Android uses for its Wi-Fi icon. */
fun signalBars(rssi: Int): Int = when {
    rssi >= -55 -> 4
    rssi >= -66 -> 3
    rssi >= -77 -> 2
    rssi >= -88 -> 1
    else -> 0
}

/** The network name without Android's quotes; null for the hidden "<unknown ssid>" placeholder. */
fun cleanSsid(raw: String?): String? {
    val name = raw?.removeSurrounding("\"")?.trim()
    return if (name.isNullOrEmpty() || name == "<unknown ssid>") null else name
}

/** "3 d 4 h", "5 h 12 min" or "7 min". */
fun formatUptime(millis: Long): String {
    val minutes = millis / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "$days d ${hours % 24} h"
        hours > 0 -> "$hours h ${minutes % 60} min"
        else -> "$minutes min"
    }
}

/** "4K", "1080p", "720p" for the usual TV sizes, otherwise "W x H". */
fun resolutionName(width: Int, height: Int): String {
    val (w, h) = if (width >= height) width to height else height to width
    return when {
        w >= 7680 && h >= 4320 -> "8K"
        w >= 3840 && h >= 2160 -> "4K"
        w >= 2560 && h >= 1440 -> "1440p"
        w >= 1920 && h >= 1080 -> "1080p"
        w >= 1280 && h >= 720 -> "720p"
        else -> "$w x $h"
    }
}

/** "4K (3840 x 2160)", or just "720 x 480" when the size has no TV name. */
fun modeName(width: Int, height: Int): String {
    val name = resolutionName(width, height)
    val size = "$width x $height"
    return if (name == size || name == "$height x $width") size else "$name ($size)"
}

/** "60 Hz", "59.94 Hz", "23.98 Hz". */
fun formatRefresh(hz: Float): String {
    val rounded = (hz * 100).roundToInt() / 100.0
    val text = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.US, "%.2f", rounded).trimEnd('0')
    return "$text Hz"
}

/** Names of HDR formats from Display.HdrCapabilities types (1 Dolby Vision, 2 HDR10, 3 HLG, 4 HDR10+). */
fun hdrNames(types: IntArray): List<String> = types.distinct().sorted().mapNotNull {
    when (it) {
        1 -> "Dolby Vision"
        2 -> "HDR10"
        3 -> "HLG"
        4 -> "HDR10+"
        else -> null
    }
}

/** Surround formats from AudioFormat encodings, in a fixed order, without duplicates. */
fun audioFormatNames(encodings: IntArray): List<String> {
    val names = mapOf(
        5 to "Dolby Digital", // ENCODING_AC3
        6 to "Dolby Digital Plus", // ENCODING_E_AC3
        18 to "Dolby Atmos", // ENCODING_E_AC3_JOC
        14 to "Dolby TrueHD", // ENCODING_DOLBY_TRUEHD
        17 to "Dolby AC-4", // ENCODING_AC4
        7 to "DTS", // ENCODING_DTS
        8 to "DTS-HD", // ENCODING_DTS_HD
    )
    val present = encodings.toSet()
    return names.filterKeys { it in present }.values.toList()
}

/** "Stereo", "5.1" or "7.1" for the largest channel count an output takes; null when unknown. */
fun channelLayout(channelCounts: IntArray): String? = when (channelCounts.maxOrNull()) {
    null, 0 -> null
    1, 2 -> "Stereo"
    in 3..6 -> "5.1"
    else -> "7.1"
}

/** Percent of [max] for a volume step, 0-100. */
fun volumePercent(volume: Int, max: Int): Int = if (max <= 0) 0 else (volume * 100f / max).roundToInt().coerceIn(0, 100)
