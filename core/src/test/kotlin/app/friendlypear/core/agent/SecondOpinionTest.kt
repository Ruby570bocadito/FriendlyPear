package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Speaker : DevicePort {
    val said = mutableListOf<String>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) = Unit
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
}

class SecondOpinionTest {
    private val family = listOf(Contact("1", "Laura", listOf("600111222"), Relation.CHILD))

    /** The two messages used on the emulator: the rules alone leave both as doubtful. */
    private val nephew = IncomingMessage(
        Channel.WHATSAPP, "+34 611 222 333",
        "Hola, soy tu sobrino, me he quedado sin batería. ¿Me prestas 200 € hoy mismo?", 0,
    )
    private val gym = IncomingMessage(
        Channel.SMS, "+44 7700 900777",
        "Hi, it's Riverside Gym: your membership expires tomorrow. You can pay at reception when you come in.", 0,
    )

    @Test
    fun `the demo messages are doubtful for the rules alone`() {
        assertEquals(Level.DOUBT, PeriAgent(Speaker(), family).onMessage(nephew).level)
        assertEquals(Level.DOUBT, PeriAgent(Speaker(), family, Lang.EN).onMessage(gym).level)
    }

    @Test
    fun `a doubtful message waits silently for the second opinion`() {
        val d = Speaker()
        val a = PeriAgent(d, family)
        assertEquals(Level.DOUBT, a.onMessage(nephew, secondOpinion = true).level)
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `a scam vote turns it into a scam and Peri warns with her own sentence`() {
        val d = Speaker()
        val a = PeriAgent(d, family)
        a.onMessage(nephew, secondOpinion = true)
        val v = a.onModelOpinion(nephew, 1.9)!!
        assertEquals(Level.SCAM, v.level)
        assertEquals(1.9, v.modelDelta)
        assertTrue(d.said.single().startsWith("Ojo, que esto es una trampa"))
    }

    @Test
    fun `an ok vote calms it down and Peri stays quiet`() {
        val d = Speaker()
        val a = PeriAgent(d, family, Lang.EN)
        a.onMessage(gym, secondOpinion = true)
        assertEquals(Level.OK, a.onModelOpinion(gym, -1.86)!!.level)
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `without an answer the rules' doubt is spoken`() {
        val d = Speaker()
        val a = PeriAgent(d, family)
        a.onMessage(nephew, secondOpinion = true)
        assertEquals(Level.DOUBT, a.onModelOpinion(nephew, null)!!.level)
        assertTrue(d.said.single().startsWith("Este mensaje tiene algo raro"))
    }

    @Test
    fun `the vote is capped at two`() {
        val a = PeriAgent(Speaker(), family)
        a.onMessage(nephew, secondOpinion = true)
        assertEquals(2.0, a.onModelOpinion(nephew, 50.0)!!.modelDelta)
    }

    @Test
    fun `a late or repeated vote is ignored`() {
        val d = Speaker()
        val a = PeriAgent(d, family)
        a.onMessage(nephew, secondOpinion = true)
        a.onModelOpinion(nephew, 1.9)
        assertNull(a.onModelOpinion(nephew, -2.0))
        assertEquals(1, d.said.size)
    }

    @Test
    fun `messages the rules already decided never wait`() {
        val d = Speaker()
        val a = PeriAgent(d, family)
        a.onCallStateChanged(true)
        a.onMessage(IncomingMessage(Channel.SMS, "BBVA", "Tu código es 482913", 0), secondOpinion = true)
        assertTrue(d.said.single().startsWith("Cuelga ahora mismo"))
    }

    @Test
    fun `the read-back uses the verdict after the vote`() {
        val d = Speaker()
        val a = PeriAgent(d, family)
        a.onMessage(nephew, secondOpinion = true)
        a.onModelOpinion(nephew, 1.9)
        a.onHeard("léeme los mensajes")
        assertTrue(d.said.last().contains("este no te lo leo"))
    }
}
