package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class Mouth : DevicePort {
    val said = mutableListOf<String>()
    override fun speak(text: String) {
        said += text
    }
    override fun call(phone: String) = Unit
    override fun sendSms(phone: String, text: String) = Unit
    override fun lastKnownLocation(): String? = null
}

class BankDuringCallTest {
    private val laura = Contact("1", "Laura", listOf("600111222"), Relation.CHILD)
    private val bbva = "com.bbva.bbvacontigo"

    private fun onCallWith(number: String, lang: Lang = Lang.ES): Pair<Mouth, PeriAgent> {
        val d = Mouth()
        val a = PeriAgent(d, listOf(laura), lang)
        a.onIncomingCall(number)
        a.onCallStateChanged(active = true, answered = true)
        d.said.clear()
        return d to a
    }

    @Test
    fun `opening the bank app while talking to a stranger says hang up and call the bank`() {
        val (d, a) = onCallWith("+34 699 000 111")
        a.onAppOpened(bbva)
        assertTrue(d.said.single().contains("Cuelga, por favor, y llama tú a tu banco al número de tu tarjeta."))
    }

    @Test
    fun `only once per call`() {
        val (d, a) = onCallWith("+34 699 000 111")
        a.onAppOpened(bbva)
        a.onAppOpened("com.revolut.revolut")
        assertEquals(1, d.said.size)
    }

    @Test
    fun `a call with family is left alone`() {
        val (d, a) = onCallWith("600111222")
        a.onAppOpened(bbva)
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `other apps during the call are left alone`() {
        val (d, a) = onCallWith("+34 699 000 111")
        a.onAppOpened("com.whatsapp")
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `while it is still ringing nothing is said`() {
        val d = Mouth()
        val a = PeriAgent(d, listOf(laura))
        a.onIncomingCall("+34 699 000 111")
        a.onCallStateChanged(active = true, answered = false)
        d.said.clear()
        a.onAppOpened(bbva)
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `in english too`() {
        val (d, a) = onCallWith("+44 7700 900 555", Lang.EN)
        a.onAppOpened("com.barclays.android.barclaysmobilebanking")
        assertTrue(d.said.single().startsWith("Hold on: is someone telling you what to do in your bank app"))
    }
}
