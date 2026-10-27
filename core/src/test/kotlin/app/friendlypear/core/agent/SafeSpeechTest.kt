package app.friendlypear.core.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeSpeechTest {
    @Test
    fun `links and domains become 'un enlace'`() {
        assertEquals("Entra en un enlace ya", SafeSpeech.clean("Entra en https://banco-seguro.top/login ya"))
        assertEquals("Mira un enlace", SafeSpeech.clean("Mira www.restaurante.es"))
        assertEquals("Pague en un enlace", SafeSpeech.clean("Pague en correos-envio.info"))
    }

    @Test
    fun `codes and phone numbers are not read`() {
        assertEquals("Tu código es unos números", SafeSpeech.clean("Tu código es 482913"))
        assertEquals("Llama al unos números", SafeSpeech.clean("Llama al 600 111 222"))
    }

    @Test
    fun `money, times and short numbers are kept`() {
        assertEquals("Pague 1,99 € hoy", SafeSpeech.clean("Pague 1,99 € hoy"))
        assertEquals("Gana 3.000 euros", SafeSpeech.clean("Gana 3.000 euros"))
        assertEquals("A las 10:30 en el portal 12", SafeSpeech.clean("A las 10:30 en el portal 12"))
    }

    @Test
    fun `emails are not spelled out`() {
        assertEquals("Escribe a una dirección de correo", SafeSpeech.clean("Escribe a soporte@banco.com"))
    }

    @Test
    fun `phones are grouped for dialling by hand`() {
        assertEquals("600 111 222", SafeSpeech.phone("+34 600111222"))
    }

    @Test
    fun `codes are told apart from phone numbers`() {
        assertTrue(containsCode("el código es 4821"))
        assertTrue(containsCode("es el 48 21 93"))
        assertFalse(containsCode("llámame al 600 111 222"))
        assertFalse(containsCode("llego a las 10"))
    }
}
