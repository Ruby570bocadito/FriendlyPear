package app.friendlypear.core

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.TrustLevel
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {
    @Test
    fun `contacts start as acquaintances until the family verifies them`() {
        assertEquals(TrustLevel.ACQUAINTANCE, Contact("1", "Laura", listOf("+34 600 000 000")).trust)
    }
}
