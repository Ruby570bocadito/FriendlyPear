package app.friendlypear.core.voice

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ContactDirectoryTest {
    private val laura = Contact("1", "Laura Gómez", listOf("+34 600 111 222"), Relation.CHILD, aliases = listOf("la niña"))
    private val pablo = Contact("2", "Pablo", listOf("600333444"), Relation.GRANDCHILD, aliases = listOf("el chico"))
    private val marta = Contact("3", "Marta", listOf("600555666"), Relation.GRANDCHILD)
    private val martin = Contact("4", "Martín", listOf("600777888"), Relation.FRIEND)
    private val carmen = Contact("5", "Carmen", listOf("600999000"), Relation.SIBLING)
    private val pepe = Contact("6", "José", listOf("611000111"), Relation.PARTNER)
    private val directory = ContactDirectory(listOf(laura, pablo, marta, martin, carmen, pepe))

    private fun found(spoken: String) = (directory.resolve(spoken) as Resolution.Found).contact

    @Test
    fun `aliases set by the family`() {
        assertEquals(pablo, found("al chico"))
        assertEquals(laura, found("a la niña"))
    }

    @Test
    fun `names in any case and without accents`() {
        assertEquals(laura, found("a laura"))
        assertEquals(laura, found("GÓMEZ"))
        assertEquals(martin, found("martin"))
    }

    @Test
    fun `diminutives and nicknames`() {
        assertEquals(pablo, found("a pablito"))
        assertEquals(laura, found("laurita"))
        assertEquals(laura, found("lauri"))
        assertEquals(carmen, found("carmencita"))
        assertEquals(pepe, found("a pepe"))
    }

    @Test
    fun `an exact name hides a similar one`() {
        assertEquals(marta, found("marta"))
        assertEquals(martin, found("martín"))
    }

    @Test
    fun `a transcription slip of one letter still finds the name`() {
        assertEquals(carmen, found("carmel"))
    }

    @Test
    fun `relations from the person's point of view`() {
        assertEquals(laura, found("mi hija"))
        assertEquals(carmen, found("a mi hermana"))
        assertEquals(pepe, found("a mi marido"))
    }

    @Test
    fun `two people with the same relation are ambiguous so Peri asks`() {
        val r = directory.resolve("a mi nieto")
        assertIs<Resolution.Ambiguous>(r)
        assertEquals(setOf(pablo, marta), r.candidates.toSet())
    }

    @Test
    fun `a relation narrows a shared name`() {
        val twoLauras = ContactDirectory(listOf(laura, Contact("7", "Laura", listOf("622000000"), Relation.FRIEND)))
        assertEquals(laura, (twoLauras.resolve("mi hija laura") as Resolution.Found).contact)
    }

    @Test
    fun `unknown people are not found`() {
        assertEquals(Resolution.NotFound, directory.resolve("a Fernando"))
        assertEquals(Resolution.NotFound, directory.resolve("a mi"))
        assertEquals(Resolution.NotFound, directory.resolve("al fontanero"))
    }

    @Test
    fun `numbers are compared without prefix or spaces`() {
        assertEquals(laura, directory.byPhone("600111222"))
        assertEquals(pablo, directory.byPhone("+34 600 33 34 44"))
        assertNull(directory.byPhone("+34 699 000 000"))
        assertNull(directory.byPhone("Correos"))
    }
}
