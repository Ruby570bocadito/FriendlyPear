package app.friendlypear.core.shield

import java.text.Normalizer

internal object Text {
    private val marks = Regex("\\p{Mn}+")
    /** Invisible characters (zero-width spaces and joiners, direction marks): never part of what a word says. */
    private val invisible = Regex("\\p{Cf}+")
    private val sentenceBreak = Regex("[.!?;¡¿\\n]+")
    private val word = Regex("[a-z0-9]+")

    /** Cyrillic and Greek letters that look like Latin ones, as in "pаypal" or "АМАZОN": read as the Latin letter. */
    private val lookalikes = mapOf(
        'а' to 'a', 'в' to 'b', 'е' to 'e', 'к' to 'k', 'м' to 'm', 'н' to 'h', 'о' to 'o',
        'р' to 'p', 'с' to 'c', 'т' to 't', 'у' to 'y', 'х' to 'x', 'і' to 'i', 'ј' to 'j',
        'ѕ' to 's', 'һ' to 'h', 'ԁ' to 'd', 'ӏ' to 'l', 'ԛ' to 'q', 'ԝ' to 'w',
        'α' to 'a', 'β' to 'b', 'ε' to 'e', 'ι' to 'i', 'κ' to 'k', 'ν' to 'v', 'ο' to 'o',
        'ρ' to 'p', 'τ' to 't', 'υ' to 'u', 'χ' to 'x',
    )

    /**
     * Lowercase without accents, invisible characters or disguised letters, so "Mamá" and "mama" read the same, and so
     * do "ur​gente" (a zero-width space inside) and "urgente", or "pаypal" (a Cyrillic а) and "paypal".
     */
    fun fold(s: String): String {
        val plain = invisible.replace(marks.replace(Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD), ""), "")
        return if (plain.none { it in lookalikes }) plain else String(CharArray(plain.length) { lookalikes[plain[it]] ?: plain[it] })
    }

    private val hiddenInWord = Regex("\\p{IsLatin}\\p{Cf}+\\p{IsLatin}")
    private val anyWord = Regex("[\\p{L}\\p{M}]+")
    private val latin = Regex("\\p{IsLatin}")
    private val otherAlphabet = Regex("[\\p{IsCyrillic}\\p{IsGreek}]")

    /**
     * A word with an invisible character inside it, or with letters from two alphabets ("ur​gente", "pаypal"): how a
     * message is written to slip past filters. Joined emoji and a word in one alphabet are fine.
     */
    fun disguised(raw: String): Boolean =
        hiddenInWord.containsMatchIn(raw) || anyWord.findAll(raw).any { latin.containsMatchIn(it.value) && otherAlphabet.containsMatchIn(it.value) }

    fun sentences(folded: String): List<String> = folded.split(sentenceBreak).map { it.trim() }.filter { it.isNotEmpty() }

    fun words(folded: String): List<String> = word.findAll(folded).map { it.value }.toList()

    /** One pattern that matches any of the phrases as whole words. */
    fun anyOf(phrases: List<String>): Regex = Regex(phrases.joinToString("|", "\\b(", ")\\b") { Regex.escape(it) })

    /** Digits only, without the Spanish prefix, so "+34 612 34 56 78" and "612345678" match. */
    fun phoneDigits(s: String): String {
        val digits = s.filter { it.isDigit() }.removePrefix("00")
        return if (digits.length == 11 && digits.startsWith("34")) digits.substring(2) else digits
    }

    fun editDistance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[b.length]
    }
}
