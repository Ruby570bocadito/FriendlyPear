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
        Brand("DHL", listOf("dhl"), listOf("dhl"), listOf("dhl.com", "dhl.es", "dhl.de", "dhl.co.uk")),
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
        Brand("Santander", listOf("santander", "bancosantander"), listOf("santander"), listOf("bancosantander.es", "santander.com", "santander.es", "santander.co.uk")),
        Brand("CaixaBank", listOf("caixabank", "caixa", "lacaixa"), listOf("caixabank", "la caixa", "caixa"), listOf("caixabank.es", "caixabank.com", "lacaixa.es")),
        Brand("Sabadell", listOf("sabadell", "bancsabadell"), listOf("sabadell"), listOf("bancsabadell.com", "sabadell.com")),
        Brand("ING", listOf("ing"), listOf("ing"), listOf("ing.es", "ing.com")),
        Brand("Bizum", listOf("bizum"), listOf("bizum"), listOf("bizum.es")),
        Brand("Amazon", listOf("amazon"), listOf("amazon"), listOf("amazon.es", "amazon.com", "amazon.de", "amazon.fr", "amazon.it", "amazon.co.uk", "amzn.to", "amzn.eu")),
        Brand("Netflix", listOf("netflix"), listOf("netflix"), listOf("netflix.com")),
        Brand("PayPal", listOf("paypal"), listOf("paypal"), listOf("paypal.com", "paypal.es", "paypal.me", "paypal.co.uk")),
        Brand("Apple", listOf("apple", "icloud"), listOf("apple", "icloud"), listOf("apple.com", "icloud.com", "apple.co")),
        Brand("Microsoft", listOf("microsoft", "outlook", "office365"), listOf("microsoft", "outlook"), listOf("microsoft.com", "outlook.com", "live.com", "office.com")),
        Brand("WhatsApp", listOf("whatsapp"), listOf("whatsapp"), listOf("whatsapp.com", "whatsapp.net", "wa.me")),
        Brand("Iberdrola", listOf("iberdrola"), listOf("iberdrola"), listOf("iberdrola.es", "iberdrola.com")),
        Brand("Endesa", listOf("endesa"), listOf("endesa"), listOf("endesa.com", "endesa.es")),
        Brand("Naturgy", listOf("naturgy"), listOf("naturgy"), listOf("naturgy.es", "naturgy.com")),
        Brand("Movistar", listOf("movistar"), listOf("movistar"), listOf("movistar.es", "movistar.com", "telefonica.es")),
        Brand("Vodafone", listOf("vodafone"), listOf("vodafone"), listOf("vodafone.es", "vodafone.com", "vodafone.co.uk")),
        Brand("Orange", listOf("orange"), listOf("orange"), listOf("orange.es", "orange.com")),
        // Second-hand marketplaces: their fake "secure payment" pages (found by the red team).
        Brand("Wallapop", listOf("wallapop"), listOf("wallapop"), listOf("wallapop.com", "wallapop.es")),
        Brand("Vinted", listOf("vinted"), listOf("vinted"), listOf("vinted.es", "vinted.com", "vinted.co.uk", "vinted.fr", "vinted.de", "vinted.it", "vinted.pt", "vinted.ie", "vinted.nl", "vinted.be", "vinted.pl")),
        Brand("Milanuncios", listOf("milanuncios"), listOf("milanuncios"), listOf("milanuncios.com")),
        Brand("eBay", listOf("ebay"), listOf("ebay"), listOf("ebay.es", "ebay.com", "ebay.co.uk", "ebay.de", "ebay.fr", "ebay.it", "ebay.ie", "ebay.us", "ebay.com.au", "ebay.ca", "ebay.nl", "ebay.at", "ebay.ch")),
        Brand("Gumtree", listOf("gumtree"), listOf("gumtree"), listOf("gumtree.com", "gumtree.ie", "gumtree.com.au")),
        // United Kingdom and United States, for families there and for the English demo.
        Brand("Royal Mail", listOf("royalmail"), listOf("royal mail"), listOf("royalmail.com", "royalmail.co.uk")),
        Brand("Evri", listOf("evri", "hermes"), listOf("evri"), listOf("evri.com")),
        Brand("UPS", listOf("ups"), listOf("ups"), listOf("ups.com")),
        Brand("FedEx", listOf("fedex"), listOf("fedex"), listOf("fedex.com")),
        Brand("USPS", listOf("usps"), listOf("usps"), listOf("usps.com")),
        Brand("HMRC", listOf("hmrc"), listOf("hmrc"), listOf("gov.uk")),
        Brand("the DVLA", listOf("dvla"), listOf("dvla"), listOf("gov.uk")),
        Brand("the NHS", listOf("nhs"), listOf("nhs"), listOf("nhs.uk", "nhs.net", "nhs.scot", "nhs.wales")),
        Brand("the IRS", listOf("irs"), listOf("irs"), listOf("irs.gov")),
        Brand("Barclays", listOf("barclays"), listOf("barclays"), listOf("barclays.co.uk", "barclays.com")),
        Brand("HSBC", listOf("hsbc"), listOf("hsbc"), listOf("hsbc.co.uk", "hsbc.com")),
        Brand("Lloyds", listOf("lloyds", "lloydsbank"), listOf("lloyds"), listOf("lloydsbank.com", "lloydsbank.co.uk", "lloyds.com")),
        Brand("NatWest", listOf("natwest"), listOf("natwest"), listOf("natwest.com")),
        Brand("Bank of America", listOf("bankofamerica", "bofa"), listOf("bank of america"), listOf("bankofamerica.com", "bofa.com")),
        Brand("Wells Fargo", listOf("wellsfargo"), listOf("wells fargo"), listOf("wellsfargo.com")),
        Brand("Tesco", listOf("tesco"), listOf("tesco"), listOf("tesco.com")),
        Brand("Walmart", listOf("walmart"), listOf("walmart"), listOf("walmart.com")),
    )

    private val lookalikeChars = listOf("0" to "o", "1" to "l", "3" to "e", "4" to "a", "5" to "s", "7" to "t", "rn" to "m", "vv" to "w")

    private val mentionPatterns = ALL.associateWith { Text.anyOf(it.mentions) }

    fun mentionedIn(folded: String): Set<Brand> = ALL.filter { mentionPatterns.getValue(it).containsMatchIn(folded) }.toSet()

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
        Links.shown(host).split('.').flatMap { label -> label.split('-') + label.replace("-", "") }.filter { it.isNotEmpty() }.distinct()

    private fun skeleton(part: String): String = lookalikeChars.fold(Text.fold(part)) { s, (from, to) -> s.replace(from, to) }
}
