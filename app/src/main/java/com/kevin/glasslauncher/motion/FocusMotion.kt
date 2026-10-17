package com.kevin.glasslauncher.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

private val SheenColors = listOf(Color.Transparent, Color(0x55FFFFFF), Color.Transparent)
private const val SHEEN_BAND = 0.25f

/**
 * tvOS focus: spring scale + shadow lift, a tilt toward the D-pad direction and one sheen sweep.
 * Everything runs in the layer/draw phase of one node, so focus changes never recompose or
 * re-lay out. Put it before the focusable (e.g. `clickable`) in the chain.
 */
fun Modifier.focusMotion(tracker: DirectionTracker, shape: Shape): Modifier =
    this then FocusMotionElement(tracker, shape)

private data class FocusMotionElement(val tracker: DirectionTracker, val shape: Shape) :
    ModifierNodeElement<FocusMotionNode>() {
    override fun create() = FocusMotionNode(tracker, shape)

    override fun update(node: FocusMotionNode) {
        node.tracker = tracker
        node.shape = shape
    }
}

/** Tilt and sheen only ever run on a tile that got focus, so they are created on first focus. */
private class FocusFlourish {
    val tiltX = Animatable(0f)
    val tiltY = Animatable(0f)
    val sheen = Animatable(Motion.SHEEN_END)
}

private class FocusMotionNode(var tracker: DirectionTracker, var shape: Shape) :
    Modifier.Node(), LayoutModifierNode, DrawModifierNode, FocusEventModifierNode {
    private val progress = Animatable(0f)
    private var flourish: FocusFlourish? = null
    private var focused = false

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val p = progress.value
        val s = 1f + (Motion.FOCUS_SCALE - 1f) * p
        scaleX = s
        scaleY = s
        val f = flourish
        rotationX = f?.tiltX?.value ?: 0f
        rotationY = f?.tiltY?.value ?: 0f
        cameraDistance = Motion.CAMERA_DISTANCE * density
        shadowElevation = p * Motion.LIFT_ELEVATION.toPx()
        shape = this@FocusMotionNode.shape
        clip = true
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layerBlock) }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val s = flourish?.sheen?.value ?: return
        if (s <= Motion.SHEEN_START || s >= Motion.SHEEN_END) return
        val x = s * size.width
        val band = size.width * SHEEN_BAND
        drawRect(Brush.linearGradient(SheenColors, Offset(x - band, 0f), Offset(x + band, size.height * 0.4f)))
    }

    override fun onFocusEvent(focusState: FocusState) {
        val now = focusState.isFocused
        if (now == focused) return
        focused = now
        coroutineScope.launch { progress.animateTo(if (now) 1f else 0f, Motion.focusSpring) }
        if (!now) return
        val f = flourish ?: FocusFlourish().also { flourish = it }
        val (x, y) = tracker.tiltDegrees()
        coroutineScope.launch { f.tiltX.snapTo(x); f.tiltX.animateTo(0f, Motion.tiltSpring) }
        coroutineScope.launch { f.tiltY.snapTo(y); f.tiltY.animateTo(0f, Motion.tiltSpring) }
        coroutineScope.launch { f.sheen.snapTo(Motion.SHEEN_START); f.sheen.animateTo(Motion.SHEEN_END, Motion.sheenSpring) }
    }
}
