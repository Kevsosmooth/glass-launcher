package com.kevin.glasslauncher.debug

import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.glasslauncher.BuildConfig
import com.kevin.glasslauncher.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val STATS_WIDTH = 260.dp
internal val StatsText = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
private val StatsShape = RoundedCornerShape(8.dp)
private val StatsBackground = Color(0xCC000000)
private val SectionGap = 6.dp
private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * Stats overlay (Settings > Stats overlay): frames, memory, CPU, network, recent requests and the
 * device. Drawn over everything; holds nothing focusable, so D-pad focus never lands on it.
 * Sampling runs off the main thread only while it is shown; each section reads its own state so
 * only that section recomposes.
 */
@Composable
fun StatsOverlay(modifier: Modifier = Modifier) {
    val frames = rememberFrameSnapshot()
    val samples = rememberStatsSamples()
    Column(
        modifier
            .padding(24.dp)
            .droppedOutline(frames)
            .background(StatsBackground, StatsShape)
            .padding(8.dp)
            .width(STATS_WIDTH),
    ) {
        FrameHud(frames)
        SampleLines(samples)
        RequestLines()
    }
}

@Composable
private fun rememberStatsSamples(): State<StatsSample?> {
    val context = LocalContext.current
    val view = LocalView.current
    val flow = remember(view) {
        val display = view.display ?: context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        statsSamples(context, display)
    }
    return flow.collectAsStateWithLifecycle(null)
}

/** Red outline after a dropped frame; read at draw time so it never recomposes the card. */
private fun Modifier.droppedOutline(frames: State<HudSnapshot?>) = drawWithContent {
    drawContent()
    if (frames.value?.dropped == true) {
        drawRoundRect(Dropped, cornerRadius = CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun SampleLines(samples: State<StatsSample?>) {
    val s = samples.value
    Column(Modifier.padding(top = SectionGap)) {
        if (s != null) {
            val na = stringResource(R.string.stats_na)
            Line(stringResource(R.string.stats_memory, s.pssMb, s.heapUsedMb, s.heapMaxMb))
            Line(
                stringResource(R.string.stats_ram, s.ramAvailMb, s.ramTotalMb) +
                    if (s.lowMemory) " " + stringResource(R.string.stats_ram_low) else "",
                if (s.lowMemory) Dropped else Color.White,
            )
            Line(stringResource(R.string.stats_cpu, s.cpuPercent?.let { "%.1f%%".format(it) } ?: na))
            Line(stringResource(R.string.stats_net_app, kbs(s.appRxKbs, na), kbs(s.appTxKbs, na)))
            Line(stringResource(R.string.stats_net_device, kbs(s.deviceRxKbs, na), kbs(s.deviceTxKbs, na)))
        }
        Line(stringResource(R.string.stats_device, Build.VERSION.RELEASE, Build.MODEL))
        val version = stringResource(R.string.stats_version, BuildConfig.VERSION_NAME)
        val mode = s?.display?.let { stringResource(R.string.stats_display, it.width, it.height, "%.2f".format(it.refreshHz)) }
        Line(listOfNotNull(mode, version).joinToString(", "))
    }
}

@Composable
private fun RequestLines() {
    val requests by NetLog.entries.collectAsStateWithLifecycle()
    Column(Modifier.padding(top = SectionGap)) {
        Line(stringResource(R.string.stats_requests))
        if (requests.isEmpty()) Line(stringResource(R.string.stats_no_requests))
        requests.asReversed().forEach { Line(requestLine(it)) }
    }
}

@Composable
private fun Line(text: String, color: Color = Color.White) {
    BasicText(
        text,
        style = if (color == Color.White) StatsText else StatsText.copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun kbs(value: Double?, na: String) = value?.let { "%.1f".format(it) } ?: na

/** "14:03:21 GET 200 12KB 340ms host/path" (target last, the part that gets cut); an error name stands in for a missing status. */
private fun requestLine(e: NetEntry): String {
    val time = ClockFormat.format(Instant.ofEpochMilli(e.atMillis).atZone(ZoneId.systemDefault()))
    val result = e.status?.toString() ?: e.error ?: "?"
    return "$time ${e.method} $result ${e.bytes / 1024}KB ${e.millis}ms ${e.target}"
}
