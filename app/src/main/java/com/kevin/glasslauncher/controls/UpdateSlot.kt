package com.kevin.glasslauncher.controls

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.update.UpdateCard
import com.kevin.glasslauncher.update.UpdateController

/** The "Update available" card above the controls; nothing when there is no update to show. */
@Composable
fun UpdateSlot(updates: UpdateController, onNoInstallScreen: () -> Unit) {
    val state by updates.state.collectAsStateWithLifecycle()
    UpdateCard(
        state,
        onUpdate = { if (!updates.requestUpdate()) onNoInstallScreen() },
        onLater = updates::dismiss,
        modifier = Modifier.width(ControlsWidth),
    )
}
