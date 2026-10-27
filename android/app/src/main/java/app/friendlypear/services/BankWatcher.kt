package app.friendlypear.services

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * Notices which app comes to the front, only while Peri has a reason to ([keepGoing]): during a picked-up call
 * with a stranger, and for half an hour after a doubtful message, a scam or a stranger's call. Then a money or
 * remote-control app gets the safety pause. It needs usage access, granted in Settings, and only reads the
 * package name of the app in front, never what happens inside it.
 */
class BankWatcher(
    context: Context,
    private val keepGoing: () -> Boolean,
    private val onOpened: (packageName: String) -> Unit,
) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var since = 0L

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (!keepGoing()) return stop()
            val now = System.currentTimeMillis()
            val usage = context.getSystemService(UsageStatsManager::class.java)
            val events = usage?.queryEvents(since, now)
            val event = UsageEvents.Event()
            while (events != null && events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) onOpened(event.packageName)
            }
            since = now
            main.postDelayed(this, EVERY_MILLIS)
        }
    }

    /** Whether the person granted usage access to Peri in Settings. */
    fun allowed(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun start() {
        if (running || !allowed()) return
        running = true
        since = System.currentTimeMillis()
        main.post(tick)
    }

    fun stop() {
        running = false
        main.removeCallbacks(tick)
    }

    private companion object {
        const val EVERY_MILLIS = 2_000L
    }
}
