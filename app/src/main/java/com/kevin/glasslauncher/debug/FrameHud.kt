package com.kevin.glasslauncher.debug

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kevin.glasslauncher.R

private const val CAPACITY = 120
private const val PUBLISH_MS = 250L
private const val BUDGET_MS = 16.7f
private const val GRAPH_MAX_MS = 33.4f
internal val Dropped = Color(0xFFE5484D)
private val Ok = Color(0xFF30A46C)

/**
 * Frame stats of the activity window from FrameMetrics, published at most every 250 ms from a
 * background thread. Listens only while in composition; forces no frames.
 */
@Composable
fun rememberFrameSnapshot(): State<HudSnapshot?> {
    val snapshot = remember { mutableStateOf<HudSnapshot?>(null) }
    val activity = LocalActivity.current ?: return snapshot
    DisposableEffect(activity) {
        val window = activity.window
        val thread = HandlerThread("frame-hud").apply { start() }
        val publisher = HudPublisher(FrameRing(CAPACITY), PUBLISH_MS) { snapshot.value = it }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            publisher.onFrame(
                metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000f,
                metrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP),
                SystemClock.uptimeMillis(),
            )
        }
        window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
        onDispose {
            window.removeOnFrameMetricsAvailableListener(listener)
            thread.quitSafely()
        }
    }
    return snapshot
}

/** FPS, jank % and the frame-time graph (red bars over the 16.7 ms budget line). */
@Composable
fun FrameHud(frames: State<HudSnapshot?>) {
    val s = frames.value
    BasicText(
        if (s == null) stringResource(R.string.stats_frames_waiting) else stringResource(R.string.stats_frames, s.fps, s.jankyPercent),
        style = StatsText,
    )
    if (s == null) return
    Canvas(Modifier.padding(top = 4.dp).size(STATS_WIDTH, 40.dp)) {
        val bar = size.width / CAPACITY
        s.frameMs.forEachIndexed { i, ms ->
            val h = (ms / GRAPH_MAX_MS).coerceAtMost(1f) * size.height
            drawRect(if (ms > BUDGET_MS) Dropped else Ok, Offset(i * bar, size.height - h), Size(bar * 0.8f, h))
        }
        val y = size.height * (1f - BUDGET_MS / GRAPH_MAX_MS)
        drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 1f)
    }
}
