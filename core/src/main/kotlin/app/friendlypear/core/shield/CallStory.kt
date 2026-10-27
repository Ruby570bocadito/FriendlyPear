package app.friendlypear.core.shield

import app.friendlypear.core.model.Level

/**
 * What a person tells Peri about a call from a stranger ("the bank, they wanted the code they texted me"),
 * compared with the scripts of phone scams: who the caller claimed to be, and what they asked for. Rules only,
 * on the phone: the person's words about the call never leave it.
 *
 * Asking for a code, a transfer to a "safe account", an app, the card or the cash is a scam whoever claimed to
 * call; a bank or the police on their own is only a doubt (real ones do call), so Peri asks what they wanted.
 */
object CallStory {
    enum class Claim { BANK, POLICE, TAX, TECH, COMPANY, FAMILY }

    /** In order of harm: the first one found is the one Peri talks about. */
    enum class Ask { CODE, MOVE_MONEY, HAND_OVER, INSTALL_APP, CARD_DETAILS, PERSONAL_DATA }

    data class Reading(val claims: Set<Claim>, val asks: Set<Ask>, val secrecy: Boolean, val urgency: Boolean) {
        val level: Level
            get() = when {
                asks.isNotEmpty() -> Level.SCAM
                claims.isNotEmpty() || secrecy -> Level.DOUBT
                else -> Level.OK
            }

        /** The worst thing they asked for. */
        val mainAsk: Ask? get() = Ask.entries.firstOrNull { it in asks }
    }

    fun read(text: String): Reading {
        val t = Text.fold(text)
        return Reading(
            claims = CLAIMS.filterValues { it.containsMatchIn(t) }.keys,
            asks = ASKS.filterValues { it.containsMatchIn(t) }.keys,
            secrecy = SECRECY.containsMatchIn(t),
            urgency = URGENCY.containsMatchIn(t),
        )
    }

    private val CLAIMS = mapOf(
        Claim.BANK to Text.anyOf(listOf("banco", "bank", "caixa", "bbva", "santander", "sabadell", "ing", "unicaja", "barclays", "hsbc", "lloyds", "natwest", "tarjeta", "card company", "fraud team", "departamento de fraude")),
        Claim.POLICE to Text.anyOf(listOf("policia", "police", "guardia civil", "detective", "juzgado", "court", "agente")),
        Claim.TAX to Text.anyOf(listOf("hacienda", "agencia tributaria", "seguridad social", "hmrc", "irs", "tax office", "impuestos")),
        Claim.TECH to Text.anyOf(listOf("microsoft", "soporte", "support", "tecnico", "technician", "virus", "ordenador", "computer", "router", "internet")),
        Claim.COMPANY to Text.anyOf(listOf("compania", "company", "empresa", "luz", "electricity", "telefonica", "movistar", "vodafone", "amazon", "correos", "seguro", "insurance")),
        Claim.FAMILY to Text.anyOf(listOf("mi hijo", "mi hija", "mi nieto", "mi nieta", "my son", "my daughter", "my grandson", "my granddaughter", "sobrino", "sobrina", "nephew", "niece")),
    )

    private val ASKS = mapOf(
        Ask.CODE to Text.anyOf(listOf("codigo", "code", "clave", "pin", "contrasena", "password", "numeros que me llegaron", "numbers they sent", "sms que me llego", "the text they sent", "verificacion", "verification")),
        Ask.MOVE_MONEY to Text.anyOf(listOf("transferencia", "transfer", "mover el dinero", "move my money", "move the money", "cuenta segura", "safe account", "bizum", "pasar el dinero", "send money", "mandar dinero", "ingresar", "pay", "pagar")),
        Ask.HAND_OVER to Text.anyOf(listOf("recoger la tarjeta", "collect the card", "collect my card", "mensajero", "courier will", "vendra alguien", "someone will come", "efectivo", "cash", "joyas", "jewellery", "jewelry")),
        Ask.INSTALL_APP to Text.anyOf(listOf("instalar", "install", "descargar", "download", "aplicacion", "app", "anydesk", "teamviewer", "quicksupport", "rustdesk", "acceso remoto", "remote access", "control del movil", "control my phone")),
        Ask.CARD_DETAILS to Text.anyOf(listOf("numero de la tarjeta", "card number", "datos de la tarjeta", "card details", "caducidad", "expiry", "cvv", "codigo de seguridad", "security code", "los tres numeros", "three digits")),
        Ask.PERSONAL_DATA to Text.anyOf(listOf("dni", "nie", "pasaporte", "passport", "fecha de nacimiento", "date of birth", "direccion", "address", "numero de cuenta", "account number", "iban", "datos", "details")),
    )

    private val SECRECY = Text.anyOf(listOf("no se lo dijera", "no se lo diga", "no lo cuente", "no se lo cuentes", "not to tell", "don't tell", "dont tell", "keep it secret", "secreto"))
    private val URGENCY = Text.anyOf(listOf("ahora mismo", "urgente", "rapido", "right now", "urgent", "quickly", "inmediatamente", "immediately", "hoy mismo", "today"))
}
