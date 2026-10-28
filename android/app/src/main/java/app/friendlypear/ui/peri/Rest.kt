package app.friendlypear.ui.peri

import android.os.SystemClock

/**
 * When the person last touched the screen, and the calm frame rate that follows from it. Peri's breathing and her
 * world's drifting clouds need about 30 frames a second, 20 while nobody touches the screen: still smooth to look at,
 * and kinder to an old phone's battery. Any touch, and anything lively, brings every frame back at once.
 */
internal object Rest {
    private const val AFTER_MS = 20_000L
    private const val CALM_FRAME_MS = 33L
    private const val RESTING_FRAME_MS = 50L

    @Volatile
    private var touchedAt = SystemClock.uptimeMillis()

    fun touched() {
        touchedAt = SystemClock.uptimeMillis()
    }

    fun resting(now: Long = SystemClock.uptimeMillis()): Boolean = now - touchedAt > AFTER_MS

    /** How long a calm frame lasts right now. */
    fun calmFrameMs(now: Long = SystemClock.uptimeMillis()): Long = if (resting(now)) RESTING_FRAME_MS else CALM_FRAME_MS

    /**
     * The calm frame the vsync at [frameTimeNanos] falls in. Each slow view steps when it changes, so they all step on
     * the same vsync and the screen is drawn once for all of them, not once for each.
     */
    fun slot(frameTimeNanos: Long, frameMs: Long): Long = frameTimeNanos / (frameMs * 1_000_000L)
}
