package app.friendlypear.core.agent

import app.friendlypear.core.shield.Text

/** What Peri may say aloud: never links, codes or unknown numbers, which a scammer could be waiting for. */
internal class SafeSpeech(private val p: Phrases) {
    fun clean(text: String): String {
        var out = url.replace(text, p.link)
        out = email.replace(out, p.email)
        out = domain.replace(out, p.link)
        out = digits.replace(out) { m ->
            val before = out.substring(0, m.range.first).trimEnd().lowercase()
            val after = out.substring(m.range.last + 1).trimStart().lowercase()
            val isMoney = p.moneyWords.any { after.startsWith(it) } || before.endsWith("$") || before.endsWith("£")
            if (isMoney || m.value.count { it.isDigit() } < 4) m.value else p.digits
        }
        return out.trim()
    }

    fun names(contacts: List<String>): String =
        if (contacts.size == 1) contacts.single() else contacts.dropLast(1).joinToString(", ") + " ${p.or} " + contacts.last()

    companion object {
        private val url = Regex("""(?i)\b(?:https?://|www\.)\S+""")
        private val email = Regex("""\S+@\S+\.\S+""")
        private val domain = Regex(
            """(?i)\b(?:[\p{L}\p{N}-]+\.)+(?:com|es|net|org|info|io|me|ly|co|uk|app|link|xyz|top|online|site|eu|cat|gal|shop|store|club|live)\b(?:/\S*)?""",
        )
        private val digits = Regex("""\d[\d .-]*\d""")

        /** Spoken phone number for "you can dial it yourself": "600 111 222". */
        fun phone(phone: String): String = Text.phoneDigits(phone).chunked(3).joinToString(" ")
    }
}
