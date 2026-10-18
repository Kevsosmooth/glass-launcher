package com.kevin.glasslauncher.update

/** Why a check or an update did not finish. */
enum class FailReason {
    /** No connection, DNS failure or a timeout. */
    Network,

    /** GitHub answered with an error status (rate limit, outage). */
    Server,

    /** The release JSON could not be read or has no `glass-launcher.apk`. */
    BadRelease,

    /** A redirect left the GitHub allow-list, dropped HTTPS, or went round too often. */
    Blocked,

    /** The download is bigger than Glass accepts. */
    TooLarge,

    /** Size or SHA-256 does not match the release, or the file is not a readable APK. */
    Corrupt,

    /** Another app, or Glass signed with a different key. */
    NotTrusted,

    /** The APK is not newer than the installed Glass. */
    NotNewer,

    /** The user pressed Cancel on Android's install screen. */
    Cancelled,

    /** Not enough space to install. */
    Storage,

    /** Android's installer refused the APK for another reason. */
    InstallFailed,
}

/** A [FailReason] raised inside the update pipeline; the controller turns it into [UpdateState.Failed]. */
class UpdateException(val reason: FailReason) : Exception(reason.name)

/** Where the updater is; the card and the Settings row both read this. */
sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data class UpToDate(val checkedAt: Long) : UpdateState

    /** [dismissed]: the user pressed Later for this version; the card hides, Settings still shows it. */
    data class Available(val release: ReleaseInfo, val dismissed: Boolean) : UpdateState

    /** [progress] is 0..1. */
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState

    data class Verifying(val release: ReleaseInfo) : UpdateState

    /** Android has not allowed Glass to install apps yet (Install unknown apps). */
    data class NeedsPermission(val release: ReleaseInfo) : UpdateState

    /** Handed to Android's installer; its confirmation screen is showing or about to. */
    data class Installing(val release: ReleaseInfo) : UpdateState

    /** [release] is null when the check itself failed. */
    data class Failed(val reason: FailReason, val release: ReleaseInfo?) : UpdateState
}

/** The release the update card is about, or null when the card should not show. */
fun UpdateState.cardRelease(): ReleaseInfo? = when (this) {
    is UpdateState.Available -> release.takeUnless { dismissed }
    is UpdateState.Downloading -> release
    is UpdateState.Verifying -> release
    is UpdateState.NeedsPermission -> release
    is UpdateState.Installing -> release
    is UpdateState.Failed -> release
    UpdateState.Idle, UpdateState.Checking, is UpdateState.UpToDate -> null
}

/** True while a download, verification or install is under way (buttons must not restart it). */
val UpdateState.isBusy: Boolean
    get() = this is UpdateState.Downloading || this is UpdateState.Verifying || this is UpdateState.Installing
