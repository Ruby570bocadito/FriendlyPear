package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A phone where the person answers each permission request with [answers]. */
private class AskingDevice(private val answers: Map<Capability, Boolean>) : DevicePort {
    val said = mutableListOf<String>()
    val calls = mutableListOf<String>()
    val sms = mutableListOf<String>()
    val asked = mutableListOf<Capability>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) {
        calls += phone
    }
    override fun sendSms(phone: String, text: String) {
        sms += phone
    }
    override fun lastKnownLocation(): String? = null
    override fun prepare(capability: Capability, then: (Boolean) -> Unit) {
        asked += capability
        then(answers[capability] ?: true)
    }
}

class PermissionTest {
    private val laura = Contact("1", "Laura", listOf("600111222"), Relation.CHILD)

    @Test
    fun `calling asks for the call permission only when it is needed`() {
        val d = AskingDevice(emptyMap())
        val a = PeriAgent(d, listOf(laura))
        a.onHeard("llama a Laura")
        assertTrue(d.asked.isEmpty())
        a.onHeard("sí")
        assertEquals(listOf(Capability.CALL), d.asked)
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `without the call permission Peri gives the number instead`() {
        val d = AskingDevice(mapOf(Capability.CALL to false))
        val a = PeriAgent(d, listOf(laura))
        a.onHeard("llama a Laura")
        a.onHeard("sí")
        assertTrue(d.calls.isEmpty())
        assertEquals(
            "Vale, sin ese permiso no llamo yo. El número de Laura es 600 111 222; si quieres, márcalo tú. Puedes darme el permiso cuando quieras.",
            d.said.last(),
        )
    }

    @Test
    fun `without the sms permission Peri suggests the messages app`() {
        val d = AskingDevice(mapOf(Capability.SEND_SMS to false))
        val a = PeriAgent(d, listOf(laura))
        a.onHeard("dile a Laura que llego tarde")
        a.onHeard("sí")
        assertTrue(d.sms.isEmpty())
        assertTrue(d.said.last().startsWith("Vale, sin ese permiso no mando mensajes."))
    }

    @Test
    fun `the emergency asks for what is missing one by one during the countdown`() {
        val d = AskingDevice(emptyMap())
        PeriAgent(d, listOf(laura)).onHeard("me he caído")
        assertEquals(listOf(Capability.CALL, Capability.SEND_SMS, Capability.LOCATION), d.asked)
    }
}
