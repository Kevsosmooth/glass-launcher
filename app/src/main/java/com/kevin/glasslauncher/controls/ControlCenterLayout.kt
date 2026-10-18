package com.kevin.glasslauncher.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.home.HomeLayout
import com.kevin.glasslauncher.home.StatusCorner
import com.kevin.glasslauncher.motion.Motion
import com.kevin.glasslauncher.weather.rememberWeatherNow

private val DropIn = 16.dp
private val CornerGap = 16.dp

/**
 * Quick Controls without a panel, like tvOS Control Center: the status corner, its button white,
 * in the same place as on home, and [content] right-aligned under it, fading and dropping in.
 * Selecting the corner button closes it. D-pad focus cannot leave, so home underneath is
 * unreachable; [content] gets a bounded height and scrolls itself. The animation only changes
 * the layer; nothing recomposes.
 */
@Composable
fun ControlCenterLayout(
    tag: String,
    clock24: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, Motion.panelSpring) }
    Box(
        modifier.fillMaxSize().padding(top = HomeLayout.StatusInset, end = HomeLayout.SidePadding),
        contentAlignment = Alignment.TopEnd,
    ) {
        Column(
            Modifier
                .graphicsLayer {
                    alpha = shown.value.coerceIn(0f, 1f)
                    translationY = (shown.value - 1f) * DropIn.toPx()
                }
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup(),
            horizontalAlignment = Alignment.End,
        ) {
            StatusCorner(clock24, active = true, onOpen = onClose, weather = rememberWeatherNow())
            Box(Modifier.padding(top = CornerGap).weight(1f, fill = false).testTag(tag)) { content() }
        }
    }
}
