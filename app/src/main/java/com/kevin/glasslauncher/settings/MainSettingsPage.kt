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
import com.kevin.glasslauncher.settings.ui.ChoiceRow
import com.kevin.glasslauncher.settings.ui.DetailRow
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.SectionHeader
import com.kevin.glasslauncher.settings.ui.ToggleRow
import com.kevin.glasslauncher.settings.ui.focusOnShow

/**
 * The main Settings list. [focusId] gets focus when shown (the first row if null or absent) and
 * again whenever [focusKey] changes.
 */
@Composable
fun ColumnScope.MainSettingsPage(
    sections: List<SettingsSection>,
    focusId: SettingId?,
    focusKey: Int,
    onSelect: (SettingId) -> Unit,
) {
    val rows = sections.flatMap { it.rows }
    val target = rows.firstOrNull { it.id == focusId }?.id ?: rows.first().id
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState())) {
        PageTitle(stringResource(R.string.panels_settings_title))
        sections.forEach { section ->
            SectionHeader(stringResource(section.title))
            section.rows.forEach { row ->
                val focus = if (row.id == target) Modifier.focusOnShow(focusKey) else Modifier
                SettingsRowView(row, focus) { onSelect(row.id) }
            }
        }
    }
}

@Composable
private fun SettingsRowView(row: SettingsRow, modifier: Modifier, onSelect: () -> Unit) {
    val title = stringResource(row.title)
    when (val kind = row.kind) {
        is RowKind.Toggle -> ToggleRow(title, kind.on, onSelect, modifier)
        is RowKind.Choice -> ChoiceRow(title, kind.value.resolve(), onSelect, modifier)
        is RowKind.Link -> ListRow(title, kind.value?.resolve(), onSelect, modifier, chevron = true)
        RowKind.Action -> ListRow(title, null, onSelect, modifier)
        is RowKind.Info -> DetailRow(title, kind.value.resolve(), modifier)
    }
}
