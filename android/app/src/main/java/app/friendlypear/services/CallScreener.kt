package app.friendlypear.services

import android.telecom.Call
import android.telecom.CallScreeningService
import app.friendlypear.PeriApp

/**
 * Sees the number of an incoming call before it is picked up, so Peri can warn when it isn't in the contacts.
 * It always lets the call through: Peri never blocks, answers or listens to a call.
 */
class CallScreener : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        respondToCall(details, CallResponse.Builder().build())
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) return
        val number = details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() } ?: return
        PeriApp.hub(this).onIncomingCall(number)
    }
}
