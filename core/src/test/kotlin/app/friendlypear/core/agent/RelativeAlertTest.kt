package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class AlertDevice : DevicePort {
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

class RelativeAlertTest {
    private val mum = Contact("1", "Mamá", listOf("5554"), Relation.PARENT)
    private val marisa = Contact("2", "Marisa", listOf("600111222"), Relation.FRIEND)

    @Test
    fun `a relative's fall alert offers to call them, not a scam warning`() {
        val d = AlertDevice()
        val a = PeriAgent(d, listOf(mum), Lang.EN)
        val alert = IncomingMessage(Channel.SMS, "5554", "Alert from Peri: your relative said they have fallen and did not cancel the alert. Location: https://maps.google.com/?q=40.41680,-3.70380", 0)
        val verdict = a.onMessage(alert)
        assertEquals(Level.OK, verdict.level)
        assertEquals("An alert from Mamá's Peri: they said they've fallen and didn't cancel it. Shall I call Mamá now?", d.said.last())
        a.onHeard("yes")
        assertEquals(listOf("5554"), d.calls)
    }

    @Test
    fun `after a bank's call, an order is still an order`() {
        val d = AlertDevice()
        var now = 1_000_000L
        val a = PeriAgent(d, listOf(mum, marisa), Lang.EN, clock = { now })
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = false)
        a.onCallStateChanged(active = true, answered = true)
        // A minute's talk: a few seconds gets no question afterwards.
        now += 60_000L
        a.onCallStateChanged(active = false)
        a.onHeard("the bank")
        assertTrue(d.said.last().endsWith("What did they ask you for?"))
        a.onHeard("call Marisa")
        assertTrue(d.said.last().contains("Marisa"), d.said.last())
        a.onHeard("yes")
        assertEquals(listOf("600111222"), d.calls)
    }
}
