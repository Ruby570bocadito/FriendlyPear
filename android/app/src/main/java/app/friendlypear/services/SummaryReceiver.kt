package app.friendlypear.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import app.friendlypear.PeriApp

/**
 * Once a day Android wakes Peri to see whether the guardians' weekly summary is due. An inexact alarm: no special
 * permission, and Android may batch it with others to save battery.
 */
class SummaryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PeriApp.hub(context).weeklySummary()
    }

    companion object {
        fun schedule(context: Context) {
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            val pending = PendingIntent.getBroadcast(
                context, 0, Intent(context, SummaryReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarms.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_DAY, pending,
            )
        }
    }
}
