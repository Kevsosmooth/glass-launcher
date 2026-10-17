package com.kevin.glasslauncher.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import com.kevin.glasslauncher.apps.AppEntry
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.motion.wiggle
import com.kevin.glasslauncher.ui.TestTags

/**
 * Glass slab with the favourites. The slab reaches [HomeLayout.DockPadSide] past the grid
 * columns on each side and pads its tiles by the same amount, so dock tiles sit exactly on the
 * grid columns, empty slots stay empty, and a focused tile (scaled, shadowed) stays inside the
 * slab. The [TestTags.DOCK] node is the visible slab. [movingKey] wiggles while it is moved.
 */
@Composable
fun Dock(
    apps: List<AppEntry>,
    env: TileEnv,
    focus: DockFocus,
    movingKey: String?,
    onFocusEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(focus.wantFirst, apps.isNotEmpty()) {
        if (focus.wantFirst && apps.isNotEmpty()) {
            first.requestFocus()
            focus.wantFirst = false
        }
    }
    Row(
        modifier
            .outsetWidth()
            .testTag(TestTags.DOCK)
            .glass(HomeLayout.DockCorner)
            .onFocusChanged {
                if (it.hasFocus) onFocusEnter() else focus.report(null)
            }
            .focusGroup()
            .padding(
                start = HomeLayout.DockPadSide,
                end = HomeLayout.DockPadSide,
                top = HomeLayout.DockPadTop,
                bottom = HomeLayout.DockPadBottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(HomeLayout.TileGap),
    ) {
        apps.forEachIndexed { index, app ->
            key(app.key) {
                AppTile(
                    app, env, TilePlace.Dock,
                    Modifier
                        .weight(1f)
                        .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                        .then(if (app.key == movingKey) Modifier.wiggle() else Modifier)
                        .onFocusChanged { if (it.isFocused) focus.report(app) },
                )
            }
        }
        repeat(HomeLayout.COLUMNS - apps.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** Measures [HomeLayout.DockPadSide] wider than the grid content on both sides, centred on it. */
private fun Modifier.outsetWidth(): Modifier = layout { measurable, constraints ->
    val outset = HomeLayout.DockPadSide.roundToPx()
    val width = constraints.maxWidth + 2 * outset
    val placeable = measurable.measure(
        Constraints(width, width, constraints.minHeight, constraints.maxHeight),
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-outset, 0) }
}
