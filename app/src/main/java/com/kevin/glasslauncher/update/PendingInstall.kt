package com.kevin.glasslauncher.update

import android.content.Intent

/** How long a committed install may wait for Android's answer before Glass gives up on it. */
const val INSTALL_TIMEOUT_MS = 10L * 60 * 1000

/**
 * The install session behind [UpdateState.Installing]: when it was handed to Android, and the
 * confirmation screen Android asked for (already made safe by [systemConfirmIntent]), kept so it
 * can be shown again. Android may block that screen when it is started while Glass is in the
 * background, which would otherwise leave the card stuck on Installing. [shown] is true once it
 * was started while Glass was in front: it really appeared, so it is not shown again (Fire OS
 * closes it on Install, before the install ends, which would bring it straight back).
 */
internal data class PendingInstall(val startedAt: Long, val confirm: Intent? = null, val shown: Boolean = false)

/** What returning to Glass does while an install is pending. */
internal sealed interface InstallResume {
    /** Android has not asked for confirmation yet, or the user has already seen it. */
    data object Wait : InstallResume

    /** Show Android's confirmation screen again. */
    data class Confirm(val intent: Intent) : InstallResume

    /** No answer for too long (or nothing to resume): abandon the session and offer Try again. */
    data object GiveUp : InstallResume
}

/** True once an install started at [startedAt] has waited past [INSTALL_TIMEOUT_MS], or the clock went back. */
fun isInstallStale(startedAt: Long, now: Long): Boolean = now - startedAt !in 0..INSTALL_TIMEOUT_MS

internal fun installResume(pending: PendingInstall?, now: Long): InstallResume {
    val confirm = pending?.confirm
    return when {
        pending == null || isInstallStale(pending.startedAt, now) -> InstallResume.GiveUp
        confirm != null && !pending.shown -> InstallResume.Confirm(confirm)
        else -> InstallResume.Wait
    }
}
