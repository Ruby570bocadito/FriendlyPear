package app.friendlypear.core.voice

import app.friendlypear.core.model.Relation
import app.friendlypear.core.shield.Text

/** Spanish words for relations, seen from the protected person: "mi hija" is a [Relation.CHILD]. */
object RelationWords {
    private val words: Map<String, Relation> = buildMap {
        listOf("hijo", "hija", "hijos", "hijas").forEach { put(it, Relation.CHILD) }
        listOf("nieto", "nieta", "nietos", "nietas").forEach { put(it, Relation.GRANDCHILD) }
        listOf("sobrino", "sobrina", "sobrinos", "sobrinas").forEach { put(it, Relation.NEPHEW_NIECE) }
        listOf("hermano", "hermana", "hermanos", "hermanas").forEach { put(it, Relation.SIBLING) }
        listOf("marido", "mujer", "esposo", "esposa", "pareja", "novio", "novia").forEach { put(it, Relation.PARTNER) }
        listOf("amigo", "amiga").forEach { put(it, Relation.FRIEND) }
        listOf("cuidador", "cuidadora").forEach { put(it, Relation.CAREGIVER) }
    }

    /** [word] must be folded (lowercase, no accents). */
    fun of(word: String): Relation? = words[word]

    /**
     * Older people often save contacts as "Laura hija" or "Mi nieto Pablo". Returns the relation and the
     * name without it, or null when the saved name has no relation word.
     */
    fun inSavedName(savedName: String): Pair<Relation, String>? {
        val parts = savedName.trim().split(Regex("[\\s()\\-,]+")).filter { it.isNotEmpty() }
        val index = parts.indexOfFirst { of(Text.fold(it)) != null }
        if (index < 0) return null
        val relation = of(Text.fold(parts[index]))!!
        val name = parts.filterIndexed { i, p -> i != index && Text.fold(p) != "mi" }.joinToString(" ")
        return relation to name.ifEmpty { parts[index] }
    }
}
