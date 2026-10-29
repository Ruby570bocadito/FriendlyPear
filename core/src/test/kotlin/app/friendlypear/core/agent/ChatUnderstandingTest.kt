package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import app.friendlypear.core.voice.Command
import app.friendlypear.core.voice.VoiceCommands
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What the chat understands of what people really write: their language, vague trouble, and a message pasted in. */
class ChatUnderstandingTest {
    private fun topic(s: String) = (VoiceCommands.parse(s) as? Command.Advise)?.topic

    @Test
    fun `the language a message is written in`() {
        assertEquals(Lang.ES, Language.of("Tengo un problema"))
        assertEquals(Lang.ES, Language.of("Mi banco me llamo"))
        assertEquals(Lang.ES, Language.of("me han liado"))
        assertEquals(Lang.ES, Language.of("¿Qué hago?"))
        assertEquals(Lang.ES, Language.of("hola peri, que tal"))
        assertEquals(Lang.EN, Language.of("my bank called me"))
        assertEquals(Lang.EN, Language.of("I have a problem"))
        assertEquals(Lang.EN, Language.of("is this link safe?"))
        assertEquals(Lang.EN, Language.of("hi Peri, how are you"))
        assertNull(Language.of("ok"))
        assertNull(Language.of("Laura"))
        assertNull(Language.of("612 345 678"))
    }

    @Test
    fun `vague trouble gets a calm question in both languages`() {
        assertEquals(Topic.TROUBLE, topic("Tengo un problema"))
        assertEquals(Topic.TROUBLE, topic("tengo un problema, no se que hacer"))
        assertEquals(Topic.TROUBLE, topic("creo que me han estafado"))
        assertEquals(Topic.TROUBLE, topic("me han liado"))
        assertEquals(Topic.TROUBLE, topic("estoy preocupada"))
        assertEquals(Topic.TROUBLE, topic("I have a problem"))
        assertEquals(Topic.TROUBLE, topic("I think I've been scammed"))
        assertEquals(Topic.TROUBLE, topic("I need help with something"))
        assertEquals(Topic.CHECK_MESSAGE, topic("me ha llegado un mensaje raro"))
        assertEquals(Topic.CHECK_MESSAGE, topic("I got a strange message"))
    }

    @Test
    fun `anything more precise wins, and orders stay orders`() {
        assertEquals(Topic.BANK_CALL, topic("tengo un problema con el banco"))
        assertEquals(Topic.BANK_CALL, topic("Mi banco me llamo esta manana"))
        assertEquals(Topic.BANK_CALL, topic("me han llamado del banco"))
        assertEquals(Topic.PARCEL, topic("me llegó un mensaje de correos"))
        assertNull(topic("me llamo Ana"))
        assertEquals(Topic.BIZUM, topic("creo que me han estafado por bizum"))
        assertEquals(Topic.PARCEL, topic("me ha llegado un mensaje de correos"))
        assertIs<Command.SendMessage>(VoiceCommands.parse("dile a Laura que tengo un problema"))
        assertIs<Command.Call>(VoiceCommands.parse("llama a mi hija que tengo un problema"))
    }

    @Test
    fun `a message pasted or retold is found, questions are not`() {
        assertEquals(
            "Su paquete esta retenido. Pague 1,99 EUR en correos-envio.net",
            PastedMessage.find("Me ha llegado esto: Su paquete esta retenido. Pague 1,99 EUR en correos-envio.net"),
        )
        assertEquals("Your account is locked, confirm at amzn-verify.co today", PastedMessage.find("is this a scam? \"Your account is locked, confirm at amzn-verify.co today\""))
        assertNotNull(PastedMessage.find("BBVA: Hemos detectado un acceso no autorizado. Verifique su identidad en bbva-seguridad.com hoy"))
        assertNotNull(PastedMessage.find("Hola mama este es mi numero nuevo, hazme un bizum de 480 euros que es urgente"))
        assertNull(PastedMessage.find("¿es seguro bbva.es?"))
        assertNull(PastedMessage.find("Tengo un problema"))
        assertNull(PastedMessage.find("me ha llamado el banco, ¿qué hago?"))
        assertNull(PastedMessage.find("llama a mi hija"))
    }

    @Test
    fun `Peri answers in the chat's language and goes back to the phone's`() {
        val device = object : DevicePort {
            val said = mutableListOf<String>()
            override fun speak(text: String) {
                said += text
            }
            override fun call(phone: String) = Unit
            override fun sendSms(phone: String, text: String) = Unit
            override fun lastKnownLocation(): String? = null
        }
        val agent = PeriAgent(device, listOf(Contact("1", "Laura", listOf("600111222"), Relation.CHILD)), Lang.EN)
        agent.answerIn(Lang.ES)
        agent.onHeard("Tengo un problema")
        assertEquals(Phrases.of(Lang.ES).advice(Topic.TROUBLE), device.said.last())
        agent.answerIn(Lang.EN)
        agent.onHeard("I have a problem")
        assertEquals(Phrases.of(Lang.EN).advice(Topic.TROUBLE), device.said.last())
        assertTrue(device.said.size == 2)
    }

    @Test
    fun `written alone, help is trouble to talk through, and an emergency is still one`() {
        val device = object : DevicePort {
            val said = mutableListOf<String>()
            override fun speak(text: String) {
                said += text
            }
            override fun call(phone: String) = Unit
            override fun sendSms(phone: String, text: String) = Unit
            override fun lastKnownLocation(): String? = null
        }
        val agent = PeriAgent(device, emptyList(), Lang.ES)
        agent.onWritten("ayuda")
        assertEquals(Phrases.of(Lang.ES).advice(Topic.TROUBLE), device.said.last())
        assertTrue(!agent.isCountingDown)
        agent.onWritten("emergencia, me he caido")
        assertTrue(agent.isCountingDown || device.said.last() != Phrases.of(Lang.ES).advice(Topic.TROUBLE))
    }
}
