package com.kevin.glasslauncher.settings.backup

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow
import com.kevin.glasslauncher.system.PageGap
import com.kevin.glasslauncher.system.SystemPage

/**
 * Backups in Download/GlassLauncher, newest first, for TVs without a document picker. [onAllow]
 * (null when not needed) asks for storage access so files this install did not write show up too.
 */
@Composable
fun ColumnScope.BackupFilesPage(files: List<BackupFile>, onAllow: (() -> Unit)?, onPick: (BackupFile) -> Unit) {
    SystemPage(stringResource(R.string.backup_files_title)) {
        if (files.isEmpty()) TextBlock(stringResource(R.string.backup_files_none), Modifier.focusOnShow())
        files.forEachIndexed { index, file ->
            ListRow(file.name, null, { onPick(file) }, if (index == 0) Modifier.focusOnShow() else Modifier)
        }
        if (onAllow != null) {
            PageGap()
            ListRow(stringResource(R.string.backup_files_allow), null, onAllow, chevron = true)
            TextBlock(stringResource(R.string.backup_files_allow_hint))
        }
    }
}
