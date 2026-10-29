package app.friendlypear.core.agent

import app.friendlypear.core.shield.Links

/**
 * A message the person pasted or retold in the chat ("me ha llegado esto: …", "is this a scam? «…»", or the message
 * itself), so the same shield as for SMS looks at it, on the phone. A question or an order is not a message: it goes
 * to Peri as usual. What counts as the message itself: long enough, and with what messages carry and questions don't
 * (a link, an amount, a code).
 */
object PastedMessage {
    private val intro = Regex(
        "^(?:me (?:ha|han) (?:llegado|mandado|enviado|escrito)(?: (?:esto|este (?:mensaje|sms|correo|email|whatsapp)))?|" +
            "mira (?:esto|este mensaje)|es (?:esto|este mensaje) un timo|es esto (?:seguro|de fiar)|" +
            "i (?:got|received|ve got|have got) this(?: message| text| email| sms)?|check this(?: message| text)?|" +
            "is this (?:a scam|safe|legit|real))\\s*[:?\\-]\\s*",
        RegexOption.IGNORE_CASE,
    )
    private val quoted = Regex("[«\"“]([^«»\"“”]{20,})[»\"”]")
    private val amount = Regex("(?:[€$£]\\s?\\d)|(?:\\b\\d+(?:[.,]\\d{1,2})?\\s?(?:€|eur|euros|usd|dollars|dolares|£|gbp|pounds|libras)\\b)", RegexOption.IGNORE_CASE)
    private val code = Regex("\\b\\d{4,8}\\b")
    private const val MIN_WORDS = 8
    private const val MIN_CHARS = 12

    /** The message inside [text] if it looks like one the person received; null for a question or an order. */
    fun find(text: String): String? {
        val t = text.trim()
        intro.find(t)?.let { m ->
            val rest = t.substring(m.range.last + 1).trim()
            return (quoted.find(rest)?.groupValues?.get(1)?.trim() ?: rest).takeIf { it.length >= MIN_CHARS }
        }
        quoted.find(t)?.let { return it.groupValues[1].trim() }
        val words = t.split(Regex("\\s+")).size
        val carries = Links.hosts(t).isNotEmpty() || amount.containsMatchIn(t) || code.containsMatchIn(t)
        return if (words >= MIN_WORDS && carries) t else null
    }
}
