package com.kevin.glasslauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.GlassPanel
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.SettingsPanelWidth
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow

/** First-run card while Home still opens the TV's own home screen; shown once (LauncherRoot). */
@Composable
fun HomeSetupPanel(onSetUp: () -> Unit, onClose: () -> Unit) {
    GlassPanel(TestTags.HOME_SETUP, width = SettingsPanelWidth) {
        PageTitle(stringResource(R.string.home_setup_title))
        TextBlock(stringResource(R.string.home_setup_body))
        ListRow(stringResource(R.string.home_setup_go), null, onSetUp, Modifier.focusOnShow(), chevron = true)
        ListRow(stringResource(R.string.home_setup_later), null, onClose)
        TextBlock(stringResource(R.string.home_setup_where))
    }
}
