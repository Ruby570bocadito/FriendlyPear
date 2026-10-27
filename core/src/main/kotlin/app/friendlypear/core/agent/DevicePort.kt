package app.friendlypear.core.agent

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
}
