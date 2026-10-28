package app.friendlypear.services

import android.telecom.Call
import android.telecom.CallScreeningService
import app.friendlypear.PeriApp

/**
 * Sees the number of an incoming call before it is picked up, so Peri can warn when it isn't in the contacts. It lets
 * every call through except the numbers the person, or someone else in the family, chose to block (from a scam's
 * card): those are turned away before they ring, unless it's a saved contact on this phone. Peri never answers or listens to a call.
 */
class CallScreener : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
        val incoming = details.callDirection == Call.Details.DIRECTION_INCOMING
        val hub = PeriApp.hub(this)
        val blocker = if (incoming && number != null) hub.blockedHere(number) else null
        if (blocker != null) {
            respondToCall(details, CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipNotification(true).build())
            hub.onBlockedCall(blocker)
            return
        }
        respondToCall(details, CallResponse.Builder().build())
        if (!incoming || number == null) return
        hub.onIncomingCall(number)
    }
}
