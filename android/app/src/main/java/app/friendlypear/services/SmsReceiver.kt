package app.friendlypear.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import app.friendlypear.PeriApp
import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.IncomingMessage

/**
 * Incoming SMS straight from the system. Since Android 15, notification listeners get one-time-code
 * notifications redacted, and a code received during a call is exactly what Peri must catch.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        parts.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, pieces) ->
            val text = pieces.joinToString("") { it.messageBody.orEmpty() }.trim()
            if (sender.isNotEmpty() && text.isNotEmpty()) {
                // The SMS's own address: a number, or letters whoever sends it picks ("BBVA", or "Hija" from a scammer).
                val message = IncomingMessage(Channel.SMS, sender, text, pieces.first().timestampMillis, nameFromContacts = false)
                PeriApp.hub(context).onMessage(message)
            }
        }
    }
}
