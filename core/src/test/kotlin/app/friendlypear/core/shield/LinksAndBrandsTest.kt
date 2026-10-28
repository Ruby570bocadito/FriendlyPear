package app.friendlypear.core.shield

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinksAndBrandsTest {
    private fun brand(name: String) = Brands.ALL.first { it.name == name }

    @Test
    fun `finds links with and without scheme`() {
        val text = "Pague aquí: https://www.correos-envio.top/pago o en bit.ly/3xYz. También www.dgt.es."
        assertEquals(listOf("correos-envio.top", "dgt.es", "bit.ly"), Links.hosts(text))
    }

    @Test
    fun `finds British links without a scheme, and fake marketplace pages`() {
        assertEquals(listOf("brightpay-secure.co.uk"), Links.hosts("just pop your card in here brightpay-secure.co.uk/p/8821"))
        assertEquals(brand("Vinted"), Brands.impersonated("vintd-payhelp.co.uk", setOf(brand("Vinted"))))
        assertEquals(brand("Gumtree"), Brands.impersonated("gumtree-safebox.co.uk", emptySet()))
        assertNull(Brands.impersonated("vinted.co.uk", setOf(brand("Vinted"))))
        // Seen as links now, the brands' own British sites must not look like impersonations.
        assertNull(Brands.impersonated("dhl.co.uk", setOf(brand("DHL"))))
        assertNull(Brands.impersonated("vodafone.co.uk", setOf(brand("Vodafone"))))
        assertNull(Brands.impersonated("www.nhs.scot", setOf(brand("the NHS"))))
        // A Cyrillic а, raw or as an international name, is still PayPal's name on someone else's site.
        assertEquals(brand("PayPal"), Brands.impersonated("pаypal.com", emptySet()))
        assertEquals(brand("PayPal"), Brands.impersonated("xn--pypal-4ve.com", emptySet()))
        assertEquals("españa.es", Links.shown("xn--espaa-rta.es"))
    }

    @Test
    fun `abbreviations, amounts and emails are not links`() {
        assertEquals(emptyList(), Links.hosts("Son 1,99 €, p.ej. mañana. Escribe a laura@gmail.com"))
    }

    @Test
    fun `reads the domain of an email sender`() {
        assertEquals("correos-envio.com", Links.senderDomain("Correos <info@correos-envio.com>"))
        assertNull(Links.senderDomain("+34 612 34 56 78"))
    }

    @Test
    fun `knows the usual shorteners`() {
        assertTrue(Links.isShortener("bit.ly"))
        assertTrue(!Links.isShortener("correos.es"))
    }

    @Test
    fun `official domains and their subdomains are not impersonations`() {
        assertNull(Brands.impersonated("correos.es", setOf(brand("Correos"))))
        assertNull(Brands.impersonated("sede.agenciatributaria.gob.es", setOf(brand("la Agencia Tributaria"))))
        assertNull(Brands.impersonated("amazon.de", setOf(brand("Amazon"))))
    }

    @Test
    fun `brand names inside foreign domains are impersonations even without a mention`() {
        assertEquals("BBVA", Brands.impersonated("bbva-seguridad.com", emptySet())?.name)
        assertEquals("Correos", Brands.impersonated("correos.es.pago-tasas.info", emptySet())?.name)
        assertEquals("la DGT", Brands.impersonated("dgt-multas.net", emptySet())?.name)
    }

    @Test
    fun `disguised letters do not hide the brand`() {
        assertEquals("Correos", Brands.impersonated("c0rreos.es", emptySet())?.name)
        assertEquals("PayPal", Brands.impersonated("paypa1.com", emptySet())?.name)
        assertEquals("Correos", Brands.impersonated("corréos.es", emptySet())?.name)
    }

    @Test
    fun `near misses only count when the message names the brand`() {
        assertNull(Brands.impersonated("correo.ugr.es", emptySet()))
        assertEquals("Correos", Brands.impersonated("correo-envios.com", setOf(brand("Correos")))?.name)
        assertEquals("Correos", Brands.impersonated("correosenvios.com", setOf(brand("Correos")))?.name)
    }

    @Test
    fun `unrelated domains are left alone`() {
        assertNull(Brands.impersonated("booking.com", Brands.ALL.toSet()))
        assertNull(Brands.impersonated("elpais.com", emptySet()))
    }

    @Test
    fun `finds brands named in the text as whole words`() {
        val folded = Text.fold("Correos: su paquete está retenido. Hacienda le devuelve 300 €")
        assertEquals(setOf("Correos", "la Agencia Tributaria"), Brands.mentionedIn(folded).map { it.name }.toSet())
        assertTrue(Brands.mentionedIn(Text.fold("Estoy haciendo la cena")).isEmpty())
    }

    @Test
    fun `phone numbers compare without prefix or spaces`() {
        assertEquals("612345678", Text.phoneDigits("+34 612 34 56 78"))
        assertEquals("612345678", Text.phoneDigits("0034612345678"))
        assertEquals("612345678", Text.phoneDigits("612 345 678"))
    }
}
