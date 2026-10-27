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

    @Test
    fun `english orders`() {
        assertEquals(Command.Call("my daughter"), parse("Hey Peri, call my daughter please"))
        assertEquals(Command.Call("the boy"), parse("ring the boy"))
        assertEquals(Command.Call("laura"), parse("I want to talk to Laura"))
        assertEquals(Command.SendMessage("laura", "I'll be late"), parse("Tell Laura that I'll be late"))
        assertEquals(Command.SendMessage("pablo", "see you tomorrow"), parse("text Pablo saying see you tomorrow, please"))
        assertEquals(Command.ReadMessages(), parse("Read me my messages"))
        assertEquals(Command.ReadMessages(onlyLast = true), parse("read the last message"))
        assertEquals(Command.ReadMessages(), parse("any new messages?"))
    }

    @Test
    fun `english answers and emergencies`() {
        assertEquals(Command.Yes, parse("yes please"))
        assertEquals(Command.Yes, parse("go ahead"))
        assertEquals(Command.No, parse("not now"))
        assertEquals(Command.Cancel, parse("I'm fine"))
        assertEquals(Command.Repeat, parse("Sorry?"))
        assertEquals(Command.Emergency, parse("I've fallen and I can't get up"))
        assertEquals(Command.Emergency, parse("help!"))
    }

    @Test
    fun `asking someone for help calls them`() {
        assertEquals(Command.Call("mama"), parse("Peri, pide ayuda a mamá"))
        assertEquals(Command.Call("a mama"), parse("llama a mamá, que necesito ayuda"))
        assertEquals(Command.Call("mi hija"), parse("avisa a mi hija"))
        assertEquals(Command.Call("my daughter"), parse("ask my daughter for help"))
        assertEquals(Command.Call("mum"), parse("call mum, I need help"))
    }

    @Test
    fun `help inside an order is not an emergency`() {
        assertEquals(Command.Call("laura"), parse("help me call Laura"))
    }
}
