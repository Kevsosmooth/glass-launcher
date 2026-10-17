package com.kevin.glasslauncher.system

import android.content.Context
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.DeviceSummary
import com.kevin.glasslauncher.settings.UiText

/** The Wi-Fi row's value: network name, or how the TV is (not) connected. */
fun wifiSummary(status: NetworkStatus): UiText = when (status.connection) {
    Connection.Wifi -> status.ssid?.let(UiText::Raw) ?: UiText.Res(R.string.system_wifi_connected)
    Connection.Ethernet -> UiText.Res(R.string.system_ethernet)
    Connection.Other -> UiText.Res(R.string.system_wifi_connected)
    Connection.None -> UiText.Res(if (status.wifiEnabled) R.string.system_wifi_not_connected else R.string.system_wifi_off)
}

/** The Sound row's value: "On your TV", "Muted" or the volume in percent. */
fun soundSummary(state: SoundState): UiText = when {
    state.onTv -> UiText.Res(R.string.system_volume_on_tv)
    state.muted -> UiText.Res(R.string.system_muted)
    else -> UiText.Res(R.string.system_volume_value, listOf(state.percent))
}

/** Reads the values beside the Device rows on the Settings main page. Call off the main thread. */
class DeviceSummaryReader(context: Context) {
    private val wifi = WifiSource(context)
    private val bluetooth = BluetoothSource(context)
    private val sound = SoundSource(context)
    private val display = DisplaySource(context)
    private val device = DeviceSource(context)

    fun read(): DeviceSummary = DeviceSummary(
        wifi = wifiSummary(wifi.status()),
        bluetooth = when {
            !bluetooth.available || !bluetooth.hasPermission -> null
            !bluetooth.enabled -> UiText.Res(R.string.system_bt_off)
            else -> UiText.Res(R.string.system_bt_count, listOf(bluetooth.paired().size))
        },
        sound = soundSummary(sound.state()),
        display = display.state()?.let { UiText.Raw(it.summary) },
        storage = device.storage().firstOrNull()?.let { UiText.Res(R.string.system_storage_free, listOf(formatBytes(it.free))) },
    )
}
