package app.friendlypear.core.trust

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel

/**
 * What the family sets up on the protected person's phone, on top of the contact book: who the guardians are,
 * each contact's relation and aliases ("el chico"), and whether they agreed a family word.
 *
 * The family word itself is never stored: Peri only reminds people to ask for it, so it can't leak.
 * Guardians receive alerts and approve risky actions; they can't read messages or grant permissions.
 */
data class FamilyConfig(
    val guardians: Set<String> = emptySet(),
    val relations: Map<String, Relation> = emptyMap(),
    val aliases: Map<String, List<String>> = emptyMap(),
    val hasFamilyWord: Boolean = false,
) {
    /** The contact book as Peri sees it: family settings win over what was guessed from the saved names. */
    fun applyTo(contacts: List<Contact>): List<Contact> = contacts.map { c ->
        c.copy(
            relation = relations[c.id] ?: c.relation,
            aliases = (c.aliases + aliases[c.id].orEmpty().map { it.trim() }.filter { it.isNotEmpty() }).distinct(),
            trust = if (c.id in guardians) TrustLevel.GUARDIAN else c.trust,
        )
    }

    fun withGuardian(id: String, guardian: Boolean) = copy(guardians = if (guardian) guardians + id else guardians - id)

    fun withRelation(id: String, relation: Relation?) =
        copy(relations = if (relation == null) relations - id else relations + (id to relation))

    /** Aliases as the family types them: "el chico, Pablito". */
    fun withAliases(id: String, typed: String): FamilyConfig {
        val list = typed.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_ALIASES)
        return copy(aliases = if (list.isEmpty()) aliases - id else aliases + (id to list))
    }

    companion object {
        const val MAX_ALIASES = 5
    }
}
