package app.friendlypear.services

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * During a call that was picked up, notices which app comes to the front, so Peri can warn if a banking app
 * opens while a stranger is on the line. It only runs during calls and needs usage access, granted in Settings.
 */
class BankWatcher(context: Context, private val onOpened: (packageName: String) -> Unit) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var running = false
    private var since = 0L

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
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
