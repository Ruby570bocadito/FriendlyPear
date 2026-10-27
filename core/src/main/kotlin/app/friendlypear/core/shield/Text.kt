package app.friendlypear.core.shield

import java.text.Normalizer

internal object Text {
    private val marks = Regex("\\p{Mn}+")
    private val sentenceBreak = Regex("[.!?;¡¿\\n]+")
    private val word = Regex("[a-z0-9]+")

    /** Lowercase without accents, so "Mamá" and "mama" read the same. */
    fun fold(s: String): String = marks.replace(Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD), "")

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
