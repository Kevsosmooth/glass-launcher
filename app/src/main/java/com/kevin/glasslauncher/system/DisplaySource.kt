package com.kevin.glasslauncher.system

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

/** One resolution and every refresh rate the TV offers at it. */
data class DisplayModeGroup(val width: Int, val height: Int, val rates: List<Float>)

data class DisplayState(
    val width: Int,
    val height: Int,
    val refreshHz: Float,
    val modes: List<DisplayModeGroup>,
    val hdr: List<String>,
    val wideColor: Boolean,
) {
    /** "4K 60 Hz". */
    val summary: String get() = "${resolutionName(width, height)} ${formatRefresh(refreshHz)}"
}

/** Modes grouped by resolution, largest first, rates ascending and de-duplicated to 0.01 Hz. */
fun groupModes(modes: List<Triple<Int, Int, Float>>): List<DisplayModeGroup> =
    modes.groupBy { (w, h, _) -> w to h }
        .map { (size, same) ->
            val rates = same.map { Math.round(it.third * 100) / 100f }.distinct().sorted()
            DisplayModeGroup(size.first, size.second, rates)
        }
        .sortedByDescending { it.width * it.height }

/**
 * The HDMI output: current mode, every mode the TV reports, HDR formats. Changing the resolution
 * or refresh rate system-wide is a system setting (an app can only ask for a mode for its own window).
 */
class DisplaySource(context: Context) {
    private val displays = context.applicationContext.getSystemService(DisplayManager::class.java)

    fun state(): DisplayState? {
        val display = displays?.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
        val mode = display.mode
        return DisplayState(
            width = mode.physicalWidth,
            height = mode.physicalHeight,
            refreshHz = mode.refreshRate,
            modes = groupModes(display.supportedModes.map { Triple(it.physicalWidth, it.physicalHeight, it.refreshRate) }),
            // Display.hdrCapabilities is deprecated in API 34 in favour of Mode.supportedHdrTypes (API 34).
            hdr = hdrNames(display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)),
            wideColor = display.isWideColorGamut,
        )
    }
}
