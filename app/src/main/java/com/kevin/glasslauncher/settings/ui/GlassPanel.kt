package com.kevin.glasslauncher.settings.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.glass.glass
import com.kevin.glasslauncher.motion.Motion
import kotlin.math.roundToInt

/** Quick Controls width: about a quarter of a 960 dp wide TV screen, like tvOS Control Center. */
val CompactPanelWidth = 300.dp

/** Settings width: room for a title, a value and a switch on one line. */
val SettingsPanelWidth = 340.dp

private val PanelMargin = 24.dp
private val PanelPadding = 12.dp

/**
 * Compact glass panel in the top-right corner that springs in from the right. It is only as tall
 * as its content (long pages scroll inside it, up to the screen height); give scrolling children
 * `Modifier.weight(1f, fill = false)` so they do not stretch it. D-pad focus cannot leave it, so
 * home underneath is unreachable while it is open. The slide only re-places the panel each frame;
 * nothing recomposes.
 */
@Composable
fun GlassPanel(
    tag: String,
    modifier: Modifier = Modifier,
    width: Dp = CompactPanelWidth,
    content: @Composable ColumnScope.() -> Unit,
) {
    val slide = remember { Animatable(1f) }
    LaunchedEffect(Unit) { slide.animateTo(0f, Motion.panelSpring) }
    Box(modifier.fillMaxSize().padding(PanelMargin), contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier
                .testTag(tag)
                .width(width)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        val hidden = placeable.width + PanelMargin.roundToPx()
                        placeable.place((slide.value * hidden).roundToInt(), 0)
                    }
                }
                .glass(PanelStyle.PanelCorner)
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
                .padding(PanelPadding),
            content = content,
        )
    }
}

/**
 * Focuses this element once it is shown, and again whenever [key] changes; then calls [onFocused]
 * (lazy lists use it to stop a recycled first row from taking focus back).
 */
@Composable
fun Modifier.focusOnShow(key: Any? = Unit, onFocused: () -> Unit = {}): Modifier {
    val requester = remember { FocusRequester() }
    LaunchedEffect(key) {
        // A request made before the first layout is dropped; wait one frame for the node to be placed.
        withFrameNanos { }
        requester.requestFocus()
        onFocused()
    }
    return focusRequester(requester)
}
