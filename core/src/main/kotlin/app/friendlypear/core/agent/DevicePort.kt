package app.friendlypear.core.agent

/** Things Peri does that need the person's permission on the phone, asked the first time they are needed. */
enum class Capability { CALL, SEND_SMS, LOCATION }

/**
 * Everything the agent needs from the phone. Android implements it in android/.../device;
 * tests use a fake. Changes here are additive and need a claim (shared zone).
 */
interface DevicePort {
    fun speak(text: String)
    fun call(phone: String)

    /**
     * Ends the call on the line, if the phone lets Peri (Android's permission to end calls). True if it did. Only on
     * the person's own tap, or a guardian's request about a call from a number that isn't in the contacts.
     */
    fun hangUp(): Boolean = false
    fun sendSms(phone: String, text: String)
    /** "lat,lon", or null when there is no recent fix. */
    fun lastKnownLocation(): String?

    /**
     * Makes sure Peri may do [capability], asking the person first (with Peri's explanation) if it was never
     * granted. [then] learns whether it may; a "no" is respected and Peri carries on with less.
     */
    fun prepare(capability: Capability, then: (Boolean) -> Unit) = then(true)

    /**
     * Stops the person for a moment over the app they just opened: Peri's [text], a button to call [callName]
     * if there is someone to check with, a big "hang up" while a stranger is on the line ([hangUp]), "keep talking"
     * when the family asked Peri to hang up ([keep]: three seconds to say no), "leave this app" and, after a few
     * seconds, "continue anyway". The answer comes back through [PeriAgent.onPauseAnswer]. Without a screen to show
     * it on, Peri only speaks.
     */
    fun showPause(text: String, callName: String?, hangUp: Boolean = false, keep: Boolean = false) = Unit
}

/** What the person chose on the safety pause. */
enum class PauseAnswer { CALL_FAMILY, HANG_UP, KEEP_CALL, LEAVE, CONTINUE }

/** What happened a moment ago that makes Peri pause the person: a scam, a doubtful message, a stranger's call, a picture. */
enum class RecentKind { SCAM, DOUBT, CALL, PICTURE }
