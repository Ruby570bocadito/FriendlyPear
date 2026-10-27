package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class PauseDevice : DevicePort {
    val said = mutableListOf<String>()
    val calls = mutableListOf<String>()
    val pauses = mutableListOf<Pair<String, String?>>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) {
        calls += phone
    }
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
    override fun showPause(text: String, callName: String?) {
        pauses += text to callName
    }
}

class SafetyPauseTest {
    private val ana = Contact("1", "Ana", listOf("600111222"), Relation.CHILD, trust = TrustLevel.GUARDIAN)
    private val bbva = "com.bbva.bbvacontigo"
    private var now = 1_000_000L
    private val scam = "Banco Santander: tu cuenta está bloqueada. Responde con el código que te hemos enviado"

    private fun agent(lang: Lang = Lang.ES): Pair<PauseDevice, PeriAgent> {
        val d = PauseDevice()
        return d to PeriAgent(d, listOf(ana), lang, clock = { now })
    }

    private fun PeriAgent.afterScam() = onMessage(IncomingMessage(Channel.SMS, "+34 699 000 111", scam, now))

    @Test
    fun `a money app soon after a scam gets a pause that offers to call the family`() {
        val (d, a) = agent()
        a.afterScam()
        assertTrue(a.watchesApps)
        a.onAppOpened(bbva)
        val (text, name) = d.pauses.single()
        assertEquals("Ana", name)
        assertTrue("Espera un momentito" in text && "¿Llamamos a Ana para comprobarlo?" in text)
        a.onPauseAnswer(PauseAnswer.CALL_FAMILY)
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `a spoken yes on the pause calls too`() {
        val (d, a) = agent()
        a.afterScam()
        a.onAppOpened(bbva)
        a.onHeard("sí")
        assertEquals(listOf("600111222"), d.calls)
    }

    @Test
    fun `continue or leave never calls, and leaving is praised`() {
        val (d, a) = agent(Lang.EN)
        a.afterScam()
        a.onAppOpened(bbva)
        a.onPauseAnswer(PauseAnswer.LEAVE)
        assertTrue(d.said.last().startsWith("Well done for stopping"))
        a.onPauseAnswer(PauseAnswer.CALL_FAMILY)
        assertTrue(d.calls.isEmpty())
        assertFalse(a.isWaitingForAnswer)
    }

    @Test
    fun `once per risk, and not after the window`() {
        val (d, a) = agent()
        a.afterScam()
        a.onAppOpened(bbva)
        a.onPauseAnswer(PauseAnswer.CONTINUE)
        a.onAppOpened(bbva)
        assertEquals(1, d.pauses.size)
        a.afterScam()
        now += 31 * 60_000L
        assertFalse(a.watchesApps)
        a.onAppOpened(bbva)
        assertEquals(1, d.pauses.size)
    }

    @Test
    fun `a remote control app after a stranger's call is paused by name`() {
        val (d, a) = agent(Lang.EN)
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onCallStateChanged(active = false)
        a.onAppOpened("com.anydesk.anydeskandroid")
        assertTrue("a number that isn't in your contacts called you" in d.pauses.single().first)
        assertTrue("AnyDesk" in d.pauses.single().first)
    }

    @Test
    fun `during the stranger's call the pause says hang up and offers no call`() {
        val (d, a) = agent()
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onAppOpened(bbva)
        assertNull(d.pauses.single().second)
        assertTrue("Cuelga" in d.pauses.single().first)
    }

    @Test
    fun `nothing worrying, no pause`() {
        val (d, a) = agent()
        assertFalse(a.watchesApps)
        a.onAppOpened(bbva)
        a.onAppOpened("com.whatsapp")
        assertTrue(d.pauses.isEmpty() && d.said.isEmpty())
    }
}
