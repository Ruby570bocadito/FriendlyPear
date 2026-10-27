package app.friendlypear.core.voice

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Relation
import app.friendlypear.core.shield.Text

sealed interface Resolution {
    data class Found(val contact: Contact) : Resolution
    /** Peri asks which one: "¿Laura o Pablo?". */
    data class Ambiguous(val candidates: List<Contact>) : Resolution
    data object NotFound : Resolution
}

/**
 * Finds who the person means: an alias set by the family ("el chico"), a name or its diminutive
 * ("Pablito"), or a relation ("mi nieta"). Aliases win over names, names over relations.
 */
class ContactDirectory(private val contacts: List<Contact>) {

    fun resolve(spoken: String): Resolution {
        val words = meaningful(Text.fold(spoken))
        if (words.isEmpty()) return Resolution.NotFound
        val phrase = words.joinToString(" ")

        val byAlias = contacts.filter { c -> c.aliases.any { meaningful(Text.fold(it)).joinToString(" ") == phrase } }
        if (byAlias.isNotEmpty()) return pick(byAlias)

        val scored = contacts.map { it to nameMatch(it.name, words) }
        val best = scored.maxOfOrNull { it.second } ?: Diminutives.Match.NONE
        if (best != Diminutives.Match.NONE) {
            val byName = scored.filter { it.second == best }.map { it.first }
            val relation = relationOf(words)
            val narrowed = if (relation != null) byName.filter { it.relation == relation }.ifEmpty { byName } else byName
            return pick(narrowed)
        }

        val relation = relationOf(words) ?: return Resolution.NotFound
        return pick(contacts.filter { it.relation == relation })
    }

    /** The saved contact behind a number, so "this number is not Pablo's" can be said. */
    fun byPhone(phone: String): Contact? {
        val digits = Text.phoneDigits(phone)
        if (digits.isEmpty()) return null
        return contacts.firstOrNull { c -> c.phones.any { Text.phoneDigits(it) == digits } }
    }

    private fun pick(found: List<Contact>): Resolution = when (found.size) {
        0 -> Resolution.NotFound
        1 -> Resolution.Found(found.single())
        else -> Resolution.Ambiguous(found)
    }

    private fun nameMatch(name: String, words: List<String>): Diminutives.Match {
        val nameWords = Text.words(Text.fold(name))
        return words.flatMap { w -> nameWords.map { n -> Diminutives.match(w, n) } }.maxOrNull() ?: Diminutives.Match.NONE
    }

    private fun relationOf(words: List<String>): Relation? =
        words.firstNotNullOfOrNull { RelationWords.of(it) }

    private fun meaningful(folded: String): List<String> = Text.words(folded).filter { it !in stopWords }

    private companion object {
        val stopWords = setOf(
            "a", "al", "el", "la", "los", "las", "mi", "mis", "de", "del", "con", "y", "que", "tu", "su", "nuestro", "nuestra",
            "my", "the", "to", "an", "with", "his", "her", "our", "me", "and",
        )
    }
}
