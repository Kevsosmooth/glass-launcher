package com.kevin.glasslauncher.system

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.DetailRow
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.SectionHeader
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow

private const val POLL_MS = 3_000L

private data class BluetoothView(val available: Boolean, val permission: Boolean, val enabled: Boolean, val devices: List<PairedDevice>)

/**
 * Paired remotes, controllers, keyboards and audio devices with their connection state, like tvOS
 * Settings > Remotes and Devices. Pairing and removing devices open Android's accessory screen.
 */
@Composable
fun ColumnScope.BluetoothPage() {
    val context = LocalContext.current
    val source = remember(context) { BluetoothSource(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val ask = rememberPermissionRequest(source.permission) { refresh++ }
    val view by rememberPolled(refresh, POLL_MS, null) {
        BluetoothView(source.available, source.hasPermission, source.enabled, source.paired())
    }

    SystemPage(stringResource(R.string.system_bluetooth)) {
        val current = view
        when {
            current == null -> TextBlock(stringResource(R.string.system_loading), Modifier.focusOnShow())
            !current.available -> TextBlock(stringResource(R.string.system_bt_none), Modifier.focusOnShow())
            !current.permission -> ListRow(stringResource(R.string.system_bt_allow), null, ask, Modifier.focusOnShow(), chevron = true)
            !current.enabled -> TextBlock(stringResource(R.string.system_bt_off), Modifier.focusOnShow())
            else -> {
                SectionHeader(stringResource(R.string.system_bt_paired))
                if (current.devices.isEmpty()) TextBlock(stringResource(R.string.system_bt_no_devices), Modifier.focusOnShow())
                current.devices.forEachIndexed { i, device ->
                    val state = stringResource(if (device.connected) R.string.system_bt_connected else R.string.system_bt_not_connected)
                    DetailRow(
                        device.name,
                        "${stringResource(kindName(device.kind))} · $state",
                        if (i == 0) Modifier.focusOnShow() else Modifier,
                    )
                }
            }
        }
        PageGap()
        NativeLinkRow(stringResource(R.string.system_bt_pair), NativePage.Bluetooth)
    }
}

private fun kindName(kind: DeviceKind): Int = when (kind) {
    DeviceKind.Remote -> R.string.system_kind_remote
    DeviceKind.Gamepad -> R.string.system_kind_gamepad
    DeviceKind.Keyboard -> R.string.system_kind_keyboard
    DeviceKind.Audio -> R.string.system_kind_audio
    DeviceKind.Other -> R.string.system_kind_other
}
