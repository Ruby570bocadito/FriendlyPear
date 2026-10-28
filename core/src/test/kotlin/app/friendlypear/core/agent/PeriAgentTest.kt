package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeDevice(
    val failingPhones: Set<String> = emptySet(),
    val location: String? = "40.41,-3.70",
) : DevicePort {
    val said = mutableListOf<String>()
    val calls = mutableListOf<String>()
    val sms = mutableListOf<Pair<String, String>>()

    override fun speak(text: String) {
        said += text
    }

    override fun call(phone: String) {
        if (phone in failingPhones) error("no signal")
        calls += phone
    }

    override fun sendSms(phone: String, text: String) {
        if (phone in failingPhones) error("no signal")
        sms += phone to text
    }

    override fun lastKnownLocation(): String? = location
}

class PeriAgentTest {
    private val laura = Contact("1", "Laura", listOf("600111222", "910000001"), Relation.CHILD, trust = TrustLevel.GUARDIAN)
    private val pablo = Contact("2", "Pablo", listOf("600333444"), Relation.GRANDCHILD, aliases = listOf("el chico"))
    private val marta = Contact("3", "Marta", listOf("600555666"), Relation.GRANDCHILD)
    private val family = listOf(laura, pablo, marta)

    private fun agent(device: FakeDevice, contacts: List<Contact> = family) = PeriAgent(device, contacts)

    private fun message(text: String, sender: String = "+34 699 000 000", channel: Channel = Channel.WHATSAPP) =
        IncomingMessage(channel, sender, text, 0)

    @Test
    fun `a call needs a spoken yes before dialling`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama al chico")
        assertEquals("¡Claro! ¿Llamo a Pablo?", d.said.last())
        assertTrue(d.calls.isEmpty())
        assertTrue(a.isWaitingForAnswer)

        a.onHeard("sí")
        assertEquals(listOf("600333444"), d.calls)
        assertFalse(a.isWaitingForAnswer)
    }

    @Test
    fun `a no means nothing happens`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a Pablito")
        a.onHeard("no")
        assertTrue(d.calls.isEmpty())
        assertEquals("Vale, no hago nada. Aquí sigo si me necesitas.", d.said.last())
    }

    @Test
    fun `an unclear answer repeats the question instead of acting`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a Laura")
        a.onHeard("hace frío hoy")
        assertTrue(d.calls.isEmpty())
        assertEquals("Perdona, no te he oído bien. Dime «sí» o «no»: ¡Claro! ¿Llamo a Laura?", d.said.last())
    }

    @Test
    fun `a new order replaces the pending question`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a Laura")
        a.onHeard("no, llama a Pablo")
        assertEquals("¡Claro! ¿Llamo a Pablo?", d.said.last())
        a.onHeard("vale")
        assertEquals(listOf("600333444"), d.calls)
    }

    @Test
    fun `with two grandchildren Peri asks who and then confirms`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a mi nieto")
        assertEquals("¿A quién te refieres, a Pablo o Marta?", d.said.last())
        a.onHeard("a Marta")
        assertEquals("¡Claro! ¿Llamo a Marta?", d.said.last())
        a.onHeard("sí")
        assertEquals(listOf("600555666"), d.calls)
    }

    @Test
    fun `unknown people are not called`() {
        val d = FakeDevice()
        agent(d).onHeard("llama al fontanero")
        assertTrue(d.calls.isEmpty())
        assertEquals("Vaya, no encuentro a esa persona en tu agenda. ¿Me dices cómo se llama?", d.said.last())
    }

    @Test
    fun `a failed call tries the next saved number`() {
        val d = FakeDevice(failingPhones = setOf("600111222"))
        val a = agent(d)
        a.onHeard("llama a mi hija")
        a.onHeard("sí")
        assertEquals(listOf("910000001"), d.calls)
    }

    @Test
    fun `when every number fails Peri gives the number to dial by hand`() {
        val d = FakeDevice(failingPhones = setOf("600333444"))
        val a = agent(d)
        a.onHeard("llama a Pablo")
        a.onHeard("sí")
        assertEquals("Vaya, no he podido llamar a Pablo. Su número es 600 333 444; si quieres, márcalo tú.", d.said.last())
    }

    @Test
    fun `a message is read back and sent only after a yes`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("dile a Laura que llego tarde")
        assertEquals("¡Claro! ¿Le mando a Laura este mensaje: «llego tarde»?", d.said.last())
        assertTrue(d.sms.isEmpty())
        a.onHeard("sí")
        assertEquals(listOf("600111222" to "llego tarde"), d.sms)
        assertEquals("¡Listo! Mensaje enviado a Laura.", d.said.last())
    }

    @Test
    fun `if the message fails Peri offers to call`() {
        val d = FakeDevice(failingPhones = setOf("600333444"))
        val a = agent(d)
        a.onHeard("dile a Pablo que mañana no voy")
        a.onHeard("sí")
        assertEquals("Vaya, no he podido mandar el mensaje a Pablo. ¿Prefieres que llame?", d.said.last())
        assertTrue(a.isWaitingForAnswer)
    }

    @Test
    fun `dictating a code needs a guardian and is never sent`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("dile a Pablo que el código es 4821")
        assertTrue(d.sms.isEmpty())
        assertFalse(a.isWaitingForAnswer)
        assertTrue(d.said.last().contains("lo apruebe alguien de tu familia"))
    }

    @Test
    fun `moment 1 - a new number claiming to be the daughter leads to calling the real daughter`() {
        val d = FakeDevice()
        val a = agent(d)
        val v = a.onMessage(message("Mamá, se me ha roto el móvil, este es mi número nuevo, ¿me haces un Bizum?"))
        assertTrue(v.level != Level.OK)
        assertTrue(d.said.last().endsWith("Si me dices «sí», llamo ahora mismo a Laura."))
        a.onHeard("sí")
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `moment 2 - a code during a call says hang up at once`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onCallStateChanged(true)
        val v = a.onMessage(message("Tu código de verificación es 482913", sender = "BBVA", channel = Channel.SMS))
        assertEquals(Level.SCAM, v.level)
        assertTrue(d.said.last().startsWith("Cuelga ahora mismo"))
        assertFalse(d.said.last().contains("482913"))
    }

    @Test
    fun `normal messages stay quiet until asked, then are read without links or codes`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onMessage(message("¿Venís a comer el domingo? Mira www.restaurante.es", sender = "Laura"))
        assertTrue(d.said.isEmpty())
        a.onHeard("léeme los mensajes")
        assertEquals("De Laura: ¿Venís a comer el domingo? Mira un enlace", d.said.last())
        a.onHeard("léeme los mensajes")
        assertEquals("No tienes mensajes nuevos. ¡Todo tranquilo!", d.said.last())
    }

    @Test
    fun `scams are never read aloud, only Peri's warning`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onMessage(message("Correos: su paquete está retenido, pague 1,99 € en https://correos-envio.top/pago", sender = "Correos", channel = Channel.SMS))
        a.onHeard("léeme el último mensaje")
        val read = d.said.last()
        assertTrue(read.startsWith("De «Correos»: este no te lo leo, que trae trampa."))
        assertFalse(read.contains("correos-envio"))
    }

    @Test
    fun `unknown numbers are not read aloud as senders`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onMessage(message("Hola, mañana paso a las 10", sender = "+34 611 222 333"))
        a.onHeard("léeme los mensajes")
        assertTrue(d.said.last().startsWith("De un número que no está en tu agenda:"))
    }

    @Test
    fun `emergency cancelled in time calls nobody`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("me he caído")
        assertTrue(a.isCountingDown)
        a.onHeard("estoy bien")
        a.onEmergencyTimeout()
        assertTrue(d.calls.isEmpty())
        assertTrue(d.sms.isEmpty())
    }

    @Test
    fun `emergency not cancelled texts the location to the guardian and calls her`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("socorro")
        a.onHeard("ay")
        a.onEmergencyTimeout()
        assertEquals("600111222", d.sms.single().first)
        assertTrue(d.sms.single().second.contains("40.41,-3.70"))
        assertEquals(listOf("600111222"), d.calls)
        assertFalse(a.isCountingDown)
    }

    @Test
    fun `an emergency wins over a pending question`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a mi nieto")
        a.onHeard("me he caído")
        assertTrue(a.isCountingDown)
    }

    @Test
    fun `repeat says the last sentence again`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a Laura")
        a.onHeard("¿cómo?")
        assertEquals("¡Claro! ¿Llamo a Laura?", d.said.last())
    }

    @Test
    fun `a hello gets a hello, and her lines go round`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("hola")
        a.onHeard("hola")
        assertTrue(d.said[0].startsWith("¡Hola!"))
        assertTrue(d.said[0] != d.said[1])
        a.onHeard("gracias")
        assertEquals(Mood.HAPPY, a.mood)
    }

    @Test
    fun `feeling lonely offers to call family, and a yes calls`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("me siento sola")
        assertEquals("Aquí estoy contigo. ¿Te apetece que llame a Laura?", d.said.last())
        a.onHeard("sí")
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `small talk during a question asks the question again`() {
        val d = FakeDevice()
        val a = agent(d)
        a.onHeard("llama a Laura")
        a.onHeard("gracias")
        assertTrue(d.calls.isEmpty())
        assertEquals("Perdona, no te he oído bien. Dime «sí» o «no»: ¡Claro! ¿Llamo a Laura?", d.said.last())
    }
}
