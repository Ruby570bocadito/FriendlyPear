package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import app.friendlypear.core.voice.Command
import app.friendlypear.core.voice.VoiceCommands
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class Helper : DevicePort {
    val said = mutableListOf<String>()
    val shown = mutableListOf<Breach>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) = Unit
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
    override fun showAftercare(what: Breach) {
        shown += what
    }
}

class AftercareTest {
    private fun gave(s: String) = (VoiceCommands.parse(s) as? Command.Gave)?.what

    @Test
    fun `what was given away is understood in both languages`() {
        assertEquals(Breach.CODE, gave("Oye Peri, le he dado el código a un señor"))
        assertEquals(Breach.MONEY, gave("he hecho un bizum a uno que decía ser mi nieto"))
        assertEquals(Breach.CARD, gave("le di los números de la tarjeta"))
        assertEquals(Breach.REMOTE, gave("he instalado la app que me dijeron, el anydesk"))
        assertEquals(Breach.ID, gave("les mandé una foto del DNI"))
        assertEquals(Breach.BANK, gave("puse la clave del banco en el enlace"))
        assertEquals(Breach.PASSWORD, gave("me han robado la cuenta"))
        assertEquals(Breach.PASSWORD, gave("se han filtrado mis datos"))
        assertEquals(Breach.SOMETHING, gave("creo que les he dado algo"))
        assertEquals(Breach.CODE, gave("I gave them the code"))
        assertEquals(Breach.MONEY, gave("I sent money to a man who called"))
        assertEquals(Breach.CARD, gave("I typed my card number in the link"))
    }

    @Test
    fun `questions and orders that only mention a code or a card are not taken for it`() {
        assertEquals(null, gave("cuál es mi pin"))
        assertEquals(null, gave("léeme el código"))
        assertTrue(VoiceCommands.parse("llama a Laura") is Command.Call)
        assertEquals(null, gave("what is my card number"))
    }

    @Test
    fun `Peri answers calmly with the first step and shows the rest`() {
        val d = Helper()
        val a = PeriAgent(d, listOf(Contact("1", "Laura", listOf("600111222"), Relation.CHILD)), Lang.ES)
        a.onHeard("le he dado los datos de la tarjeta")
        assertEquals(listOf(Breach.CARD), d.shown)
        assertTrue(d.said.last().contains("llama a tu banco al número que viene en la tarjeta"))
    }
}
