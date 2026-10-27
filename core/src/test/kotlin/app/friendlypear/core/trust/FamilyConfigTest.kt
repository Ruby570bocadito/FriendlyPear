package app.friendlypear.core.trust

import app.friendlypear.core.agent.DevicePort
import app.friendlypear.core.agent.PeriAgent
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FamilyConfigTest {
    private val laura = Contact("1", "Laura", listOf("600111222"))
    private val pablo = Contact("2", "Pablo", listOf("600333444"), Relation.GRANDCHILD)

    @Test
    fun `guardians, relations and aliases are applied on top of the contact book`() {
        val config = FamilyConfig()
            .withGuardian("1", true)
            .withRelation("1", Relation.CHILD)
            .withAliases("2", " el chico , Pablito,,el chico ")
        val (l, p) = config.applyTo(listOf(laura, pablo))
        assertEquals(TrustLevel.GUARDIAN, l.trust)
        assertEquals(Relation.CHILD, l.relation)
        assertEquals(listOf("el chico", "Pablito"), p.aliases)
        assertEquals(Relation.GRANDCHILD, p.relation)
    }

    @Test
    fun `removing settings brings back the contact book`() {
        val config = FamilyConfig().withGuardian("1", true).withGuardian("1", false).withRelation("2", null).withAliases("2", "  ")
        assertEquals(listOf(laura, pablo), config.applyTo(listOf(laura, pablo)))
    }

    @Test
    fun `at most five aliases per person`() {
        assertEquals(FamilyConfig.MAX_ALIASES, FamilyConfig().withAliases("1", "a,b,c,d,e,f,g").aliases.getValue("1").size)
    }

    @Test
    fun `the agent uses them - the chosen guardian is asked first, the alias is understood`() {
        val said = mutableListOf<String>()
        val device = object : DevicePort {
            override fun speak(text: String) {
                said += text
            }
            override fun call(phone: String) = Unit
            override fun sendSms(phone: String, text: String) = Unit
            override fun lastKnownLocation(): String? = null
        }
        val family = FamilyConfig().withGuardian("2", true).withAliases("2", "el chico").applyTo(listOf(laura, pablo))
        val agent = PeriAgent(device, family)
        agent.onHeard("me he caído")
        assertTrue(said.last().endsWith("en diez segundos aviso a Pablo."))
        agent.onHeard("cancela")
        agent.onHeard("llama al chico")
        assertEquals("¡Claro! ¿Llamo a Pablo?", said.last())
    }
}
