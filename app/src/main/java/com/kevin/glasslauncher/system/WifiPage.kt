package com.kevin.glasslauncher.system

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PanelRow
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.SectionHeader
import com.kevin.glasslauncher.settings.ui.SignalBars
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow
import kotlinx.coroutines.delay

private const val STATUS_POLL_MS = 3_000L
private const val SCAN_EVERY_MS = 15_000L

private data class WifiView(
    val status: NetworkStatus?,
    val nearby: List<NearbyNetwork>,
    val permission: Boolean,
    val locationOn: Boolean,
)

/**
 * The current connection (name, signal, band, speed, IP) and nearby networks, like tvOS
 * Settings > Network. Joining, forgetting and the Wi-Fi switch are Android's: picking a network
 * opens Android's Wi-Fi screen, which asks for the password.
 */
@Composable
fun ColumnScope.WifiPage() {
    val context = LocalContext.current
    val source = remember(context) { WifiSource(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val askLocation = rememberPermissionRequest(android.Manifest.permission.ACCESS_FINE_LOCATION) { refresh++ }
    val view by rememberPolled(refresh, STATUS_POLL_MS, WifiView(null, emptyList(), source.hasLocationPermission, true)) {
        val status = source.status()
        WifiView(status, nearbyNetworks(source.scanResults(), status.ssid), source.hasLocationPermission, source.locationOn)
    }
    val canScan = view.permission
    LaunchedEffect(canScan) {
        while (canScan) {
            source.requestScan()
            delay(SCAN_EVERY_MS)
        }
    }

    SystemPage(stringResource(R.string.system_wifi)) {
        val status = view.status
        if (status == null) {
            TextBlock(stringResource(R.string.system_loading), Modifier.focusOnShow())
            return@SystemPage
        }
        ConnectionRows(status, Modifier.focusOnShow())
        when {
            !status.wifiEnabled && status.connection != Connection.Wifi -> Unit
            !view.permission -> {
                SectionHeader(stringResource(R.string.system_wifi_networks))
                ListRow(stringResource(R.string.system_wifi_allow), null, askLocation, chevron = true)
                TextBlock(stringResource(R.string.system_wifi_allow_hint))
            }
            !view.locationOn -> {
                SectionHeader(stringResource(R.string.system_wifi_networks))
                NativeLinkRow(stringResource(R.string.system_wifi_location_off), NativePage.Location)
            }
            else -> {
                SectionHeader(stringResource(R.string.system_wifi_networks))
                if (view.nearby.isEmpty()) TextBlock(stringResource(R.string.system_wifi_searching))
                view.nearby.forEach { network -> NetworkRow(network) { NativePage.Wifi.open(context) } }
            }
        }
        PageGap()
        NativeLinkRow(stringResource(R.string.system_wifi_manage), NativePage.Wifi)
    }
}

@Composable
private fun ConnectionRows(status: NetworkStatus, first: Modifier) {
    when (status.connection) {
        Connection.Ethernet -> ListRow(stringResource(R.string.system_ethernet), stringResource(R.string.system_wifi_connected), null, first)
        Connection.Wifi -> {
            val name = status.ssid ?: stringResource(R.string.system_wifi_this_network)
            val bars = status.rssi?.let(::signalBars)
            CurrentNetworkRow(name, bars, first)
            bars?.let { ListRow(stringResource(R.string.system_wifi_signal), stringResource(signalName(it)), null) }
            wifiBand(status.frequencyMhz)?.let { ListRow(stringResource(R.string.system_wifi_band), it, null) }
            status.linkMbps?.let { ListRow(stringResource(R.string.system_wifi_speed), stringResource(R.string.system_wifi_speed_value, it), null) }
        }
        Connection.Other, Connection.None -> {
            val text = if (status.wifiEnabled) R.string.system_wifi_not_connected else R.string.system_wifi_off
            ListRow(stringResource(R.string.system_wifi), stringResource(text), null, first)
        }
    }
    status.ip?.let { ListRow(stringResource(R.string.system_ip), it, null) }
}

@Composable
private fun CurrentNetworkRow(name: String, bars: Int?, modifier: Modifier) {
    PanelRow(onClick = null, modifier) { focused ->
        BasicText(
            name,
            Modifier.weight(1f),
            style = PanelStyle.on(PanelStyle.Row, if (focused) PanelStyle.FocusedText else PanelStyle.Text, focused),
            maxLines = 1,
        )
        BasicText(
            "✓",
            style = PanelStyle.on(PanelStyle.Row, if (focused) PanelStyle.FocusedText else PanelStyle.SwitchOn, focused),
        )
        bars?.let { SignalBars(it, focused) }
    }
}

@Composable
private fun NetworkRow(network: NearbyNetwork, onClick: () -> Unit) {
    val detail = listOfNotNull(
        stringResource(if (network.secure) R.string.system_wifi_secured else R.string.system_wifi_open),
        wifiBand(network.frequencyMhz),
    ).joinToString(" · ")
    PanelRow(onClick) { focused ->
        BasicText(
            network.ssid,
            Modifier.weight(1f),
            style = PanelStyle.on(PanelStyle.Row, if (focused) PanelStyle.FocusedText else PanelStyle.Text, focused),
            maxLines = 1,
        )
        BasicText(detail, style = PanelStyle.on(PanelStyle.Value, if (focused) PanelStyle.FocusedText else PanelStyle.SecondaryText, focused))
        SignalBars(signalBars(network.rssi), focused)
    }
}

private fun signalName(bars: Int): Int = when (bars) {
    4 -> R.string.system_wifi_signal_4
    3 -> R.string.system_wifi_signal_3
    2 -> R.string.system_wifi_signal_2
    1 -> R.string.system_wifi_signal_1
    else -> R.string.system_wifi_signal_0
}
