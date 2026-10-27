package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals

private class Quiet : DevicePort {
    override fun speak(text: String) = Unit
    override fun call(phone: String) = Unit
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
}

class MoodTest {
    @Test
    fun `after the call she puts the phone down`() {
        val d = object : DevicePort {
            override fun speak(text: String) = Unit
            override fun call(phone: String) = Unit
            override fun sendSms(phone: String, text: String) = Unit
            override fun lastKnownLocation(): String? = null
        }
        val a = PeriAgent(d, listOf(Contact("1", "Ana", listOf("600111222"), Relation.CHILD)))
        a.onHeard("llama a Ana")
        a.onHeard("sí")
        assertEquals(Mood.CALLING, a.mood)
        a.onCallStateChanged(active = true)
        a.onCallStateChanged(active = false)
        assertEquals(Mood.CALM, a.mood)
    }

    private val laura = Contact("1", "Laura", listOf("600111222"), Relation.CHILD)
    private fun agent() = PeriAgent(Quiet(), listOf(laura))
    private fun sms(text: String) = IncomingMessage(Channel.SMS, "+34 699 000 111", text, 0)

    @Test
    fun `calm by default and after ordinary answers`() {
        val a = agent()
        assertEquals(Mood.CALM, a.mood)
        a.onHeard("hace un día precioso")
        assertEquals(Mood.CALM, a.mood)
    }

    @Test
    fun `a scam makes her protective and a doubt worried`() {
        val a = agent()
        a.onMessage(sms("Correos: pague 1,99 € en correos-envio.top/pago"))
        assertEquals(Mood.PROTECTIVE, a.mood)
        val b = agent()
        b.onMessage(sms("Hola, soy tu sobrino, ¿me prestas 200 € hoy mismo?"))
        assertEquals(Mood.WORRIED, b.mood)
    }

    @Test
    fun `waiting for the model she is thinking`() {
        val a = agent()
        val m = sms("Hola, soy tu sobrino, ¿me prestas 200 € hoy mismo?")
        a.onMessage(m, secondOpinion = true)
        assertEquals(Mood.THINKING, a.mood)
        a.onModelOpinion(m, -2.0)
        assertEquals(Mood.CALM, a.mood)
    }

    @Test
    fun `calling, then happy when a message goes out`() {
        val a = agent()
        a.onHeard("llama a Laura")
        a.onHeard("sí")
        assertEquals(Mood.CALLING, a.mood)
        a.onHeard("dile a Laura que llego tarde")
        a.onHeard("sí")
        assertEquals(Mood.HAPPY, a.mood)
    }

    @Test
    fun `an unknown caller worries her and a fall too`() {
        val a = agent()
        a.onIncomingCall("+34 699 000 111")
        assertEquals(Mood.WORRIED, a.mood)
        a.onHeard("me he caído")
        assertEquals(Mood.WORRIED, a.mood)
        a.onHeard("estoy bien")
        assertEquals(Mood.HAPPY, a.mood)
    }

    @Test
    fun `a remote-control app makes her protective`() {
        val a = agent()
        a.onAppInstalled("com.anydesk.anydeskandroid")
        assertEquals(Mood.PROTECTIVE, a.mood)
    }
}
