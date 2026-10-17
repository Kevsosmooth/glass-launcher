package com.kevin.glasslauncher.debug

import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CAPACITY = 120
private const val PUBLISH_MS = 250L
private const val BUDGET_MS = 16.7f
private const val GRAPH_MAX_MS = 33.4f
private val HudShape = RoundedCornerShape(8.dp)
private val HudBackground = Color(0xCC000000)
private val Dropped = Color(0xFFE5484D)
private val Ok = Color(0xFF30A46C)
private val HudText = TextStyle(color = Color.White, fontSize = 12.sp)

/** Debug HUD: FPS, jank %, frame-time graph, red frame after a dropped frame. Reads FrameMetrics; forces no frames. */
@Composable
fun FrameHud(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current ?: return
    val snapshot = remember { mutableStateOf<HudSnapshot?>(null) }
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
    snapshot.value?.let { HudPanel(it, modifier) }
}

@Composable
private fun HudPanel(s: HudSnapshot, modifier: Modifier) {
    Column(
        modifier
            .padding(16.dp)
            .background(HudBackground, HudShape)
            .border(2.dp, if (s.dropped) Dropped else Color.Transparent, HudShape)
            .padding(8.dp),
    ) {
        BasicText("${s.fps} fps   jank ${"%.1f".format(s.jankyPercent)}%", style = HudText)
        Canvas(Modifier.padding(top = 4.dp).size(180.dp, 40.dp)) {
            val bar = size.width / CAPACITY
            s.frameMs.forEachIndexed { i, ms ->
                val h = (ms / GRAPH_MAX_MS).coerceAtMost(1f) * size.height
                drawRect(if (ms > BUDGET_MS) Dropped else Ok, Offset(i * bar, size.height - h), Size(bar * 0.8f, h))
            }
            val y = size.height * (1f - BUDGET_MS / GRAPH_MAX_MS)
            drawLine(Color.White, Offset(0f, y), Offset(size.width, y), 1f)
        }
    }
}
