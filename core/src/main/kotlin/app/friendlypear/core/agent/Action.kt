package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.trust.Risk

/** Something Peri does in the world on the person's behalf. Reading and analysing are not actions. */
sealed interface Action {
    val contact: Contact
    val risk: Risk

    /** A call needs a spoken "yes". */
    data class Call(override val contact: Contact) : Action {
        override val risk = Risk.MEDIUM
    }

    /** A message needs a spoken "yes"; dictating a code needs a guardian. */
    data class SendSms(override val contact: Contact, val text: String) : Action {
        override val risk = if (containsCode(text)) Risk.HIGH else Risk.MEDIUM
    }
}

private val digitRun = Regex("\\d[\\d .-]*\\d")

/** 4 to 8 digits together, like a one-time code ("4821", "48 21"); a 9-digit phone number is not one. */
internal fun containsCode(text: String): Boolean =
    digitRun.findAll(text).any { m -> m.value.count { it.isDigit() } in 4..8 }
