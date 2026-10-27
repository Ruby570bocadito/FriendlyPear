package app.friendlypear.core.shield

import app.friendlypear.core.model.Contact

/** What the shield looks for in a message. Each one adds its log-odds weight to the score. */
enum class Signal {
    UNKNOWN_SENDER,
    LINK,
    URL_SHORTENER,
    BRAND_LOOKALIKE,
    URGENCY,
    MONEY_REQUEST,
    CREDENTIAL_REQUEST,
    /** "Tell us the code we just sent you": the classic way to steal a one-time code. */
    CODE_FORWARD_REQUEST,
    NEW_NUMBER_CLAIM,
    PRIZE_OR_PROFIT,
    REMOTE_ACCESS_APP,
    AI_INSTRUCTIONS,
    ONE_TIME_CODE,
    CODE_DURING_CALL,
}

/** Rules the model can never overrule: they make the message a scam on their own. */
enum class HardRule { CODE_DURING_CALL, BRAND_LOOKALIKE }

/** What the phone knows around the message: who is in the circle and whether a call is going on. */
data class ShieldContext(
    val contacts: List<Contact> = emptyList(),
    val callActive: Boolean = false,
) {
    fun knows(sender: String): Boolean {
        val name = Text.fold(sender).trim()
        val phone = Text.phoneDigits(sender)
        return contacts.any { c ->
            Text.fold(c.name) == name ||
                c.aliases.any { Text.fold(it) == name } ||
                (phone.length >= 6 && c.phones.any { Text.phoneDigits(it) == phone })
        }
    }
}
