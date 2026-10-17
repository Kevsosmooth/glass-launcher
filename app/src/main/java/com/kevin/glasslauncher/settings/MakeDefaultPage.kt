package com.kevin.glasslauncher.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.CodeBlock
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow

/**
 * How to make Glass the Home-button launcher on each kind of device, with the exact adb
 * commands. Every block is focusable so the D-pad scrolls the page.
 */
@Composable
fun ColumnScope.MakeDefaultPage() {
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        PageTitle(stringResource(R.string.panels_make_default))
        TextBlock(stringResource(R.string.panels_default_intro), Modifier.focusOnShow())
        TextBlock(stringResource(R.string.panels_default_google))
        CodeBlock(stringResource(R.string.panels_default_google_cmd))
        TextBlock(stringResource(R.string.panels_default_undo))
        CodeBlock(stringResource(R.string.panels_default_undo_cmd))
        TextBlock(stringResource(R.string.panels_default_projectivy))
        TextBlock(stringResource(R.string.panels_default_fire))
        TextBlock(stringResource(R.string.panels_default_script))
    }
}
