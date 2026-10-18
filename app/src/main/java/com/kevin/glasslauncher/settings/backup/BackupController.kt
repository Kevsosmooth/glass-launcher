package com.kevin.glasslauncher.settings.backup

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kevin.glasslauncher.data.PrefsPatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * State and steps of the Backup page: write a backup, read one into the confirm step, apply it.
 * File work runs on the IO dispatcher; [restore] writes the settings in one edit.
 */
class BackupController(
    private val storage: BackupStorage,
    private val restore: suspend (PrefsPatch) -> Unit,
) {
    var status: BackupStatus by mutableStateOf(BackupStatus.Idle)
        private set

    /** Backups found in Download/GlassLauncher while the file list is open; null when it is closed. */
    var files: List<BackupFile>? by mutableStateOf(null)
        private set

    suspend fun exportTo(uri: Uri, patch: PrefsPatch) = track {
        val ok = withContext(Dispatchers.IO) { storage.write(uri, encodeBackup(patch)) }
        if (!ok) return@track BackupStatus.Failed(BackupProblem.WriteFailed)
        val name = withContext(Dispatchers.IO) { storage.displayName(uri) }
        BackupStatus.Saved(name ?: uri.lastPathSegment.orEmpty())
    }

    suspend fun exportToDownloads(name: String, patch: PrefsPatch) = track {
        if (!storage.hasSharedStorage) return@track BackupStatus.Failed(BackupProblem.NoStorage)
        val path = withContext(Dispatchers.IO) { storage.saveToDownloads(name, encodeBackup(patch)) }
        if (path == null) BackupStatus.Failed(BackupProblem.WriteFailed) else BackupStatus.Saved(path)
    }

    /** Reads and checks [uri]; a valid file waits in [BackupStatus.Confirm] until [confirm]. */
    suspend fun load(uri: Uri) = track {
        files = null
        val bytes = withContext(Dispatchers.IO) { storage.read(uri) }
            ?: return@track BackupStatus.Failed(BackupProblem.ReadFailed)
        withContext(Dispatchers.Default) { decodeBackup(bytes) }.toStatus()
    }

    suspend fun confirm() {
        val pending = status as? BackupStatus.Confirm ?: return
        track {
            try {
                restore(pending.patch)
                BackupStatus.Imported(pending.patch.size, pending.skipped)
            } catch (e: IOException) {
                BackupStatus.Failed(BackupProblem.WriteFailed)
            }
        }
    }

    fun cancel() {
        if (status is BackupStatus.Confirm) status = BackupStatus.Idle
    }

    suspend fun openFiles() {
        if (!storage.hasSharedStorage) {
            fail(BackupProblem.NoStorage)
            return
        }
        files = withContext(Dispatchers.IO) { storage.listDownloads() }
    }

    fun closeFiles() {
        files = null
    }

    fun fail(problem: BackupProblem) {
        status = BackupStatus.Failed(problem)
    }

    /** Shows [BackupStatus.Working] while [step] runs, then the status it returns. */
    private suspend fun track(step: suspend () -> BackupStatus) {
        status = BackupStatus.Working
        status = step()
    }
}
