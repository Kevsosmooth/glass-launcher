package com.kevin.glasslauncher.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.glasslauncher.R

private val RowHeight = 44.dp
private val RowShape = RoundedCornerShape(14.dp)
private val FocusedText = Color(0xFF111111)
private val DisabledText = Color(0x80FFFFFF)
private val RowText = TextStyle(
    color = Color.White,
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    shadow = Shadow(Color(0x66000000), Offset(0f, 1f), 3f),
)
private val NoteText = RowText.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = DisabledText)

/** One menu row: glass text, solid white when focused. Disabled rows can't take focus. */
@Composable
internal fun MenuRow(item: MenuItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val style = when {
        focused -> RowText.copy(color = FocusedText, shadow = null)
        item.enabled -> RowText
        else -> RowText.copy(color = DisabledText)
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(RowHeight)
            .onFocusChanged { focused = it.isFocused }
            .then(if (item.enabled) Modifier.clickable(interactionSource = null, indication = null, onClick = onClick) else Modifier)
            .then(if (focused) Modifier.background(Color.White, RowShape) else Modifier)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(stringResource(item.action.label()), style = style)
        item.slot?.note()?.let { BasicText(stringResource(it), style = NoteText) }
    }
}

private fun MenuAction.label(): Int = when (this) {
    MenuAction.Move -> R.string.home_menu_move
    MenuAction.RemoveFromDock -> R.string.home_menu_remove
    MenuAction.AddToDock -> R.string.home_menu_add
    MenuAction.Hide -> R.string.home_menu_hide
    MenuAction.AppInfo -> R.string.home_menu_info
    MenuAction.Uninstall -> R.string.home_menu_uninstall
}

private fun DockSlot.note(): Int? = when (this) {
    DockSlot.Full -> R.string.home_menu_dock_full
    DockSlot.InDock -> R.string.home_menu_in_dock
    DockSlot.Free -> null
}
