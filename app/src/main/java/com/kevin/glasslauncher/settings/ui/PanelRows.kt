package com.kevin.glasslauncher.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.settings.ui.PanelStyle.FocusedText
import com.kevin.glasslauncher.settings.ui.PanelStyle.SecondaryText
import com.kevin.glasslauncher.settings.ui.PanelStyle.Text
import com.kevin.glasslauncher.settings.ui.PanelStyle.on

private val RowMinHeight = 40.dp

/**
 * One focusable row: glass (transparent) when idle, solid white with dark text when focused.
 * [onClick] null makes it focusable only (read-only rows still scroll into view with the D-pad).
 */
@Composable
fun PanelRow(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val action = if (onClick != null) {
        Modifier.clickable(interactionSource = null, indication = null, role = role, onClick = onClick)
    } else {
        Modifier.focusable()
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .onFocusChanged { focused = it.isFocused }
            .then(action)
            .then(if (focused) Modifier.background(PanelStyle.FocusedSurface, PanelStyle.RowShape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content(focused) }
}

/** Title with an optional value on the right; [chevron] marks rows that open a sub-page. */
@Composable
fun ListRow(
    title: String,
    value: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    chevron: Boolean = false,
) {
    PanelRow(onClick, modifier) { focused ->
        RowTitle(title, focused)
        val trailing = listOfNotNull(value, if (chevron) "›" else null).joinToString("  ")
        if (trailing.isNotEmpty()) {
            BasicText(trailing, style = on(PanelStyle.Value, if (focused) FocusedText else SecondaryText, focused))
        }
    }
}

/** Select cycles to the next value; the current one shows on the right. */
@Composable
fun ChoiceRow(title: String, value: String, onNext: () -> Unit, modifier: Modifier = Modifier) {
    ListRow(title, value, onNext, modifier)
}

/** On/off row with a switch on the right; Select flips it. */
@Composable
fun ToggleRow(title: String, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    PanelRow(
        onToggle,
        modifier.semantics { toggleableState = ToggleableState(checked) },
        role = Role.Switch,
    ) { focused ->
        RowTitle(title, focused)
        GlassSwitch(checked, focused)
    }
}

@Composable
private fun RowScope.RowTitle(title: String, focused: Boolean) {
    BasicText(
        title,
        Modifier.weight(1f),
        style = on(PanelStyle.Row, if (focused) FocusedText else Text, focused),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Small caps-style label above a group of rows. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text.uppercase(),
        modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
        style = PanelStyle.Header.copy(color = SecondaryText),
    )
}

/** Page heading at the top of a panel page. */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    BasicText(text, modifier.padding(start = 12.dp, top = 2.dp, bottom = 8.dp), style = PanelStyle.Title.copy(color = Text))
}

/** A focusable paragraph, so long help pages scroll with the D-pad. */
@Composable
fun TextBlock(text: String, modifier: Modifier = Modifier) {
    PanelRow(onClick = null, modifier) { focused ->
        BasicText(text, style = on(PanelStyle.Body, if (focused) FocusedText else Text, focused))
    }
}

/** A shell command in monospace on a dark filled surface (readable on the white focus too). */
@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    PanelRow(onClick = null, modifier) { _ ->
        BasicText(
            code,
            Modifier.fillMaxWidth().background(PanelStyle.CodeSurface, PanelStyle.CodeShape).padding(8.dp),
            style = PanelStyle.Code,
        )
    }
}

/** Read-only row with the [detail] under the title (long values such as a URL). */
@Composable
fun DetailRow(title: String, detail: String, modifier: Modifier = Modifier) {
    PanelRow(onClick = null, modifier) { focused ->
        Column(Modifier.weight(1f)) {
            BasicText(title, style = on(PanelStyle.Row, if (focused) FocusedText else Text, focused))
            BasicText(detail, style = on(PanelStyle.Value, if (focused) FocusedText else SecondaryText, focused))
        }
    }
}
