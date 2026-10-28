package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import app.friendlypear.core.voice.Command
import app.friendlypear.core.voice.VoiceCommands
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class Listener : DevicePort {
    val said = mutableListOf<String>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) = Unit
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
}

class AdviceTest {
    private fun topic(s: String) = (VoiceCommands.parse(s) as? Command.Advise)?.topic

    @Test
    fun `questions and situations get their topic in both languages`() {
        assertEquals(Topic.BANK_CALL, topic("Me ha llamado el banco, ¿qué hago?"))
        assertEquals(Topic.BANK_CALL, topic("me llama alguien del banco"))
        assertEquals(Topic.CODE, topic("me piden un código que me ha llegado por SMS"))
        assertEquals(Topic.LINK, topic("¿es seguro este enlace?"))
        assertEquals(Topic.PRIZE, topic("he ganado un premio"))
        assertEquals(Topic.FAMILY_MONEY, topic("mi nieto me pide dinero desde un número nuevo"))
        assertEquals(Topic.REMOTE_APP, topic("me piden que instale el anydesk"))
        assertEquals(Topic.STOLEN_PHONE, topic("me han robado el móvil, ¿qué hago?"))
        assertEquals(Topic.WHATSAPP, topic("¿cómo protejo mi WhatsApp?"))
        assertEquals(Topic.WHAT_IS_A_SCAM, topic("¿qué es el phishing?"))
        assertEquals(Topic.WHAT_IS_A_SCAM, topic("phishing"))
        assertEquals(Topic.BIZUM, topic("bizum"))
        assertEquals(Topic.BANK_CALL, topic("my bank called me, what should I do?"))
        assertEquals(Topic.LINK, topic("is this link safe?"))
        assertEquals(Topic.PARCEL, topic("I got a text about a parcel"))
        assertEquals(Topic.INVESTMENT, topic("someone offers me crypto"))
    }

    /** The questions the chat offers as buttons (android res, chat_s_*), in both languages. */
    @Test
    fun `every question the chat offers is understood`() {
        val asked = listOf(
            "Me ha llamado mi banco" to Topic.BANK_CALL, "My bank called me" to Topic.BANK_CALL,
            "Me piden un código" to Topic.CODE, "Someone asks me for a code" to Topic.CODE,
            "¿Es seguro este enlace?" to Topic.LINK, "Is this link safe?" to Topic.LINK,
            "Un familiar me pide dinero" to Topic.FAMILY_MONEY, "A relative asks me for money" to Topic.FAMILY_MONEY,
            "Me piden que instale una app" to Topic.REMOTE_APP, "They asked me to install an app" to Topic.REMOTE_APP,
            "He ganado un premio" to Topic.PRIZE, "I won a prize" to Topic.PRIZE,
            "¿Cómo protejo mi WhatsApp?" to Topic.WHATSAPP, "How do I protect my WhatsApp?" to Topic.WHATSAPP,
            "Me han robado el móvil" to Topic.STOLEN_PHONE, "Someone stole my phone" to Topic.STOLEN_PHONE,
            "¿Qué es el phishing?" to Topic.WHAT_IS_A_SCAM, "What is phishing?" to Topic.WHAT_IS_A_SCAM,
            "¿Qué hago si me quedo sin línea?" to Topic.SIM_SWAP, "What if my phone loses its line?" to Topic.SIM_SWAP,
            "¿Es seguro comprar por internet?" to Topic.SHOPPING, "Is it safe to shop online?" to Topic.SHOPPING,
            "Me llaman de la policía" to Topic.FAKE_AUTHORITY, "The police called me" to Topic.FAKE_AUTHORITY,
        )
        for ((words, expected) in asked) assertEquals(expected, topic(words), words)
        val given = listOf(
            "Ya les he dado mis datos", "I already gave them my details", "He instalado la app que me pidieron", "I installed the app they asked for",
        )
        for (words in given) assertTrue(VoiceCommands.parse(words) is Command.Gave, words)
        assertEquals(Breach.REMOTE, (VoiceCommands.parse("He instalado la app que me pidieron") as Command.Gave).what)
        assertEquals(Breach.REMOTE, (VoiceCommands.parse("I installed the app they asked for") as Command.Gave).what)
    }

    @Test
    fun `orders stay orders, and things already given away stay first aid`() {
        assertTrue(VoiceCommands.parse("llama a Laura") is Command.Call)
        assertTrue(VoiceCommands.parse("llama al banco") is Command.Call)
        assertTrue(VoiceCommands.parse("léeme los mensajes") is Command.ReadMessages)
        assertEquals(Breach.CODE, (VoiceCommands.parse("le he dado el código a uno del banco") as? Command.Gave)?.what)
        assertEquals(Breach.SOMETHING, (VoiceCommands.parse("creo que les he dado algo") as? Command.Gave)?.what)
        assertTrue(VoiceCommands.parse("buenos días") is Command.Unknown)
    }

    @Test
    fun `every answer is reviewed, speakable and has a next step in both languages`() {
        for (lang in Lang.entries) {
            val p = Phrases.of(lang)
            for (t in Topic.entries) {
                val text = p.advice(t)
                assertTrue(text.length in 80..330, "$lang $t: ${text.length}")
                assertFalse(Regex("\\d{3,}|https?://|www\\.").containsMatchIn(text), "$lang $t")
            }
        }
        val d = Listener()
        PeriAgent(d, listOf(Contact("1", "Laura", listOf("600111222"), Relation.CHILD)), Lang.ES).onHeard("me ha llamado el banco")
        assertTrue(d.said.last().startsWith("Tu banco nunca te pedirá claves"), d.said.last())
    }
}
