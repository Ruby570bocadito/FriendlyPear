package app.friendlypear.core.agent

import app.friendlypear.core.shield.Text

/**
 * Something already given away to a scammer, or leaked: what Peri helps with afterwards, step by step. [SOMETHING]
 * when the person says they gave something but not what.
 */
enum class Breach { CARD, BANK, MONEY, REMOTE, CODE, ID, PASSWORD, SOMETHING }

/**
 * "Le he dado el código", "he hecho un bizum", "I typed my bank password in the link": said after the fact, in either
 * language. A giving verb in the past is needed, so "what's my PIN?" or "read me the code" never count.
 */
object Breaches {
    private val gave = Regex(
        "\\b(he dado|le he dado|les he dado|di|le di|les di|he dicho|le he dicho|dije|le dije|les dije|he pasado|le he pasado|pase|le pase|" +
            "he mandado|mande|he enviado|envie|he puesto|puse|he metido|meti|he escrito|escribi|he instalado|instale|he descargado|descargue|" +
            "he hecho|hice|he pagado|pague|he transferido|transferi|me han robado|me han hackeado|me han quitado|me han pillado|se han filtrado|" +
            "han filtrado|filtraron|me han entrado|" +
            "i gave|i ve given|i told|i ve told|i sent|i ve sent|i typed|i entered|i put|i installed|i ve installed|i downloaded|i paid|i ve paid|" +
            "i transferred|i made a|i ve made a|they stole|they hacked|they took|they got into|was leaked|were leaked|got leaked|data breach)\\b",
    )
    private val kinds: List<Pair<Breach, Regex>> = listOf(
        Breach.MONEY to Text.anyOf(listOf("transferencia", "bizum", "dinero", "pagado", "pague", "pago", "euros", "transfer", "money", "paid", "payment")),
        Breach.REMOTE to Text.anyOf(listOf("anydesk", "teamviewer", "quicksupport", "rustdesk", "app que me dijeron", "aplicacion que me dijeron", "controlar el movil", "control remoto", "app they told me", "remote", "control my phone")),
        Breach.CARD to Text.anyOf(listOf("tarjeta", "pin", "cvv", "codigo de seguridad", "card", "cvc")),
        Breach.CODE to Text.anyOf(listOf("codigo", "whatsapp", "sms que me llego", "code", "verification")),
        Breach.ID to Text.anyOf(listOf("dni", "pasaporte", "nie", "foto de mi carnet", "carnet", "id card", "passport", "my id", "driving licence", "driver s license")),
        Breach.BANK to Text.anyOf(listOf("banco", "clave", "contrasena del banco", "usuario del banco", "bank", "online banking")),
        Breach.PASSWORD to Text.anyOf(listOf("contrasena", "cuenta", "datos", "correo", "password", "account", "email", "data", "leaked", "breach", "filtr")),
    )

    /** What was given away, if the person says they gave something; null when it isn't that. */
    fun of(clean: String): Breach? {
        if (!gave.containsMatchIn(clean)) return null
        return kinds.firstOrNull { (_, words) -> words.containsMatchIn(clean) }?.first ?: Breach.SOMETHING
    }
}
