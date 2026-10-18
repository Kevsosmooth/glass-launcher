package com.kevin.glasslauncher.settings.backup

import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.data.PrefsPatch
import com.kevin.glasslauncher.settings.UiText

/** Why an export or import did not happen. */
enum class BackupProblem {
    TooLarge, Malformed, WrongFormat, NewerVersion, NoSettings,
    WriteFailed, ReadFailed, NoStorage, PermissionDenied,
}

/** Where the Backup page is: idle, busy, waiting for the import confirm, or showing a result. */
sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data class Saved(val where: String) : BackupStatus
    data class Confirm(val patch: PrefsPatch, val skipped: Int) : BackupStatus
    data class Imported(val count: Int, val skipped: Int) : BackupStatus
    data class Failed(val problem: BackupProblem) : BackupStatus
}

fun BackupError.toProblem(): BackupProblem = when (this) {
    BackupError.TooLarge -> BackupProblem.TooLarge
    BackupError.Malformed -> BackupProblem.Malformed
    BackupError.WrongFormat -> BackupProblem.WrongFormat
    BackupError.NewerVersion -> BackupProblem.NewerVersion
    BackupError.NoSettings -> BackupProblem.NoSettings
}

/** What a decoded file leads to: the confirm step, or the reason it was refused. */
fun DecodedBackup.toStatus(): BackupStatus = when (this) {
    is DecodedBackup.Valid -> BackupStatus.Confirm(patch, skipped)
    is DecodedBackup.Rejected -> BackupStatus.Failed(error.toProblem())
}

/** The line shown under the rows, or null when there is nothing to say. */
fun BackupStatus.message(): UiText? = when (this) {
    BackupStatus.Idle -> null
    BackupStatus.Working -> UiText.Res(R.string.backup_working)
    is BackupStatus.Saved -> UiText.Res(R.string.backup_saved, listOf(where))
    is BackupStatus.Confirm -> UiText.Res(R.string.backup_confirm_detail, listOf(patch.size, skipped))
    is BackupStatus.Imported -> UiText.Res(R.string.backup_imported, listOf(count, skipped))
    is BackupStatus.Failed -> UiText.Res(problemText(problem))
}

private fun problemText(problem: BackupProblem): Int = when (problem) {
    BackupProblem.TooLarge -> R.string.backup_error_too_large
    BackupProblem.Malformed -> R.string.backup_error_malformed
    BackupProblem.WrongFormat -> R.string.backup_error_wrong_format
    BackupProblem.NewerVersion -> R.string.backup_error_newer
    BackupProblem.NoSettings -> R.string.backup_error_no_settings
    BackupProblem.WriteFailed -> R.string.backup_error_write
    BackupProblem.ReadFailed -> R.string.backup_error_read
    BackupProblem.NoStorage -> R.string.backup_error_no_storage
    BackupProblem.PermissionDenied -> R.string.backup_error_permission
}
