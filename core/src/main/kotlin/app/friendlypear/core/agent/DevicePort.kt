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
    fun sendSms(phone: String, text: String)
    /** "lat,lon", or null when there is no recent fix. */
    fun lastKnownLocation(): String?

    /**
     * Makes sure Peri may do [capability], asking the person first (with Peri's explanation) if it was never
     * granted. [then] learns whether it may; a "no" is respected and Peri carries on with less.
     */
    fun prepare(capability: Capability, then: (Boolean) -> Unit) = then(true)
}
