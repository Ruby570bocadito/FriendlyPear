package app.friendlypear.services

import android.telecom.Call
import android.telecom.CallScreeningService
import app.friendlypear.PeriApp

/**
 * Sees the number of an incoming call before it is picked up, so Peri can warn when it isn't in the contacts. It lets
 * every call through except the numbers the person chose to block (from a scam's card): those are turned away
 * before they ring. Peri never answers or listens to a call.
 */
class CallScreener : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.schemeSpecificPart?.takeIf { it.isNotBlank() }
        val incoming = details.callDirection == Call.Details.DIRECTION_INCOMING
        val hub = PeriApp.hub(this)
        if (incoming && number != null && hub.blocked.has(number)) {
            respondToCall(details, CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipNotification(true).build())
            hub.onBlockedCall()
            return
        }
        respondToCall(details, CallResponse.Builder().build())
        if (!incoming || number == null) return
        hub.onIncomingCall(number)
    }
}
