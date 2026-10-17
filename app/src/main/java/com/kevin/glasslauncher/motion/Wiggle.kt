package com.kevin.glasslauncher.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * Edit-mode wiggle: swings +/- [Motion.WIGGLE_DEGREES] on springs for as long as it is in the
 * chain, in the layer only (no recomposition). Add it only to the tile being moved.
 */
fun Modifier.wiggle(): Modifier = this then WiggleElement

private data object WiggleElement : ModifierNodeElement<WiggleNode>() {
    override fun create() = WiggleNode()

    override fun update(node: WiggleNode) = Unit
}

private class WiggleNode : Modifier.Node(), LayoutModifierNode {
    private val angle = Animatable(0f)
    private val layerBlock: GraphicsLayerScope.() -> Unit = { rotationZ = angle.value }

    // The node's scope is cancelled on detach, which stops the loop.
    override fun onAttach() {
        coroutineScope.launch {
            var target = Motion.WIGGLE_DEGREES
            while (true) {
                angle.animateTo(target, Motion.wiggleSpring)
                target = -target
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layerBlock) }
    }
}
