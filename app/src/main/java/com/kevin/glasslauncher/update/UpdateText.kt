package com.kevin.glasslauncher.update

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import kotlin.math.roundToInt

private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"

/**
 * How to allow installs on this TV. Fire OS opens Developer options instead of Glass's own switch,
 * and its list can miss an app that gained the permission while Settings was running.
 */
@StringRes
fun permissionHintRes(context: Context): Int =
    if (context.packageManager.hasSystemFeature(FIRE_TV_FEATURE)) {
        R.string.update_needs_permission_fire
    } else {
        R.string.update_needs_permission
    }

/** The sentence shown for a failed check or update. */
@get:StringRes
val FailReason.messageRes: Int
    get() = when (this) {
        FailReason.Network -> R.string.update_failed_network
        FailReason.Server -> R.string.update_failed_server
        FailReason.BadRelease -> R.string.update_failed_bad_release
        FailReason.Blocked -> R.string.update_failed_blocked
        FailReason.TooLarge -> R.string.update_failed_too_large
        FailReason.Corrupt -> R.string.update_failed_corrupt
        FailReason.NotTrusted -> R.string.update_failed_not_trusted
        FailReason.NotNewer -> R.string.update_failed_not_newer
        FailReason.Cancelled -> R.string.update_failed_cancelled
        FailReason.Storage -> R.string.update_failed_storage
        FailReason.InstallFailed -> R.string.update_failed_install
    }

/** Label of the card's main button: Update, live progress, Allow installs, or Try again. */
@Composable
fun updateButtonLabel(state: UpdateState): String = when (state) {
    is UpdateState.Downloading -> stringResource(R.string.update_downloading, (state.progress * 100).roundToInt())
    is UpdateState.Verifying -> stringResource(R.string.update_verifying)
    is UpdateState.Installing -> stringResource(R.string.update_installing)
    is UpdateState.NeedsPermission -> stringResource(R.string.update_allow)
    is UpdateState.Failed -> stringResource(R.string.update_retry)
    else -> stringResource(R.string.update_button)
}

/** Value text for the Settings "Check for updates" row; null when there is nothing to say yet. */
@Composable
fun updateStatusText(state: UpdateState): String? = when (state) {
    UpdateState.Idle -> null
    UpdateState.Checking -> stringResource(R.string.update_checking)
    is UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
    is UpdateState.Failed -> stringResource(state.reason.messageRes)
    is UpdateState.Available -> stringResource(R.string.update_new_version, state.release.version)
    else -> updateButtonLabel(state)
}
