package app.friendlypear.core.shield

/** A brand scammers imitate: the words that name it and the domains that really belong to it. */
data class Brand(
    val name: String,
    val tokens: List<String>,
    val mentions: List<String>,
    val domains: List<String>,
)

internal object Brands {
    val ALL = listOf(
        Brand("Correos", listOf("correos"), listOf("correos"), listOf("correos.es", "correos.com")),
        Brand("SEUR", listOf("seur"), listOf("seur"), listOf("seur.com", "seur.es")),
        Brand("DHL", listOf("dhl"), listOf("dhl"), listOf("dhl.com", "dhl.es", "dhl.de")),
        Brand("la DGT", listOf("dgt"), listOf("dgt", "trafico"), listOf("dgt.es", "dgt.gob.es")),
        Brand(
            "la Agencia Tributaria", listOf("agenciatributaria", "aeat", "hacienda"),
            listOf("agencia tributaria", "hacienda", "aeat"), listOf("agenciatributaria.gob.es", "agenciatributaria.es"),
        ),
        Brand(
            "la Seguridad Social", listOf("segsocial", "seguridadsocial"),
            listOf("seguridad social"), listOf("seg-social.es", "seg-social.gob.es"),
        ),
        Brand("BBVA", listOf("bbva"), listOf("bbva"), listOf("bbva.es", "bbva.com")),
        Brand("Santander", listOf("santander", "bancosantander"), listOf("santander"), listOf("bancosantander.es", "santander.com", "santander.es")),
        Brand("CaixaBank", listOf("caixabank", "caixa", "lacaixa"), listOf("caixabank", "la caixa", "caixa"), listOf("caixabank.es", "caixabank.com", "lacaixa.es")),
        Brand("Sabadell", listOf("sabadell", "bancsabadell"), listOf("sabadell"), listOf("bancsabadell.com", "sabadell.com")),
        Brand("ING", listOf("ing"), listOf("ing"), listOf("ing.es", "ing.com")),
        Brand("Bizum", listOf("bizum"), listOf("bizum"), listOf("bizum.es")),
        Brand("Amazon", listOf("amazon"), listOf("amazon"), listOf("amazon.es", "amazon.com", "amazon.de", "amazon.fr", "amazon.it", "amazon.co.uk", "amzn.to", "amzn.eu")),
        Brand("Netflix", listOf("netflix"), listOf("netflix"), listOf("netflix.com")),
        Brand("PayPal", listOf("paypal"), listOf("paypal"), listOf("paypal.com", "paypal.es", "paypal.me")),
        Brand("Apple", listOf("apple", "icloud"), listOf("apple", "icloud"), listOf("apple.com", "icloud.com", "apple.co")),
        Brand("Microsoft", listOf("microsoft", "outlook", "office365"), listOf("microsoft", "outlook"), listOf("microsoft.com", "outlook.com", "live.com", "office.com")),
        Brand("WhatsApp", listOf("whatsapp"), listOf("whatsapp"), listOf("whatsapp.com", "whatsapp.net", "wa.me")),
        Brand("Iberdrola", listOf("iberdrola"), listOf("iberdrola"), listOf("iberdrola.es", "iberdrola.com")),
        Brand("Endesa", listOf("endesa"), listOf("endesa"), listOf("endesa.com", "endesa.es")),
        Brand("Naturgy", listOf("naturgy"), listOf("naturgy"), listOf("naturgy.es", "naturgy.com")),
        Brand("Movistar", listOf("movistar"), listOf("movistar"), listOf("movistar.es", "movistar.com", "telefonica.es")),
        Brand("Vodafone", listOf("vodafone"), listOf("vodafone"), listOf("vodafone.es", "vodafone.com")),
        Brand("Orange", listOf("orange"), listOf("orange"), listOf("orange.es", "orange.com")),
    )

    private val lookalikeChars = listOf("0" to "o", "1" to "l", "3" to "e", "4" to "a", "5" to "s", "7" to "t", "rn" to "m", "vv" to "w")

    fun mentionedIn(folded: String): Set<Brand> = ALL.filter { b -> b.mentions.any { Text.hasWord(folded, it) } }.toSet()

    fun isOfficial(host: String, brand: Brand): Boolean = brand.domains.any { host == it || host.endsWith(".$it") }

    /**
     * The brand a host pretends to be. An exact or disguised name ("c0rreos", "bbva-seguridad") always counts;
     * a near miss ("correo", "correosenvios") only when the message also names the brand, to keep false alarms low.
     */
    fun impersonated(host: String, mentioned: Set<Brand>): Brand? = ALL.firstOrNull { brand ->
        !isOfficial(host, brand) && parts(host).any { part ->
            val s = skeleton(part)
            brand.tokens.any { t ->
                s == t || (brand in mentioned && t.length >= 5 && (t in s || Text.editDistance(s, t) <= 1))
            }
        }
    }

    private fun parts(host: String): List<String> =
        host.split('.').flatMap { label -> label.split('-') + label.replace("-", "") }.filter { it.isNotEmpty() }.distinct()

    private fun skeleton(part: String): String = lookalikeChars.fold(Text.fold(part)) { s, (from, to) -> s.replace(from, to) }
}
