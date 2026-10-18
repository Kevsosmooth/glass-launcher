package com.kevin.glasslauncher.settings.backup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.data.Prefs
import com.kevin.glasslauncher.data.PrefsPatch
import com.kevin.glasslauncher.data.toPatch
import com.kevin.glasslauncher.settings.resolve
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.TextBlock
import com.kevin.glasslauncher.settings.ui.focusOnShow
import com.kevin.glasslauncher.system.PageGap
import com.kevin.glasslauncher.system.SystemPage
import com.kevin.glasslauncher.system.rememberPermissionRequest
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Settings > Backup: export every launcher setting to a JSON file, or import one after a confirm
 * step. Uses Android's document picker when the TV has one, else Download/GlassLauncher (a file
 * list stands in for the picker; Back closes it). [startOnImport] focuses Import first.
 */
@Composable
fun ColumnScope.BackupPage(prefs: Prefs, restore: suspend (PrefsPatch) -> Unit, startOnImport: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestRestore by rememberUpdatedState(restore)
    val patch by rememberUpdatedState(prefs.toPatch())
    val storage = remember(context) { BackupStorage(context) }
    val controller = remember(storage) { BackupController(storage) { latestRestore(it) } }
    var focusImport by remember { mutableStateOf(startOnImport) }
    val perform: (suspend BackupController.() -> Unit) -> Unit = { step -> scope.launch { controller.step() } }

    val pickers = rememberBackupPickers(
        onCreated = { uri -> perform { exportTo(uri, patch) } },
        onOpened = { uri -> perform { load(uri) } },
    )
    val askWrite = rememberPermissionRequest(storage.writePermission) { granted ->
        if (granted) perform { exportToDownloads(backupFileName(LocalDate.now()), patch) } else controller.fail(BackupProblem.PermissionDenied)
    }
    val askRead = rememberPermissionRequest(storage.readPermission) { perform { openFiles() } }
    BackHandler(enabled = controller.files != null) { controller.closeFiles() }

    val files = controller.files
    if (files != null) {
        val allow = if (storage.isGranted(storage.readPermission)) null else askRead
        BackupFilesPage(files, allow) { file -> perform { load(file.uri) } }
        return
    }
    val busy = controller.status == BackupStatus.Working
    BackupMain(
        status = controller.status,
        focusImport = focusImport,
        onExport = export@{
            if (busy) return@export
            focusImport = false
            val name = backupFileName(LocalDate.now())
            when {
                pickers.create(name) -> Unit
                storage.isGranted(storage.writePermission) -> perform { exportToDownloads(name, patch) }
                else -> askWrite()
            }
        },
        onImport = import@{
            if (busy) return@import
            focusImport = true
            if (!pickers.open()) perform { openFiles() }
        },
        onConfirm = { perform { confirm() } },
        onCancel = controller::cancel,
    )
}

/** The two rows, the confirm step while an import waits, the last result, and a short hint. */
@Composable
private fun ColumnScope.BackupMain(
    status: BackupStatus,
    focusImport: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    SystemPage(stringResource(R.string.backup_title)) {
        // Focus moves to the confirm step and back to the row that started it when it goes away.
        val confirming = status is BackupStatus.Confirm
        val exportFocus = if (!confirming && !focusImport) Modifier.focusOnShow() else Modifier
        ListRow(stringResource(R.string.backup_export), null, onExport, exportFocus)
        val importFocus = if (!confirming && focusImport) Modifier.focusOnShow() else Modifier
        ListRow(stringResource(R.string.backup_import), null, onImport, importFocus)
        if (confirming) {
            TextBlock(stringResource(R.string.backup_confirm), Modifier.focusOnShow())
            status.message()?.let { TextBlock(it.resolve()) }
            ListRow(stringResource(R.string.backup_confirm_yes), null, onConfirm)
            ListRow(stringResource(R.string.backup_confirm_cancel), null, onCancel)
        } else {
            status.message()?.let { TextBlock(it.resolve()) }
        }
        PageGap()
        TextBlock(stringResource(R.string.backup_hint))
    }
}
