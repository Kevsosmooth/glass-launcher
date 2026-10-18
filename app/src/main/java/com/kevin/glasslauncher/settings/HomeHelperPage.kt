package com.kevin.glasslauncher.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.homebutton.HomeButtonService
import com.kevin.glasslauncher.settings.ui.CodeBlock
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow
import com.kevin.glasslauncher.system.NativeLinkRow
import com.kevin.glasslauncher.system.NativePage
import com.kevin.glasslauncher.system.PageGap
import com.kevin.glasslauncher.system.SystemPage
import com.kevin.glasslauncher.system.rememberPolled

private const val STATUS_POLL_MS = 2_000L

/**
 * Settings > Home button helper: whether the accessibility helper is on, a link to Android's
 * Accessibility screen to switch it, and the adb fallback for TVs that hide it there.
 */
@Composable
fun ColumnScope.HomeHelperPage() {
    val context = LocalContext.current
    val on by rememberPolled(Unit, STATUS_POLL_MS, false) { HomeButtonService.isOn(context) }
    SystemPage(stringResource(R.string.home_helper_title)) {
        TextBlock(stringResource(R.string.home_helper_intro), Modifier.focusOnShow())
        ListRow(
            stringResource(R.string.home_helper_status),
            stringResource(if (on) R.string.home_helper_on else R.string.home_helper_off),
            null,
        )
        NativeLinkRow(stringResource(R.string.home_helper_open), NativePage.Accessibility)
        TextBlock(stringResource(R.string.home_helper_steps))
        PageGap()
        TextBlock(stringResource(R.string.home_helper_adb))
        CodeBlock(stringResource(R.string.home_helper_adb_cmd))
        TextBlock(stringResource(R.string.home_helper_note))
    }
}
