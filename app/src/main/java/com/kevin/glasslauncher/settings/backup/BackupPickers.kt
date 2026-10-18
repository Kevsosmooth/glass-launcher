package com.kevin.glasslauncher.settings.backup

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

// Backups copied onto a TV often lose their JSON type, so plain text and untyped files are offered too.
private val OpenTypes = arrayOf(BACKUP_MIME, "text/plain", "application/octet-stream")

/** Android's document pickers for a backup file. Each launch returns false when this TV has none. */
class BackupPickers internal constructor(
    private val canCreate: Boolean,
    private val canOpen: Boolean,
    private val createLauncher: ManagedActivityResultLauncher<String, Uri?>,
    private val openLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
) {
    /** Asks the user where to save [name]. */
    fun create(name: String): Boolean = canCreate && tryLaunch { createLauncher.launch(name) }

    /** Asks the user for a backup file to open. */
    fun open(): Boolean = canOpen && tryLaunch { openLauncher.launch(OpenTypes) }

    private fun tryLaunch(launch: () -> Unit): Boolean = try {
        launch()
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Pickers whose chosen file goes to [onCreated] / [onOpened]; Back out of a picker does nothing. */
@Composable
fun rememberBackupPickers(onCreated: (Uri) -> Unit, onOpened: (Uri) -> Unit): BackupPickers {
    val context = LocalContext.current
    val createContract = remember { ActivityResultContracts.CreateDocument(BACKUP_MIME) }
    val openContract = remember { ActivityResultContracts.OpenDocument() }
    val create = rememberLauncherForActivityResult(createContract) { uri -> uri?.let(onCreated) }
    val open = rememberLauncherForActivityResult(openContract) { uri -> uri?.let(onOpened) }
    return remember(context, create, open) {
        val packages = context.packageManager
        BackupPickers(
            canCreate = createContract.createIntent(context, backupFileName(LocalDate.now())).resolveActivity(packages) != null,
            canOpen = openContract.createIntent(context, OpenTypes).resolveActivity(packages) != null,
            createLauncher = create,
            openLauncher = open,
        )
    }
}
