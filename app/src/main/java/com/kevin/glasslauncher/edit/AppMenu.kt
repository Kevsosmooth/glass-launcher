package com.kevin.glasslauncher.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.ui.TestTags
import kotlin.math.roundToInt

private val MenuWidth = 236.dp
private val MenuCorner = 22.dp
private val MenuPadding = 8.dp
private val TileGap = 16.dp
private val ScreenEdge = 24.dp

/**
 * Glass popup beside the long-pressed tile. Focus starts on the first enabled row and cannot
 * leave the menu; Back closes it. The Select still held from the long press is ignored.
 */
@Composable
fun AppMenu(target: MenuTarget, items: List<MenuItem>, onPick: (MenuAction) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val first = remember(target) { FocusRequester() }
    val guard = remember(target) { SelectGuard() }
    val firstEnabled = items.indexOfFirst { it.enabled }
    LaunchedEffect(target) {
        // A request made before the first layout is dropped; wait one frame for the rows.
        withFrameNanos { }
        first.requestFocus()
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .besideTile(target.tile.bounds)
                .width(MenuWidth)
                .testTag(TestTags.APP_MENU)
                .glass(MenuCorner)
                .selectGuard(guard)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
                .padding(MenuPadding),
        ) {
            items.forEachIndexed { index, item ->
                MenuRow(
                    item,
                    onClick = { onPick(item.action) },
                    modifier = if (index == firstEnabled) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
    }
}

/** Places the menu with [menuPosition] inside the full-screen box it is drawn in. */
private fun Modifier.besideTile(tile: Rect): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val position = menuPosition(
        tile,
        Size(placeable.width.toFloat(), placeable.height.toFloat()),
        Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
        gap = TileGap.toPx(),
        edge = ScreenEdge.toPx(),
    )
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(IntOffset(position.x.roundToInt(), position.y.roundToInt()))
    }
}
