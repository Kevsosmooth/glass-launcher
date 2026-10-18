package com.kevin.glasslauncher.update

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.settings.ui.PanelStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val CardCorner = 16.dp
private val ButtonHeight = 44.dp
private val ButtonCorner = ButtonHeight / 2
private val ButtonMinWidth = 96.dp
private val NotesMaxHeight = 140.dp
private val NotesCorner = 8.dp
private val NotesScrollStep = 60.dp
private val NotesFocusRing = Color(0x99FFFFFF)

/**
 * "Update available" card: version, What's new (release notes, scrollable with the D-pad), then
 * Update and Later. While an update runs the main button shows its progress and does nothing.
 * Draws nothing unless [cardRelease] has a release for [state]. The caller sets the width.
 */
@Composable
fun UpdateCard(state: UpdateState, onUpdate: () -> Unit, onLater: () -> Unit, modifier: Modifier = Modifier) {
    val release = state.cardRelease() ?: return
    val busy = state.isBusy
    Column(modifier.glass(CardCorner).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BasicText(
            stringResource(R.string.update_available_title, release.version),
            style = PanelStyle.Row.copy(fontWeight = FontWeight.SemiBold, color = PanelStyle.Text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BasicText(stringResource(R.string.update_whats_new), style = PanelStyle.Value.copy(color = PanelStyle.SecondaryText))
        ReleaseNotes(release.notes.ifBlank { stringResource(R.string.update_no_notes) })
        statusMessage(state)?.let { BasicText(it, style = PanelStyle.Value.copy(color = PanelStyle.Text)) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardButton(updateButtonLabel(state), onClick = if (busy) ({}) else onUpdate)
            if (!busy) CardButton(stringResource(R.string.update_later), onClick = onLater)
        }
    }
}

/** Why the card is waiting: a failure, or Android's install permission. */
@Composable
private fun statusMessage(state: UpdateState): String? = when (state) {
    is UpdateState.Failed -> stringResource(state.reason.messageRes)
    is UpdateState.NeedsPermission -> stringResource(permissionHintRes(LocalContext.current))
    else -> null
}

/** Plain-text notes in a bounded box; focus it and Up/Down scroll until an end, then focus moves on. */
@Composable
private fun ReleaseNotes(notes: String) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { NotesScrollStep.toPx() }
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = NotesMaxHeight)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { scrollOnKey(it, scroll, step, scope) }
            .focusable()
            .then(if (focused) Modifier.border(1.dp, NotesFocusRing, RoundedCornerShape(NotesCorner)) else Modifier)
            .verticalScroll(scroll)
            .padding(8.dp),
    ) {
        BasicText(notes, style = PanelStyle.Body.copy(color = PanelStyle.Text))
    }
}

/** Consumes Up/Down while the notes can still scroll that way, so focus only leaves at an end. */
private fun scrollOnKey(event: KeyEvent, scroll: ScrollState, step: Float, scope: CoroutineScope): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val delta = when {
        event.key == Key.DirectionDown && scroll.canScrollForward -> step
        event.key == Key.DirectionUp && scroll.canScrollBackward -> -step
        else -> return false
    }
    scope.launch { scroll.animateScrollBy(delta) }
    return true
}

/** Capsule button styled like the Quick Controls tiles: glass, solid white with dark text when focused. */
@Composable
private fun CardButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val color = if (focused) PanelStyle.FocusedText else PanelStyle.Text
    Box(
        Modifier
            .heightIn(min = ButtonHeight)
            .widthIn(min = ButtonMinWidth)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = null, indication = null, role = Role.Button, onClick = onClick)
            .then(
                if (focused) {
                    Modifier.background(PanelStyle.FocusedSurface, RoundedCornerShape(ButtonCorner))
                } else {
                    Modifier.glass(ButtonCorner, nested = true)
                },
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = PanelStyle.on(PanelStyle.Row, color, focused), maxLines = 1)
    }
}
