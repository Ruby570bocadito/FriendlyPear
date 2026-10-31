package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.shield.Text
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

/** Digits as words, in both languages; "o" is "or" in Spanish, so only "oh" stands for zero. */
private val digitWords = mapOf(
    "cero" to "0", "zero" to "0", "oh" to "0", "uno" to "1", "una" to "1", "one" to "1", "dos" to "2", "two" to "2",
    "tres" to "3", "three" to "3", "cuatro" to "4", "four" to "4", "cinco" to "5", "five" to "5", "seis" to "6",
    "six" to "6", "siete" to "7", "seven" to "7", "ocho" to "8", "eight" to "8", "nueve" to "9", "nine" to "9",
)
private val word = Regex("\\p{L}+")

/** Lone digits listed ("7, 3, 5 y 0") read as one run; "12,50" and "1,99" are prices, not lists. */
private val listedDigit = Regex("(?<!\\d)(\\d)\\s*(?:,|\\by\\b|\\band\\b)\\s*(?=\\d(?!\\d))")

/**
 * 4 to 8 digits together, like a one-time code ("4821", "48 21"), also spelled out in words ("siete tres cinco
 * cero", "seven three five oh") or listed ("7, 3, 5 y 0"), as a scammer on the line can dictate it. A 9-digit phone
 * number is not one.
 */
internal fun containsCode(text: String): Boolean {
    val spelled = word.replace(Text.fold(text)) { digitWords[it.value] ?: it.value }
    return digitRun.findAll(listedDigit.replace(spelled, "$1 ")).any { m -> m.value.count { it.isDigit() } in 4..8 }
}
