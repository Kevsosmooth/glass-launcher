package com.kevin.glasslauncher.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kevin.glasslauncher.motion.Motion

/**
 * Two layers for a draw-phase crossfade: [previous] underneath at [previousAlpha], [current] on
 * top at [currentAlpha]. Alphas are read while drawing, so a fade redraws without recomposing,
 * and the single animation stops when it settles (idle = no frames).
 */
@Stable
class CrossfadeLayers<T : Any>(initial: T?) {
    var previous by mutableStateOf<T?>(null)
        private set
    var current by mutableStateOf(initial)
        private set
    private val fade = Animatable(1f)

    val currentAlpha: Float get() = fade.value

    /** An opaque new layer covers the old one, so the old one stays solid until it is dropped. */
    val previousAlpha: Float get() = if (current != null) 1f else 1f - fade.value

    suspend fun show(target: T?) {
        if (target === current) return
        previous = current
        current = target
        fade.snapTo(0f)
        fade.animateTo(1f, Motion.crossfadeSpring)
        previous = null
    }
}

/** Crossfades to [target] whenever it changes (by identity); null fades out to nothing. */
@Composable
fun <T : Any> rememberCrossfade(target: T?): CrossfadeLayers<T> {
    val layers = remember { CrossfadeLayers(target) }
    LaunchedEffect(target) { layers.show(target) }
    return layers
}
