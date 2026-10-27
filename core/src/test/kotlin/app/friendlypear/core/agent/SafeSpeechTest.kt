package app.friendlypear.core.agent

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeSpeechTest {
    private val es = SafeSpeech(Spanish)
    private val en = SafeSpeech(English)

    @Test
    fun `links and domains become 'un enlace'`() {
        assertEquals("Entra en un enlace ya", es.clean("Entra en https://banco-seguro.top/login ya"))
        assertEquals("Mira un enlace", es.clean("Mira www.restaurante.es"))
        assertEquals("Pague en un enlace", es.clean("Pague en correos-envio.info"))
    }

    @Test
    fun `codes and phone numbers are not read`() {
        assertEquals("Tu código es unos números", es.clean("Tu código es 482913"))
        assertEquals("Llama al unos números", es.clean("Llama al 600 111 222"))
    }

    @Test
    fun `money, times and short numbers are kept`() {
        assertEquals("Pague 1,99 € hoy", es.clean("Pague 1,99 € hoy"))
        assertEquals("Gana 3.000 euros", es.clean("Gana 3.000 euros"))
        assertEquals("A las 10:30 en el portal 12", es.clean("A las 10:30 en el portal 12"))
    }

    @Test
    fun `emails are not spelled out`() {
        assertEquals("Escribe a una dirección de correo", es.clean("Escribe a soporte@banco.com"))
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

    @Test
    fun `english speech hides links and codes too, and keeps pounds and dollars`() {
        assertEquals("Go to a link now", en.clean("Go to www.bank-secure.co.uk now"))
        assertEquals("Your code is some numbers", en.clean("Your code is 482913"))
        assertEquals("Pay £1.99 or $2500 today", en.clean("Pay £1.99 or $2500 today"))
        assertEquals("Write to an email address", en.clean("Write to help@bank.com"))
    }
}
