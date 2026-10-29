package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class CallDevice : DevicePort {
    val said = mutableListOf<String>()
    val calls = mutableListOf<String>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) {
        calls += phone
    }
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
}

class CallerCheckTest {
    private val pablo = Contact("1", "Pablo", listOf("600333444"), Relation.GRANDCHILD)
    private val marta = Contact("2", "Marta", listOf("600555666"), Relation.GRANDCHILD)
    private val laura = Contact("3", "Laura", listOf("600111222"), Relation.CHILD)
    private val stranger = "+34 699 000 111"

    /** The agents' clock: a call lasts as long as the test says. */
    private var now = 1_000_000L

    private fun agent(d: CallDevice, contacts: List<Contact> = listOf(pablo, laura), lang: Lang = Lang.ES, word: Boolean = false) =
        PeriAgent(d, contacts, lang, hasFamilyWord = word, clock = { now })

    /** Rings, is picked up, a talk of [talkMs] (a minute by default), then ends. */
    private fun PeriAgent.answeredCall(number: String, talkMs: Long = 60_000L) {
        onIncomingCall(number)
        onCallStateChanged(active = true, answered = false)
        onCallStateChanged(active = true, answered = true)
        now += talkMs
        onCallStateChanged(active = false)
    }

    @Test
    fun `a few seconds is no call to ask about, and a long one gets the pointed question`() {
        val short = CallDevice()
        agent(short).answeredCall(stranger, talkMs = 5_000L)
        assertEquals(1, short.said.size, "only the warning before picking up: ${short.said}")
        val long = CallDevice()
        agent(long).answeredCall(stranger, talkMs = 4 * 60_000L)
        assertEquals(
            "Has hablado un buen rato con un número que no está en tu agenda. ¿Quién decía ser, y te ha pedido algo: un código, dinero o instalar una app?",
            long.said.last(),
        )
        val english = CallDevice()
        agent(english, lang = Lang.EN).answeredCall(stranger, talkMs = 4 * 60_000L)
        assertTrue(english.said.last().startsWith("That was a long call"))
    }

    @Test
    fun `an unknown number gets a warning before picking up`() {
        val d = CallDevice()
        agent(d).onIncomingCall(stranger)
        assertEquals(
            "Te llama un número que no está en tu agenda. Si dice ser de tu familia, no des dinero ni códigos; al colgar lo comprobamos juntos.",
            d.said.single(),
        )
    }

    @Test
    fun `with a family word Peri reminds to ask for it but never says it`() {
        val d = CallDevice()
        agent(d, word = true).onIncomingCall(stranger)
        assertTrue(d.said.single().endsWith("y pregúntale la palabra de la familia."))
    }

    @Test
    fun `a known number rings quietly`() {
        val d = CallDevice()
        val a = agent(d)
        a.onIncomingCall("+34 600 333 444")
        a.onCallStateChanged(active = true, answered = true)
        a.onCallStateChanged(active = false)
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `after the call Peri asks who they said they were`() {
        val d = CallDevice()
        agent(d).answeredCall(stranger)
        assertEquals("Ese número no está en tu agenda. ¿Quién decía ser?", d.said.last())
    }

    @Test
    fun `a missed call from an unknown number is not questioned`() {
        val d = CallDevice()
        val a = agent(d)
        a.onIncomingCall(stranger)
        a.onCallStateChanged(active = true, answered = false)
        a.onCallStateChanged(active = false)
        assertEquals(1, d.said.size)
    }

    @Test
    fun `my grandson - that number isn't Pablo's - one yes calls the real Pablo`() {
        val d = CallDevice()
        val a = agent(d)
        a.answeredCall(stranger)
        a.onHeard("decía ser mi nieto")
        assertTrue(d.said.last().startsWith("Ese número no es el de Pablo."))
        assertTrue(d.said.last().contains("palabra de la familia"))
        assertTrue(d.said.last().endsWith("¿Llamo a Pablo a su número de siempre para comprobarlo?"))
        a.onHeard("sí")
        assertEquals(listOf("600333444"), d.calls)
    }

    @Test
    fun `two grandchildren - Peri asks whom to call`() {
        val d = CallDevice()
        val a = agent(d, contacts = listOf(pablo, marta))
        a.answeredCall(stranger)
        a.onHeard("mi nieto")
        assertEquals("Ese número no es el de ninguno de ellos. ¿A quién llamo para comprobarlo, a Pablo o Marta?", d.said.last())
        a.onHeard("a Marta")
        a.onHeard("sí")
        assertEquals(listOf("600555666"), d.calls)
    }

    @Test
    fun `a bank on the phone - hang up and call the official number yourself, then what did they ask`() {
        val d = CallDevice()
        val a = agent(d, contacts = listOf(laura))
        a.answeredCall(stranger)
        a.onHeard("del banco")
        assertTrue(d.said.last().startsWith("Si decía ser del banco"))
        assertTrue(d.said.last().endsWith("¿Qué te pidieron?"))
        assertTrue(a.isWaitingForAnswer)
        a.onHeard("que les dijera el código que me llegó por SMS")
        assertTrue(d.said.last().startsWith("Pedirte un código es el truco"))
        assertTrue(d.said.last().endsWith("¿Llamo a Laura para contárselo?"))
        a.onHeard("sí")
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `what they asked for, told at once, is enough`() {
        val d = CallDevice()
        val a = agent(d)
        a.answeredCall(stranger)
        a.onHeard("de la policía, que pasara mi dinero a una cuenta segura")
        assertTrue(d.said.last().startsWith("Mover el dinero a una «cuenta segura» es el truco"))
    }

    @Test
    fun `a bank that only wanted to talk is a doubt, and no answer gets the usual advice`() {
        val d = CallDevice()
        val a = agent(d)
        a.answeredCall(stranger)
        a.onHeard("del banco")
        a.onHeard("no me pidieron nada, solo que esperara")
        assertTrue(d.said.last().startsWith("Vale. Si alguien te pide dinero"))
        a.answeredCall(stranger)
        a.onHeard("de Hacienda")
        a.onHeard("no")
        assertTrue(d.said.last().startsWith("Vale. Si alguien te pide dinero"))
    }

    @Test
    fun `phone-scam scripts in english`() {
        val d = CallDevice()
        val a = agent(d, contacts = listOf(laura), lang = Lang.EN)
        a.answeredCall(stranger)
        a.onHeard("Microsoft support, they wanted me to install AnyDesk")
        assertTrue(d.said.last().startsWith("Installing an app they ask for on the phone is the trick"))
        assertTrue(d.said.last().endsWith("Shall I call Laura to tell them?"))
    }

    @Test
    fun `nobody or someone unknown - general advice`() {
        val d = CallDevice()
        val a = agent(d)
        a.answeredCall(stranger)
        a.onHeard("no")
        assertTrue(d.said.last().startsWith("Vale. Si alguien te pide dinero o códigos"))
    }

    @Test
    fun `in english too`() {
        val d = CallDevice()
        val a = agent(d, lang = Lang.EN)
        a.onIncomingCall("+44 7700 900 555")
        assertTrue(d.said.last().startsWith("This number isn't in your contacts."))
        a.onCallStateChanged(active = true, answered = true)
        now += 60_000L
        a.onCallStateChanged(active = false)
        assertEquals("That number isn't in your contacts. Who did they say they were?", d.said.last())
        a.onHeard("he said he was my grandson")
        assertTrue(d.said.last().startsWith("That number isn't Pablo's."))
        a.onHeard("yes")
        assertEquals(listOf("600333444"), d.calls)
    }

    @Test
    fun `an emergency still wins over the question`() {
        val d = CallDevice()
        val a = agent(d)
        a.answeredCall(stranger)
        a.onHeard("me he caído")
        assertTrue(a.isCountingDown)
    }
}
