package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class WatchDevice : DevicePort {
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

class RemoteAppTest {
    private val laura = Contact("1", "Laura", listOf("600111222"), Relation.CHILD)

    @Test
    fun `installing AnyDesk gets a warning and an offer to tell the family`() {
        val d = WatchDevice()
        val a = PeriAgent(d, listOf(laura))
        a.onAppInstalled("com.anydesk.anydeskandroid")
        val said = d.said.single()
        assertTrue(said.startsWith("Se acaba de instalar AnyDesk, un programa para que otra persona maneje tu móvil desde lejos."))
        assertTrue(said.contains("Nadie de tu banco ni de ninguna empresa te lo pedirá nunca."))
        assertTrue(said.endsWith("¿Se lo cuento a Laura?"))
        assertTrue(d.sms.isEmpty())
    }

    @Test
    fun `the family is told only after a yes, and never the person's messages`() {
        val d = WatchDevice()
        val a = PeriAgent(d, listOf(laura))
        a.onAppInstalled("com.teamviewer.quicksupport.market")
        a.onHeard("sí")
        val (phone, text) = d.sms.single()
        assertEquals("600111222", phone)
        assertTrue(text.contains("TeamViewer QuickSupport"))
    }

    @Test
    fun `a no means the family is not told`() {
        val d = WatchDevice()
        val a = PeriAgent(d, listOf(laura))
        a.onAppInstalled("com.anydesk.anydeskandroid")
        a.onHeard("no")
        assertTrue(d.sms.isEmpty())
    }

    @Test
    fun `during a call Peri first says to hang up`() {
        val d = WatchDevice()
        val a = PeriAgent(d, listOf(laura))
        a.onCallStateChanged(active = true, answered = true)
        a.onAppInstalled("com.rustdesk.rustdesk")
        assertTrue(d.said.last().startsWith("Cuelga ahora mismo, por favor. Se acaba de instalar RustDesk"))
    }

    @Test
    fun `ordinary apps are ignored`() {
        val d = WatchDevice()
        PeriAgent(d, listOf(laura)).onAppInstalled("com.whatsapp")
        assertTrue(d.said.isEmpty())
    }

    @Test
    fun `without family Peri still warns`() {
        val d = WatchDevice()
        PeriAgent(d, emptyList(), Lang.EN).onAppInstalled("com.anydesk.anydeskandroid")
        assertTrue(d.said.single().startsWith("AnyDesk has just been installed"))
        assertTrue(d.said.single().endsWith("we'll remove it together."))
    }
}
