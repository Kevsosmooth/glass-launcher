package com.kevin.glasslauncher.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.CodeBlock
import com.kevin.glasslauncher.settings.ui.PanelStyle

/** A short note shown inside Quick Controls instead of a crash or a silent failure. */
enum class ControlsMessage { CannotOpen, AdminNeeded }

@Composable
fun ControlsMessageView(message: ControlsMessage, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 12.dp)) {
        val text = when (message) {
            ControlsMessage.CannotOpen -> R.string.panels_cannot_open
            ControlsMessage.AdminNeeded -> R.string.panels_admin_needed
        }
        BasicText(
            stringResource(text),
            Modifier.padding(horizontal = 12.dp),
            style = PanelStyle.Body.copy(color = PanelStyle.Text),
        )
        // Focusable, so the D-pad can scroll the command into view on a short panel.
        if (message == ControlsMessage.AdminNeeded) CodeBlock(stringResource(R.string.panels_admin_cmd))
    }
}
