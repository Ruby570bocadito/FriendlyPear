package app.friendlypear.core.voice

import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RelationWordsTest {
    @Test
    fun `mum and dad are parents`() {
        for (w in listOf("madre", "mama", "papa", "mother", "mum", "mom", "dad")) assertEquals(Relation.PARENT, RelationWords.of(w))
    }

    @Test
    fun `relation words in saved contact names`() {
        assertEquals(Relation.CHILD to "Laura", RelationWords.inSavedName("Laura hija"))
        assertEquals(Relation.GRANDCHILD to "Pablo", RelationWords.inSavedName("Mi nieto Pablo"))
        assertEquals(Relation.SIBLING to "Carmen", RelationWords.inSavedName("Carmen (hermana)"))
        assertEquals(Relation.PARTNER to "Marido", RelationWords.inSavedName("Marido"))
    }

    @Test
    fun `plain names have no relation`() {
        assertNull(RelationWords.inSavedName("Laura Gómez"))
        assertNull(RelationWords.inSavedName("Fontanero"))
    }

    @Test
    fun `english relations`() {
        assertEquals(Relation.CHILD to "Laura", RelationWords.inSavedName("Laura daughter"))
        assertEquals(Relation.GRANDCHILD to "Pablo", RelationWords.inSavedName("My grandson Pablo"))
        assertEquals(Relation.SIBLING, RelationWords.of("sister"))
    }
}
