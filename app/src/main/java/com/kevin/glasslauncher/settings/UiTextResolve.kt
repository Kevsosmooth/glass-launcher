package com.kevin.glasslauncher.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** The text to draw for this [UiText]. */
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Res -> stringResource(id, *args.toTypedArray())
    is UiText.Raw -> text
}
