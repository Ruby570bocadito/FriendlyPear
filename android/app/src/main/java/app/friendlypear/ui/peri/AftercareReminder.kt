package app.friendlypear.ui.peri

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.friendlypear.R
import app.friendlypear.core.agent.Breach

/**
 * The day after giving away the card, the bank's details, money or control of the phone: a reminder to look at the
 * bank's movements, since a charge sometimes shows up later. One reminder, opening the same steps.
 */
class AftercareReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(KIND)?.let { k -> Breach.entries.firstOrNull { it.name == k } } ?: return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.alerts_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 4_205, AftercareActivity.intent(context, kind), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = context.getString(R.string.after_reminder_text)
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_pear)
            .setColor(Palette.GREEN_DARK)
            .setContentTitle(context.getString(R.string.after_reminder_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(4_205, notification) }
    }

    companion object {
        private const val KIND = "kind"
        private const val CHANNEL = "peri_alerts"
        private const val DAY_MS = 20 * 60 * 60_000L

        /** Tomorrow, about this time: the one reminder (setting it again replaces it). */
        fun set(context: Context, kind: Breach) {
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            val pending = PendingIntent.getBroadcast(
                context, 4_205, Intent(context, AftercareReminder::class.java).putExtra(KIND, kind.name),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            alarms.set(AlarmManager.RTC, System.currentTimeMillis() + DAY_MS, pending)
        }
    }
}
