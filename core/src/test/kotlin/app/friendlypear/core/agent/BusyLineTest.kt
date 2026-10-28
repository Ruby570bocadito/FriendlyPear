package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class Line(var canHangUp: Boolean = true) : DevicePort {
    val said = mutableListOf<String>()
    val calls = mutableListOf<String>()
    val pauses = mutableListOf<Boolean>()
    var hungUp = 0
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) {
        calls += phone
    }
    override fun hangUp(): Boolean {
        if (canHangUp) hungUp++
        return canHangUp
    }
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
    override fun showPause(text: String, callName: String?, hangUp: Boolean) {
        pauses += hangUp
    }
}

class BusyLineTest {
    private val laura = Contact("1", "Laura", listOf("600111222"), Relation.CHILD)
    private val pablo = Contact("2", "Pablo", listOf("600333444"), Relation.GRANDCHILD)

    private fun agent(d: Line, lang: Lang = Lang.ES) = PeriAgent(d, listOf(laura, pablo), lang)

    @Test
    fun `asked to call during a family call, Peri waits for the line and then calls`() {
        val d = Line()
        val a = agent(d)
        a.onIncomingCall("600333444")
        a.onCallStateChanged(active = true, answered = true)
        a.onHeard("llama a Laura")
        a.onHeard("sí")
        assertTrue(d.calls.isEmpty(), "no second call while the line is busy")
        assertTrue(d.said.last().contains("En cuanto cuelgues, llamo a Laura"))
        a.onCallStateChanged(active = false)
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `with a stranger on the line, Peri offers to hang up first and calls once the line is free`() {
        val d = Line()
        val a = agent(d, Lang.EN)
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onHeard("call Laura")
        a.onHeard("yes")
        assertTrue(d.said.last().contains("Hang up first, and I'll call Laura"))
        assertEquals(listOf(true), d.pauses)
        a.onPauseAnswer(PauseAnswer.HANG_UP)
        assertEquals(1, d.hungUp)
        a.onCallStateChanged(active = false)
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `a guardian's request hangs up a stranger's call, never a call with the family`() {
        val d = Line()
        val a = agent(d)
        a.onIncomingCall("600111222")
        a.onCallStateChanged(active = true, answered = true)
        a.onFamilyHangUp("Ana")
        assertEquals(0, d.hungUp)
        a.onCallStateChanged(active = false)
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onFamilyHangUp("Ana")
        assertEquals(1, d.hungUp)
        assertTrue(d.said.last().contains("Ana me ha pedido que cuelgue esta llamada"))
    }

    @Test
    fun `when the phone won't let Peri hang up, she asks the person to and shows the button`() {
        val d = Line(canHangUp = false)
        val a = agent(d, Lang.EN)
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onFamilyHangUp("Ana")
        assertTrue(d.said.last().contains("Ana asks you to hang up now"))
        assertEquals(listOf(true), d.pauses)
        a.onPauseAnswer(PauseAnswer.HANG_UP)
        assertTrue(d.said.last().contains("Press the red button"))
    }
}
