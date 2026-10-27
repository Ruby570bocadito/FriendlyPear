package app.friendlypear.core.agent

import app.friendlypear.core.shield.Text

/** What Peri may say aloud: never links, codes or unknown numbers, which a scammer could be waiting for. */
internal object SafeSpeech {
    private val url = Regex("""(?i)\b(?:https?://|www\.)\S+""")
    private val email = Regex("""\S+@\S+\.\S+""")
    private val domain = Regex(
        """(?i)\b(?:[\p{L}\p{N}-]+\.)+(?:com|es|net|org|info|io|me|ly|co|app|link|xyz|top|online|site|eu|cat|gal|shop|store|club|live)\b(?:/\S*)?""",
    )
    private val digits = Regex("""\d[\d .-]*\d""")

    fun clean(text: String): String {
        var out = url.replace(text, "un enlace")
        out = email.replace(out, "una dirección de correo")
        out = domain.replace(out, "un enlace")
        out = digits.replace(out) { m ->
            val after = out.substring(m.range.last + 1).trimStart().lowercase()
            val isMoney = after.startsWith("€") || after.startsWith("euro")
            if (isMoney || m.value.count { it.isDigit() } < 4) m.value else "unos números"
        }
        return out.trim()
    }

    /** Spoken phone number for "you can dial it yourself": "600 111 222". */
    fun phone(phone: String): String = Text.phoneDigits(phone).chunked(3).joinToString(" ")

    fun names(contacts: List<String>): String =
        if (contacts.size == 1) contacts.single() else contacts.dropLast(1).joinToString(", ") + " o " + contacts.last()
}
