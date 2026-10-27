package app.friendlypear.core.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VoiceCommandsTest {
    private fun parse(s: String) = VoiceCommands.parse(s)

    @Test
    fun `call orders keep the spoken target`() {
        assertEquals(Command.Call("al chico"), parse("Llama al chico"))
        assertEquals(Command.Call("a pablito"), parse("Oye Peri, llámame a Pablito, por favor"))
        assertEquals(Command.Call("mi hija"), parse("ponme con mi hija"))
        assertEquals(Command.Call("laura"), parse("quiero hablar con Laura"))
        assertEquals(Command.Call("a mi nieta"), parse("puedes llamar a mi nieta"))
    }

    @Test
    fun `transcripts without accents or punctuation work the same`() {
        assertEquals(Command.Call("a mi hijo"), parse("llama a mi hijo"))
        assertEquals(Command.ReadMessages(), parse("leeme los mensajes"))
    }

    @Test
    fun `sending a message keeps the person's own words`() {
        assertEquals(Command.SendMessage("laura", "llego tarde"), parse("Dile a Laura que llego tarde"))
        assertEquals(Command.SendMessage("pablito", "mañana no voy"), parse("mándale un mensaje a Pablito diciendo que mañana no voy, por favor"))
        assertEquals(Command.SendMessage("mi hija", "Me llame"), parse("escribe a mi hija que Me llame."))
    }

    @Test
    fun `a fall inside a dictated message still starts the emergency`() {
        assertEquals(Command.Emergency, parse("dile a Laura que me he caído"))
    }

    @Test
    fun `reading messages`() {
        assertEquals(Command.ReadMessages(), parse("Léeme los mensajes"))
        assertEquals(Command.ReadMessages(), parse("¿Qué mensajes tengo?"))
        assertEquals(Command.ReadMessages(onlyLast = true), parse("léeme el último mensaje"))
        assertEquals(Command.ReadMessages(), parse("mis whatsapps"))
    }

    @Test
    fun `a fall or a cry for help is an emergency even inside a longer sentence`() {
        assertEquals(Command.Emergency, parse("Ay, me he caído"))
        assertEquals(Command.Emergency, parse("socorro"))
        assertEquals(Command.Emergency, parse("no me puedo levantar del suelo"))
    }

    @Test
    fun `short answers to Peri`() {
        assertEquals(Command.Yes, parse("Sí"))
        assertEquals(Command.Yes, parse("sí, por favor"))
        assertEquals(Command.Yes, parse("vale"))
        assertEquals(Command.No, parse("No"))
        assertEquals(Command.No, parse("mejor no"))
        assertEquals(Command.Cancel, parse("cancela"))
        assertEquals(Command.Cancel, parse("estoy bien"))
        assertEquals(Command.Repeat, parse("¿cómo?"))
    }

    @Test
    fun `a bare call verb is a yes, not a call to nobody`() {
        assertEquals(Command.Yes, parse("llama"))
    }

    @Test
    fun `anything else is unknown and keeps the original words`() {
        val c = parse("hace un día precioso")
        assertIs<Command.Unknown>(c)
        assertEquals("hace un día precioso", c.utterance)
        assertIs<Command.Unknown>(parse(""))
    }
}
