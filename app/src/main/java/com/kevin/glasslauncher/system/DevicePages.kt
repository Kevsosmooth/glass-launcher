package com.kevin.glasslauncher.system

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.DetailRow
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.MeterRow
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow

private const val STORAGE_POLL_MS = 10_000L
private const val ABOUT_POLL_MS = 30_000L

/** Used and free space on internal storage and any mounted USB drive or SD card. */
@Composable
fun ColumnScope.StoragePage() {
    val context = LocalContext.current
    val source = remember(context) { DeviceSource(context) }
    val volumes by rememberPolled(Unit, STORAGE_POLL_MS, null) { source.storage() }

    SystemPage(stringResource(R.string.system_storage)) {
        val list = volumes
        if (list == null) {
            TextBlock(stringResource(R.string.system_loading), Modifier.focusOnShow())
        } else {
            list.forEachIndexed { i, volume ->
                MeterRow(
                    volume.name,
                    stringResource(R.string.system_storage_value, formatBytes(volume.free), formatBytes(volume.total)),
                    volume.usedFraction,
                    if (i == 0) Modifier.focusOnShow() else Modifier,
                )
            }
        }
        PageGap()
        NativeLinkRow(stringResource(R.string.system_storage_more), NativePage.Storage)
    }
}

/** Device name, model, Android version and patch, memory, up time, network address. */
@Composable
fun ColumnScope.AboutPage() {
    val context = LocalContext.current
    val device = remember(context) { DeviceSource(context) }
    val wifi = remember(context) { WifiSource(context) }
    val about by rememberPolled(Unit, ABOUT_POLL_MS, null) { device.about() to wifi.status().ip }

    SystemPage(stringResource(R.string.system_about)) {
        val loaded = about
        if (loaded == null) {
            TextBlock(stringResource(R.string.system_loading), Modifier.focusOnShow())
        } else {
            val (info, ip) = loaded
            ListRow(stringResource(R.string.system_about_name), info.deviceName, null, Modifier.focusOnShow())
            ListRow(stringResource(R.string.system_about_model), info.model, null)
            ListRow(stringResource(R.string.system_about_android), info.android, null)
            ListRow(stringResource(R.string.system_about_patch), info.securityPatch, null)
            ListRow(stringResource(R.string.system_about_memory), formatBytes(info.memoryBytes), null)
            ListRow(stringResource(R.string.system_about_uptime), formatUptime(info.uptimeMillis), null)
            ip?.let { ListRow(stringResource(R.string.system_ip), it, null) }
            ListRow(stringResource(R.string.system_about_timezone), info.timeZone, null)
            DetailRow(stringResource(R.string.system_about_build), info.build)
        }
        PageGap()
        NativeLinkRow(stringResource(R.string.system_date_time), NativePage.DateTime)
        NativeLinkRow(stringResource(R.string.system_language), NativePage.Language)
        NativeLinkRow(stringResource(R.string.system_update), NativePage.Update)
        NativeLinkRow(stringResource(R.string.system_about_more), NativePage.About)
    }
}
