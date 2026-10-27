package app.friendlypear.core.trust

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FamilyRequestLinkTest {
    private val server = "http://10.0.2.2:8711"
    private val token = "_XG3jmG_UOT4p1-OdbfhyGKOMxMvnH4PTkRJmPuZRn8"
    private val real = "Peri, URGENTE: tu familiar quiere mandar a Marisa un código justo después de hablar por teléfono con un número desconocido. Decide aquí (caduca en 15 minutos): http://10.0.2.2:8711/a/$token"

    @Test
    fun `a request to our own server is recognised`() {
        val found = FamilyRequestLink.find(real, server)!!
        assertEquals(token, found.token)
        assertEquals("http://10.0.2.2:8711/a/$token", found.url)
        assertEquals(token, FamilyRequestLink.find("Decide here: https://peri.example.org/a/$token.", "https://peri.example.org")?.token)
    }

    @Test
    fun `the same path on another server is not ours`() {
        assertNull(FamilyRequestLink.find(real.replace("10.0.2.2:8711", "10.0.2.2:8712"), server))
        assertNull(FamilyRequestLink.find(real.replace("http://10.0.2.2:8711", "http://peri-familia.top"), server))
        assertNull(FamilyRequestLink.find(real.replace("http://", "https://"), server))
        assertNull(FamilyRequestLink.find("http://10.0.2.2:8711.evil.top/a/$token", server))
    }

    @Test
    fun `a text that talks like Peri but links elsewhere is an impersonation`() {
        assertTrue(FamilyRequestLink.imitatesPeri(real.replace("http://10.0.2.2:8711", "http://peri-familia.top"), server))
        assertTrue(FamilyRequestLink.imitatesPeri("FriendlyPear: tu cuenta caduca hoy, renueva en friendlypear-app.info", server))
        assertFalse(FamilyRequestLink.imitatesPeri(real, server))
        assertFalse(FamilyRequestLink.imitatesPeri("Le he contado a Peri lo del domingo, qué maja es", server))
        assertFalse(FamilyRequestLink.imitatesPeri("Correos: su paquete está retenido en correos-envio.top", server))
    }
}
