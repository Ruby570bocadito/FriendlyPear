package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingDevice : DevicePort {
    val said = mutableListOf<String>()
    val calls = mutableListOf<String>()
    val sms = mutableListOf<Pair<String, String>>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) {
        calls += phone
    }
    override fun sendSms(phone: String, text: String) {
        sms += phone to text
    }
    override fun lastKnownLocation(): String? = "51.50,-0.12"
}

class PeriAgentEnglishTest {
    private val laura = Contact("1", "Laura", listOf("07700900111"), Relation.CHILD, trust = TrustLevel.GUARDIAN)
    private val pablo = Contact("2", "Pablo", listOf("07700900222"), Relation.GRANDCHILD, aliases = listOf("the boy"))
    private val family = listOf(laura, pablo)

    private fun agent(d: RecordingDevice) = PeriAgent(d, family, Lang.EN)

    private fun message(text: String, sender: String = "+44 7700 900999", channel: Channel = Channel.WHATSAPP) =
        IncomingMessage(channel, sender, text, 0)

    @Test
    fun `calls in English after a spoken yes`() {
        val d = RecordingDevice()
        val a = agent(d)
        a.onHeard("Hey Peri, call my daughter please")
        assertEquals("Shall I call Laura?", d.said.last())
        a.onHeard("yes")
        assertEquals(listOf("07700900111"), d.calls)
        assertEquals("Calling Laura.", d.said.last())
    }

    @Test
    fun `aliases in English`() {
        val d = RecordingDevice()
        agent(d).onHeard("ring the boy")
        assertEquals("Shall I call Pablo?", d.said.last())
    }

    @Test
    fun `texts in English with the person's own words`() {
        val d = RecordingDevice()
        val a = agent(d)
        a.onHeard("tell Laura that I'll be late")
        assertEquals("Shall I send Laura this message: \"I'll be late\"?", d.said.last())
        a.onHeard("go ahead")
        assertEquals(listOf("07700900111" to "I'll be late"), d.sms)
    }

    @Test
    fun `moment 1 in English`() {
        val d = RecordingDevice()
        val a = agent(d)
        val v = a.onMessage(message("Hi Mum, I broke my phone, this is my new number. Can you send me money for a new one?"))
        assertTrue(v.level != Level.OK)
        assertTrue(d.said.last().startsWith("It claims to be family from a new number"))
        assertTrue(d.said.last().endsWith("If you say yes, I'll call Laura."))
        a.onHeard("yes please")
        assertEquals(listOf("07700900111"), d.calls)
    }

    @Test
    fun `moment 2 in English`() {
        val d = RecordingDevice()
        val a = agent(d)
        a.onCallStateChanged(true)
        val v = a.onMessage(message("Your bank: your verification code is 482913", sender = "Barclays", channel = Channel.SMS))
        assertEquals(Level.SCAM, v.level)
        assertTrue(d.said.last().contains("hang up"))
        assertFalse(d.said.last().contains("482913"))
    }

    @Test
    fun `reads messages in English without links`() {
        val d = RecordingDevice()
        val a = agent(d)
        a.onMessage(message("Lunch on Sunday? Look at www.pub.co.uk", sender = "Laura"))
        a.onHeard("read me my messages")
        assertEquals("From Laura: Lunch on Sunday? Look at a link", d.said.last())
    }

    @Test
    fun `emergency in English`() {
        val d = RecordingDevice()
        val a = agent(d)
        a.onHeard("I've fallen")
        assertTrue(a.isCountingDown)
        assertTrue(d.said.last().contains("say \"cancel\""))
        a.onEmergencyTimeout()
        assertTrue(d.sms.single().second.startsWith("Alert from Peri"))
        assertEquals(listOf("07700900111"), d.calls)
    }

    @Test
    fun `a Spanish order still works when Peri speaks English`() {
        val d = RecordingDevice()
        agent(d).onHeard("llama a mi hija")
        assertEquals("Shall I call Laura?", d.said.last())
    }
}
