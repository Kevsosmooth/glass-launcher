package com.kevin.glasslauncher.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.settings.ui.ListRow
import com.kevin.glasslauncher.settings.ui.PageTitle
import com.kevin.glasslauncher.settings.ui.focusOnShow

/** The TV's pass-through inputs; Select switches to one. Back (handled by the panel) returns. */
@Composable
fun InputsList(inputs: List<PassthroughInput>, onPick: (PassthroughInput) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        PageTitle(stringResource(R.string.panels_inputs))
        inputs.forEachIndexed { i, input ->
            ListRow(input.label, null, { onPick(input) }, if (i == 0) Modifier.focusOnShow() else Modifier)
        }
    }
}
