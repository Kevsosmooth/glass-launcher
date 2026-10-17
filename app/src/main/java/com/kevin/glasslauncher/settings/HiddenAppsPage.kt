package com.kevin.glasslauncher.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.PanelStyle
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.ToggleRow
import com.kevin.glasslauncher.settings.ui.focusOnShow

/**
 * Every installed app with a "hidden" switch, A-Z, under two shortcuts: keep only the dock apps
 * (a minimal home) or show everything again. [apps] is null while the list loads.
 */
@Composable
fun ColumnScope.HiddenAppsPage(
    apps: List<AppEntry>?,
    hidden: Set<String>,
    onToggle: (String) -> Unit,
    onKeepDockOnly: () -> Unit,
    onShowAll: () -> Unit,
) {
    val sorted = remember(apps) { apps?.sortedBy { it.label.lowercase() } }
    LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth()) {
        item(key = "title", contentType = "title") {
            PageTitle(stringResource(R.string.panels_hidden_apps))
            BasicText(
                stringResource(R.string.panels_hidden_hint),
                Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                style = PanelStyle.Body.copy(color = PanelStyle.SecondaryText),
            )
        }
        item(key = "dock_only", contentType = "action") {
            ListRow(stringResource(R.string.panels_hidden_dock_only), null, onKeepDockOnly, Modifier.focusOnShow())
        }
        item(key = "show_all", contentType = "action") {
            ListRow(stringResource(R.string.panels_hidden_show_all), null, onShowAll)
        }
        if (sorted == null) {
            item(key = "loading", contentType = "loading") {
                TextBlock(stringResource(R.string.panels_hidden_loading))
            }
        } else {
            items(sorted, key = { it.key }, contentType = { "app" }) { app ->
                ToggleRow(app.label, app.key in hidden, { onToggle(app.key) })
            }
        }
    }
}
