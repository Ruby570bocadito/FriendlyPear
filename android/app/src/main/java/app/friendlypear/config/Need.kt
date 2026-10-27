package app.friendlypear.config

import android.Manifest
import app.friendlypear.R
import app.friendlypear.core.agent.Capability

/**
 * Each permission Peri may ask for, with the sentence that says what for and what it won't do, and what keeps
 * working without it. [denied] is 0 when the agent itself explains the alternative (calls and messages).
 */
enum class Need(val permissions: List<String>, val why: Int, val denied: Int) {
    CONTACTS(listOf(Manifest.permission.READ_CONTACTS), R.string.why_contacts, R.string.denied_contacts),
    READ_SMS(listOf(Manifest.permission.RECEIVE_SMS), R.string.why_sms_read, R.string.denied_sms_read),
    PHONE_STATE(listOf(Manifest.permission.READ_PHONE_STATE), R.string.why_phone_state, R.string.denied_phone_state),
    MIC(listOf(Manifest.permission.RECORD_AUDIO), R.string.why_mic, R.string.denied_mic),
    CALL(listOf(Manifest.permission.CALL_PHONE), R.string.why_call, 0),
    SEND_SMS(listOf(Manifest.permission.SEND_SMS), R.string.why_sms_send, 0),
    LOCATION(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
        R.string.why_location,
        R.string.denied_location,
    ),
    ;

    companion object {
        /** Asked at first start, one at a time: without them the shield can't see who is family or what arrives. */
        val FIRST_START = listOf(CONTACTS, READ_SMS, PHONE_STATE)

        fun of(capability: Capability): Need = when (capability) {
            Capability.CALL -> CALL
            Capability.SEND_SMS -> SEND_SMS
            Capability.LOCATION -> LOCATION
        }
    }
}
