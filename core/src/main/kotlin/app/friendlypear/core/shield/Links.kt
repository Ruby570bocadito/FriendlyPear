package app.friendlypear.core.shield

internal object Links {
    private val withScheme = Regex("""(?:https?://|www\.)([\p{L}\p{N}.-]+)""")
    private val bare = Regex("""(?<![@\p{L}\p{N}._-])((?:[\p{L}\p{N}-]+\.)+([a-z]{2,24}))(?![\p{L}\p{N}-])""")
    private val emailDomain = Regex("""@([\p{L}\p{N}-]+(?:\.[\p{L}\p{N}-]+)+)""")

    /** Without a scheme or "www", a word with a dot only counts as a link on these TLDs ("p.ej." is not a link). */
    private val bareTlds = setOf(
        "com", "es", "net", "org", "info", "eu", "cat", "gal", "eus", "io", "co", "me", "ly", "gl", "gd", "at", "cc", "id",
        "app", "site", "online", "link", "click", "shop", "store", "top", "xyz", "icu", "vip", "live", "biz", "sbs",
        "cfd", "bond", "buzz", "fun", "life", "work", "support", "help", "ru", "cn", "tk", "ml", "ga", "cf", "gq", "de", "fr",
        // ".co.uk" and a few more: the red team's "brightpay-secure.co.uk/p/8821" was not seen as a link.
        "uk", "pt", "ie", "nl", "ch", "pl", "tv", "pro", "website", "club", "mobi", "cyou", "ws", "pw",
    )

    private val shorteners = setOf(
        "bit.ly", "bitly.com", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rebrand.ly", "shorturl.at",
        "t.ly", "tiny.cc", "rb.gy", "s.id", "buff.ly", "acortar.link", "shorturl.ly", "v.gd", "lnkd.in", "qr.link",
    )

    /** A host as people see it: an international name (xn--) decoded to its letters. */
    fun shown(host: String): String =
        if ("xn--" in host) runCatching { java.net.IDN.toUnicode(host, java.net.IDN.ALLOW_UNASSIGNED) }.getOrDefault(host) else host

    /** Hosts of every link in the text, lowercase and without "www.". */
    fun hosts(text: String): List<String> {
        val lower = text.lowercase()
        val found = withScheme.findAll(lower).map { it.groupValues[1] } +
            bare.findAll(lower).filter { it.groupValues[2] in bareTlds }.map { it.groupValues[1] }
        return found.map { it.trim('.', '-').removePrefix("www.") }.filter { '.' in it }.distinct().toList()
    }

    /** The domain of an email address in the sender, as in "Correos <info@correos-envio.com>". */
    fun senderDomain(sender: String): String? = emailDomain.find(sender.lowercase())?.groupValues?.get(1)

    fun isShortener(host: String): Boolean = host in shorteners

    private val withPort = Regex("""\b(https?)://([\p{L}\p{N}.-]+)(?::(\d{1,5}))?""")
    private val ipv4 = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

    /**
     * Links that don't travel over TLS on the usual port 443: "http://", an explicit other port
     * ("https://pago.example:8443"), or a bare IP address instead of a name. Real banks and shops never send these.
     */
    fun insecure(text: String): Boolean = withPort.findAll(text.lowercase()).any { m ->
        val (scheme, host, port) = m.destructured
        scheme == "http" || (port.isNotEmpty() && port != "443") || ipv4.matches(host)
    }
}
