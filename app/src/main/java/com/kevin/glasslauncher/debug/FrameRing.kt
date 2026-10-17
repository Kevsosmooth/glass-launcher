package com.kevin.glasslauncher.debug

private const val JANK_MS = 16.7f
private const val NANOS_PER_SECOND = 1_000_000_000L

/** What the HUD shows; [frameMs] is oldest to newest. */
class HudSnapshot(val fps: Int, val frameMs: FloatArray, val jankyPercent: Float, val dropped: Boolean)

/** Last [capacity] frames (duration + vsync time). Pure; fed from the frame-metrics thread. */
class FrameRing(private val capacity: Int) {
    private val durations = FloatArray(capacity)
    private val vsyncs = LongArray(capacity)
    private var next = 0
    var count = 0
        private set

    fun add(durationMs: Float, vsyncNanos: Long) {
        durations[next] = durationMs
        vsyncs[next] = vsyncNanos
        next = (next + 1) % capacity
        if (count < capacity) count++
    }

    fun snapshot(dropped: Boolean): HudSnapshot {
        val ordered = FloatArray(count) { durations[(next - count + it + capacity) % capacity] }
        val latest = vsyncs[(next - 1 + capacity) % capacity]
        var fps = 0
        for (i in 0 until count) if (latest - vsyncs[i] < NANOS_PER_SECOND) fps++
        val janky = ordered.count { it > JANK_MS }
        val percent = if (count == 0) 0f else janky * 100f / count
        return HudSnapshot(fps, ordered, percent, dropped)
    }
}

/**
 * Throttles HUD updates to [intervalMs] and publishes only when more than one new frame arrived:
 * the HUD's own redraw is a single frame, so an idle screen stays idle.
 */
class HudPublisher(
    private val ring: FrameRing,
    private val intervalMs: Long,
    private val publish: (HudSnapshot) -> Unit,
) {
    private var lastPublishMs = Long.MIN_VALUE / 2
    private var sincePublish = 0
    private var dropped = false

    fun onFrame(durationMs: Float, vsyncNanos: Long, nowMs: Long) {
        ring.add(durationMs, vsyncNanos)
        sincePublish++
        if (durationMs > JANK_MS) dropped = true
        if (sincePublish > 1 && nowMs - lastPublishMs >= intervalMs) {
            publish(ring.snapshot(dropped))
            lastPublishMs = nowMs
            sincePublish = 0
            dropped = false
        }
    }
}
