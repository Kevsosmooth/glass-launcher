package com.kevin.glasslauncher.update

/**
 * What Select on Settings' "Check for updates" row does: install the update Glass already knows
 * about (also a retry after a failure), otherwise check GitHub now. False when the TV has no
 * Install unknown apps screen to grant the install permission.
 */
fun UpdateController.checkOrInstall(): Boolean {
    val current = state.value
    return when {
        current.isBusy -> true
        current is UpdateState.Available || current.cardRelease() != null -> requestUpdate()
        else -> {
            checkNow()
            true
        }
    }
}
