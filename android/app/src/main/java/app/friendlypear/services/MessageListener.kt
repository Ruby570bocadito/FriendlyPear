package app.friendlypear.services

import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import app.friendlypear.PeriApp
import app.friendlypear.config.WebNotice
import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.IncomingMessage

/**
 * Reads SMS, WhatsApp and email from their notifications: one permission, no SMS access.
 * Message text goes to the agent only; it is never logged.
 */
class MessageListener : NotificationListenerService() {
    private val seen = LinkedHashSet<Int>()

    override fun onListenerConnected() {
        Log.i(TAG, "listener connected")
    }

    override fun onListenerDisconnected() {
        Log.i(TAG, "listener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName in BROWSERS) return webNotice(sbn)
        val channel = CHANNELS[sbn.packageName] ?: return
        // With the SMS permission, SmsReceiver reads SMS unredacted; reading them here too would warn twice.
        if (channel == Channel.SMS && checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) return
        val notification = sbn.notification
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = notification.extras
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()?.trim().orEmpty()
        if (sender.isEmpty() || text.isEmpty()) {
            Log.i(TAG, "skipped $channel notification without sender or text")
            return
        }

        // Messaging apps re-post the same notification when it is updated; read each message once.
        if (!seen.add((sbn.key + text).hashCode())) return
        if (seen.size > MAX_SEEN) seen.remove(seen.first())

        PeriApp.hub(this).onMessage(IncomingMessage(channel, sender, text, sbn.postTime))
    }

    /**
     * A website's notice, through the browser: checked like a message, with the site as the sender (browsers show
     * it in the notice's small line) and the browser's channel for that site, so Peri can open its own switch.
     */
    private fun webNotice(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val site = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (title.isEmpty() && body.isEmpty()) return
        if (!seen.add((sbn.key + title + body).hashCode())) return
        if (seen.size > MAX_SEEN) seen.remove(seen.first())
        val message = IncomingMessage(Channel.BROWSER_NOTIFICATION, site ?: title, listOf(title, body).filter { it.isNotEmpty() }.joinToString(". "), sbn.postTime)
        PeriApp.hub(this).onWebNotice(message, WebNotice.Notice(sbn.packageName, sbn.notification.channelId, site, sbn.postTime))
    }

    private companion object {
        const val TAG = "FriendlyPear"
        /** Browsers that pass websites' notices on to the phone. */
        val BROWSERS = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.sec.android.app.sbrowser",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
        )
        const val MAX_SEEN = 200
        val CHANNELS = mapOf(
            "com.google.android.apps.messaging" to Channel.SMS,
            "com.android.messaging" to Channel.SMS,
            "com.samsung.android.messaging" to Channel.SMS,
            "com.whatsapp" to Channel.WHATSAPP,
            "com.whatsapp.w4b" to Channel.WHATSAPP,
            "com.google.android.gm" to Channel.EMAIL,
        )
    }
}
