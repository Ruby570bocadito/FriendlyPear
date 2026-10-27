package app.friendlypear.core.trust

import java.net.URI

/**
 * Peri to Peri. A guardian who also has FriendlyPear gets the family request by SMS like anyone else; their own Peri
 * recognises it and shows it in the app instead of treating it as a suspicious link. Only when the link points at
 * this app's own server: the details are then fetched from there, never read from the SMS, and the sender must
 * be a saved contact (checked by the caller). A text that talks like Peri but links elsewhere is an impersonation.
 */
object FamilyRequestLink {
    data class Found(val url: String, val token: String)

    private val link = Regex("""https?://[^\s/]+/a/([A-Za-z0-9_-]{20,64})(?=$|[\s.,;:!?)\]])""")
    private val anyLink = Regex("""(https?://|www\.)[^\s]+|\b[a-z0-9-]+\.(com|net|org|info|top|xyz|es|app|link|site|online|io)\b""", RegexOption.IGNORE_CASE)
    private val periWords = Regex("""\b(peri|friendlypear|friendly pear)\b""", RegexOption.IGNORE_CASE)
    /** The map link Peri's own emergency alert carries: Google Maps, a point, nothing else. */
    private val mapsLink = Regex("""^https://maps\.google\.com/\?q=-?\d+(\.\d+)?,-?\d+(\.\d+)?$""")
    private val alert = Regex("""\b(aviso de peri|alert from peri)\b.*\b(se ha caido|have fallen)\b""")

    /** The request link in [text], if it points at [server] (same scheme, host and port). */
    fun find(text: String, server: String): Found? {
        val own = origin(server) ?: return null
        return link.findAll(text).firstNotNullOfOrNull { m ->
            Found(m.value, m.groupValues[1]).takeIf { origin(m.value) == own }
        }
    }

    /**
     * It says it's from Peri and carries a link that isn't Peri's: not to Peri's own server (a request, the
     * guardians' news page), nor the map in Peri's own emergency alert.
     */
    fun imitatesPeri(text: String, server: String): Boolean {
        if (!periWords.containsMatchIn(text) || find(text, server) != null) return false
        val own = origin(server)
        return anyLink.findAll(text).any { m -> !isPeris(m.value.trimEnd('.', ',', ';', ':', '!', '?', ')'), own) }
    }

    /** Peri's own emergency alert ("Alert from Peri: your relative said they have fallen…"), in either language. */
    fun periAlert(text: String): Boolean = alert.containsMatchIn(app.friendlypear.core.shield.Text.fold(text))

    private fun isPeris(link: String, own: String?): Boolean = mapsLink.matches(link) || (own != null && origin(link) == own)

    private fun origin(url: String): String? = runCatching {
        val u = URI(url)
        val scheme = u.scheme?.lowercase() ?: return null
        val host = u.host?.lowercase() ?: return null
        val port = if (u.port == -1) (if (scheme == "https") 443 else 80) else u.port
        "$scheme://$host:$port"
    }.getOrNull()
}
