package app.friendlypear.core.shield

import app.friendlypear.core.model.Contact

/** What the shield looks for in a message. Each one adds its log-odds weight to the score. */
enum class Signal {
    UNKNOWN_SENDER,
    LINK,
    URL_SHORTENER,
    /** A link without TLS (http://) or on a port other than 443: nobody checks who is on the other side. */
    INSECURE_LINK,
    BRAND_LOOKALIKE,
    URGENCY,
    MONEY_REQUEST,
    CREDENTIAL_REQUEST,
    /** "Tell us the code we just sent you": the classic way to steal a one-time code. */
    CODE_FORWARD_REQUEST,
    NEW_NUMBER_CLAIM,
    PRIZE_OR_PROFIT,
    /** "Don't tell dad", "keep it a secret": cutting the person off from the family who would spot it. */
    SECRECY,
    /** "Your device is infected, call the technician": fear used to get a call or remote access. */
    DEVICE_SCARE,
    /** Affection or a far-away love story (a widower, a soldier, an oil rig) from someone not in the contacts. */
    ROMANCE_BAIT,
    REMOTE_ACCESS_APP,
    /** "Call me on 910...", "ring us on 0800...": a number in the message itself, how a fake bank or courier gets them on the phone. */
    CALLBACK_REQUEST,
    /** "Let's talk on Telegram", "add me on WhatsApp" from a text or an email: away from where it can be checked. */
    CHANNEL_SWITCH,
    /** "I sent you 50 by mistake, can you send it back?": the refund trick, often by Bizum. */
    MISTAKEN_PAYMENT,
    /** "Ya te pagué con la compra protegida, revisa tu correo y acéptalo": a second-hand sale's fake payment page. */
    SAFE_PAYMENT_BAIT,
    /** "Soy Marta de Correos", "Sarah from your bank" from a personal mobile or a Gmail address: companies write under their own name. */
    COMPANY_FROM_MOBILE,
    /** Letters from another alphabet or invisible ones inside a word ("pаypal", "ur​gente"): written to slip past filters. */
    DISGUISED_LETTERS,
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
    /** [byName]: whether the sender's name can stand for a contact (see [IncomingMessage.nameFromContacts]). */
    fun knows(sender: String, byName: Boolean = true): Boolean {
        val name = Text.fold(sender).trim()
        val phone = Text.phoneDigits(sender)
        return contacts.any { c ->
            (byName && (Text.fold(c.name) == name || c.aliases.any { Text.fold(it) == name })) ||
                (phone.length >= 6 && c.phones.any { Text.phoneDigits(it) == phone })
        }
    }
}
