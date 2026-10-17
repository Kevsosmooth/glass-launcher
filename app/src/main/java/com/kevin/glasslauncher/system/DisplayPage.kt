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
import com.kevin.glasslauncher.settings.ui.SectionHeader
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow

private const val POLL_MS = 5_000L

/** Current resolution, refresh rate and HDR, plus every mode the TV reports. Changing them is Android's. */
@Composable
fun ColumnScope.DisplayPage() {
    val context = LocalContext.current
    val source = remember(context) { DisplaySource(context) }
    val state by rememberPolled(Unit, POLL_MS, null) { source.state() }

    SystemPage(stringResource(R.string.system_display)) {
        val display = state
        if (display == null) {
            TextBlock(stringResource(R.string.system_loading), Modifier.focusOnShow())
        } else {
            ListRow(
                stringResource(R.string.system_resolution),
                modeName(display.width, display.height),
                null,
                Modifier.focusOnShow(),
            )
            ListRow(stringResource(R.string.system_refresh), formatRefresh(display.refreshHz), null)
            ListRow(
                stringResource(R.string.system_hdr),
                display.hdr.joinToString(", ").ifEmpty { stringResource(R.string.system_hdr_none) },
                null,
            )
            ListRow(stringResource(R.string.system_wide_color), stringResource(if (display.wideColor) R.string.system_yes else R.string.system_no), null)
            if (display.modes.isNotEmpty()) {
                SectionHeader(stringResource(R.string.system_modes))
                display.modes.forEach { group ->
                    DetailRow(
                        modeName(group.width, group.height),
                        group.rates.joinToString(", ") { formatRefresh(it).removeSuffix(" Hz") } + " Hz",
                    )
                }
            }
        }
        PageGap()
        NativeLinkRow(stringResource(R.string.system_display_more), NativePage.Display)
    }
}
