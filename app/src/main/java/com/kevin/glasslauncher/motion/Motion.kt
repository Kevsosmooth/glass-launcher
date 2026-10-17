package com.kevin.glasslauncher.motion

import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/** Every motion value in one place so it can be tuned by eye on the Shield. */
object Motion {
    const val FOCUS_SCALE = 1.15f
    const val TILT_DEGREES = 4f
    const val SHEEN_START = -0.3f
    const val SHEEN_END = 1.3f
    const val CAMERA_DISTANCE = 12f
    // 16 dp keeps the shadow (ambient blur = elevation / 2) inside the dock with 6 dp to spare.
    val LIFT_ELEVATION = 16.dp
    const val WIGGLE_DEGREES = 1.5f

    val focusSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)
    val tiltSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)

    /** Edit-mode wiggle, one swing between +/- WIGGLE_DEGREES; critically damped, so no overshoot. */
    val wiggleSpring = spring<Float>(dampingRatio = 1f, stiffness = 300f)

    // Spike: the very-low-stiffness sheen kept frames coming for ~1.5 s. Stiffer settles in < 0.6 s.
    val sheenSpring = spring<Float>(dampingRatio = 1f, stiffness = 200f)

    /** Settings and Quick Controls slide in from the right. */
    val panelSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 420f)

    /** Switch knob: a little overshoot so it lands like a drop of glass. */
    val switchSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 520f)

    /** Wallpaper (~600 ms), AppBackdrop and TopShelf card crossfades. */
    val crossfadeSpring = spring<Float>(dampingRatio = 1f, stiffness = 120f)
}
