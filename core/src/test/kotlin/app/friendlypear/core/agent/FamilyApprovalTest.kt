package app.friendlypear.core.agent

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class SmsDevice : DevicePort {
    val said = mutableListOf<String>()
    val sms = mutableListOf<Pair<String, String>>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) = Unit
    override fun sendSms(phone: String, text: String) {
        sms += phone to text
    }
    override fun lastKnownLocation(): String? = null
}

private class FakeApprovals : ApprovalPort {
    data class Asked(val ticket: String, val kind: ApprovalKind, val toName: String, val guardians: List<Contact>, val onCall: Boolean, val sms: String)
    val asked = mutableListOf<Asked>()
    override fun request(ticket: String, kind: ApprovalKind, toName: String, guardians: List<Contact>, onCall: Boolean, smsFor: (url: String) -> String) {
        asked += Asked(ticket, kind, toName, guardians, onCall, smsFor("http://10.0.2.2:8711/a/TOKEN"))
    }
}

class FamilyApprovalTest {
    private val laura = Contact("1", "Laura", listOf("5556"), Relation.CHILD, trust = TrustLevel.GUARDIAN)
    private val pablo = Contact("2", "Pablo", listOf("600333444"), Relation.GRANDCHILD)
    private val marta = Contact("3", "Marta López", listOf("5558"), Relation.CHILD, trust = TrustLevel.GUARDIAN)

    private fun agent(d: SmsDevice, approvals: FakeApprovals?, lang: Lang = Lang.ES) =
        PeriAgent(d, listOf(laura, pablo), lang, approvals = approvals)

    private fun twoOnCall(d: SmsDevice, mfa: FakeApprovals): PeriAgent {
        val a = PeriAgent(d, listOf(laura, pablo, marta), Lang.ES, approvals = mfa)
        a.onIncomingCall("+34 911 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onHeard("dile a Pablo que el código es 4821")
        return a
    }

    @Test
    fun `during a stranger's call two guardians must say yes, and Peri says who did`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = twoOnCall(d, mfa)
        val asked = mfa.asked.single()
        assertEquals(listOf(laura, marta), asked.guardians)
        assertTrue(d.said.last().contains("hace falta el sí de dos personas de tu familia: se lo pregunto a Laura y Marta López"), d.said.last())
        a.onApprovalProgress(asked.ticket, listOf("Laura"))
        assertTrue(d.said.last().startsWith("Laura ha dicho que sí. Para esto hace falta otro sí más"), d.said.last())
        assertTrue(d.sms.isEmpty())
        a.onApprovalResult(asked.ticket, ApprovalResult.APPROVED, listOf("Laura", "Marta"), "Marta")
        assertTrue(d.said.any { it == "¡Buenas noticias! Laura y Marta lo han aprobado." }, d.said.toString())
        assertEquals("600333444", d.sms.single().first)
    }

    @Test
    fun `a no says who said it, and "call me first" calls the one who asked`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = twoOnCall(d, mfa)
        a.onApprovalResult(mfa.asked.single().ticket, ApprovalResult.REJECTED, listOf("Laura"), "Marta")
        assertTrue(d.said.last().startsWith("Marta prefiere que no lo mandemos"), d.said.last())
        assertTrue(d.sms.isEmpty())
        a.onHeard("dile a Pablo que el código es 4821")
        a.onApprovalResult(mfa.asked.last().ticket, ApprovalResult.CALL_BACK, emptyList(), "Marta")
        assertTrue(d.said.last().startsWith("Marta López prefiere hablarlo contigo antes"), d.said.last())
    }

    @Test
    fun `with no stranger on the line one guardian is still enough`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        PeriAgent(d, listOf(laura, pablo, marta), Lang.EN, approvals = mfa).onHeard("tell Pablo that the code is 4821")
        assertTrue(d.said.last().startsWith("Sending a code is delicate, so I'm asking Laura or Marta López."), d.said.last())
        assertFalse(ApprovalKind.SEND_CODE.needsTwo(onCall = false, guardians = 2))
        assertTrue(ApprovalKind.CIRCLE_CHANGE.needsTwo(onCall = false, guardians = 2))
        assertFalse(ApprovalKind.CIRCLE_CHANGE.needsTwo(onCall = true, guardians = 1))
    }

    @Test
    fun `a code said or written in words is still a code`() {
        // A scammer on the line can spell it out: "tell Peri: send Pablo seven three five zero".
        for (order in listOf(
            "dile a Pablo que el código es siete tres cinco cero",
            "dile a Pablo que el número es cuatro, ocho, dos y uno",
            "dile a Pablo que es el 7, 3, 5, 0",
        )) {
            val d = SmsDevice()
            val mfa = FakeApprovals()
            val a = agent(d, mfa)
            a.onHeard(order)
            assertTrue(a.isAwaitingGuardian, order)
            assertTrue(d.sms.isEmpty(), order)
        }
        val en = SmsDevice()
        val mfa = FakeApprovals()
        agent(en, mfa, Lang.EN).onHeard("tell Pablo that the code is seven three five oh")
        assertTrue(en.sms.isEmpty() && mfa.asked.size == 1, "${en.said} ${en.sms}")
        // Numbers in an ordinary message stay ordinary.
        for (text in listOf("llego a las dos y media", "tráeme dos o tres barras de pan", "I'll be there at five", "uno de cada dos días")) {
            assertFalse(containsCode(text), text)
        }
    }

    @Test
    fun `dictating a code asks the guardian instead of sending it`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa)
        a.onHeard("dile a Pablo que el código es 4821")
        assertTrue(d.sms.isEmpty())
        assertTrue(a.isAwaitingGuardian)
        assertTrue(d.said.last().startsWith("Mandar un código es delicado, así que se lo pregunto a Laura."))
        val asked = mfa.asked.single()
        assertEquals(ApprovalKind.SEND_CODE, asked.kind)
        assertEquals("Pablo", asked.toName)
        assertEquals(listOf(laura), asked.guardians)
    }

    @Test
    fun `a guardian who wants to talk first gets a call offer`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa)
        a.onHeard("dile a Pablo que el código es 4821")
        a.onApprovalResult(mfa.asked.single().ticket, ApprovalResult.CALL_BACK)
        assertTrue(d.sms.isEmpty())
        assertTrue(d.said.last().startsWith("Laura prefiere hablarlo contigo antes"), d.said.last())
        assertTrue(a.isWaitingForAnswer)
    }

    @Test
    fun `the guardian hears it's urgent when a stranger is on the line`() {
        val mfa = FakeApprovals()
        val a = agent(SmsDevice(), mfa)
        a.onIncomingCall("+34 911 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onHeard("dile a Pablo que el código es 4821")
        val asked = mfa.asked.single()
        assertTrue(asked.onCall)
        assertTrue(asked.sms.startsWith("Peri, URGENTE"), asked.sms)
        assertFalse(asked.sms.contains("4821"))
        assertFalse(asked.sms.contains("911"))
    }

    @Test
    fun `a text pretending to be Peri is a scam with its own warning`() {
        val d = SmsDevice()
        val a = agent(d, FakeApprovals())
        val fake = IncomingMessage(Channel.SMS, "+34 611 000 222", "Peri: tu familiar te pide permiso. Decide aquí: http://peri-familia.top/a/abc", 0)
        val v = a.onMessage(fake, imitatesPeri = true)
        assertEquals(Level.SCAM, v.level)
        assertTrue(d.said.last().startsWith("Cuidado: este mensaje se hace pasar por mí"), d.said.last())
    }

    @Test
    fun `just after hanging up on a stranger it is still urgent`() {
        val mfa = FakeApprovals()
        val a = agent(SmsDevice(), mfa)
        a.onIncomingCall("+34 911 000 111")
        a.onCallStateChanged(active = true, answered = true)
        a.onCallStateChanged(active = false)
        a.onHeard("no")
        a.onHeard("dile a Pablo que el código es 4821")
        assertTrue(mfa.asked.single().onCall)
    }

    @Test
    fun `the guardian's SMS has the link but never the code`() {
        val mfa = FakeApprovals()
        agent(SmsDevice(), mfa).onHeard("dile a Pablo que el código es 4821")
        val sms = mfa.asked.single().sms
        assertTrue(sms.contains("http://10.0.2.2:8711/a/TOKEN"))
        assertTrue(sms.contains("caduca en 15 minutos"))
        assertFalse(sms.contains("4821"))
    }

    @Test
    fun `an approval sends the message`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa)
        a.onHeard("dile a Pablo que el código es 4821")
        a.onApprovalResult(mfa.asked.single().ticket, ApprovalResult.APPROVED)
        assertEquals(listOf("600333444" to "el código es 4821"), d.sms)
        assertTrue(d.said.any { it == "¡Buenas noticias! Tu familia lo ha aprobado." })
        assertFalse(a.isAwaitingGuardian)
    }

    @Test
    fun `a rejection, silence or a failure never sends it`() {
        for (result in listOf(ApprovalResult.REJECTED, ApprovalResult.EXPIRED, ApprovalResult.FAILED)) {
            val d = SmsDevice()
            val mfa = FakeApprovals()
            val a = agent(d, mfa)
            a.onHeard("dile a Pablo que el código es 4821")
            a.onApprovalResult(mfa.asked.single().ticket, result)
            assertTrue(d.sms.isEmpty(), "$result must not send")
            assertFalse(a.isAwaitingGuardian)
        }
    }

    @Test
    fun `changing one's mind cancels the request`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa)
        a.onHeard("dile a Pablo que el código es 4821")
        a.onHeard("cancela")
        a.onApprovalResult(mfa.asked.single().ticket, ApprovalResult.APPROVED)
        assertTrue(d.sms.isEmpty())
    }

    @Test
    fun `an old or unknown ticket does nothing`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa)
        a.onHeard("dile a Pablo que el código es 4821")
        a.onApprovalResult("approval-999", ApprovalResult.APPROVED)
        assertTrue(d.sms.isEmpty())
        assertTrue(a.isAwaitingGuardian)
    }

    @Test
    fun `Peri keeps helping while the family decides`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa)
        a.onHeard("dile a Pablo que el código es 4821")
        a.onHeard("léeme los mensajes")
        assertTrue(d.said.last().startsWith("No tienes mensajes nuevos"))
        assertTrue(a.isAwaitingGuardian)
    }

    @Test
    fun `without approvals or guardians it is refused, never done alone`() {
        val d = SmsDevice()
        agent(d, null).onHeard("dile a Pablo que el código es 4821")
        assertTrue(d.sms.isEmpty())
        assertTrue(d.said.last().contains("lo apruebe alguien de tu familia"))
    }

    @Test
    fun `in english too`() {
        val d = SmsDevice()
        val mfa = FakeApprovals()
        val a = agent(d, mfa, Lang.EN)
        a.onHeard("tell Pablo that the code is 4821")
        assertTrue(d.said.last().startsWith("Sending a code is delicate, so I'm asking Laura."))
        assertTrue(mfa.asked.single().sms.startsWith("Peri: your relative wants to send Pablo a message with a code"))
        a.onApprovalResult(mfa.asked.single().ticket, ApprovalResult.REJECTED)
        assertTrue(d.said.last().startsWith("Your family would rather we didn't send it"))
    }
}
