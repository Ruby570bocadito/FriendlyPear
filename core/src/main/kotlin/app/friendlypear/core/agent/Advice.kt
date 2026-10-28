package app.friendlypear.core.agent

import app.friendlypear.core.shield.Text

/** A situation or question about scams and staying safe that Peri answers with reviewed advice ([Phrases.advice]). */
enum class Topic {
    QR, WHATSAPP, SIM_SWAP, STOLEN_PHONE, REMOTE_APP, BANK_CALL, CODE, FAMILY_MONEY, BIZUM, PARCEL, PRIZE, ROMANCE,
    INVESTMENT, FAKE_AUTHORITY, SHOPPING, LINK, PASSWORDS, UNKNOWN_CALL, WHAT_IS_A_SCAM,
}

/**
 * "Me ha llamado el banco, ¿qué hago?", "is this link safe?": the topic of a question, understood on the phone (the
 * words never leave it), so Peri answers with a sentence written and reviewed in advance, never a model's words.
 * [of] needs a question or a situation ("qué hago", "me piden", "I got"), so an order like "call the bank" stays an
 * order; [topicOnly] is the last resort for a few words that are nothing else ("phishing", "bizum").
 */
object Advice {
    private val asking = Regex(
        "\\b(que hago|que debo|que puedo|que tengo que|como|es seguro|es segura|es normal|es verdad|es un timo|es fiable|es de fiar|" +
            "que es|por que|puedo|deberia|debo|me ha|me han|me llama|me llaman|me pide|me piden|me dice|me dicen|me escribe|me escriben|" +
            "me ofrece|me ofrecen|he recibido|he ganado|tengo un|tengo una|alguien|ayudame con|consejo|consejos|que pasa si|y si|" +
            "what should|what do i|what can i|is it safe|is this|is that|how do i|how can i|how to|what is|what s|what are|" +
            "should i|can i|someone|somebody|i got|i ve got|i received|i ve received|i won|they asked|asks me|asked me|what if|" +
            "called me|calling me|texted me|sent me|i have a|help with|advice|tips)\\b",
    )

    private val topics: List<Pair<Topic, Regex>> = listOf(
        Topic.QR to Text.anyOf(listOf("qr", "codigo qr", "qr code")),
        Topic.WHATSAPP to Text.anyOf(listOf("proteger whatsapp", "proteger mi whatsapp", "protejo whatsapp", "protejo mi whatsapp", "whatsapp seguro",
            "verificacion en dos pasos", "two step", "two step verification", "protect whatsapp", "protect my whatsapp", "secure whatsapp", "secure my whatsapp")),
        Topic.SIM_SWAP to Text.anyOf(listOf("sin linea", "sin cobertura", "quitado el numero", "quitado mi numero", "duplicado de la sim", "duplicado de sim",
            "sim", "lost my line", "loses its line", "lose my line", "lost its line", "no signal", "no service", "sim swap", "took my number", "taken my number")),
        Topic.STOLEN_PHONE to Text.anyOf(listOf("robado el movil", "robado mi movil", "perdido el movil", "perdido mi movil", "robado el telefono",
            "perdido el telefono", "stolen phone", "phone was stolen", "phone stolen", "stole my phone", "lost my phone", "lost phone")),
        Topic.REMOTE_APP to Text.anyOf(listOf("anydesk", "teamviewer", "quicksupport", "rustdesk", "instalar una app", "instale una app", "instalar una aplicacion",
            "descargar una app", "descargue una app", "soporte tecnico", "microsoft", "control remoto", "install an app", "install app", "tech support",
            "remote access", "remote control")),
        Topic.BANK_CALL to Text.anyOf(listOf("banco", "cuenta segura", "bank", "safe account")),
        Topic.CODE to Text.anyOf(listOf("codigo", "codigos", "code", "codes", "verification code")),
        Topic.FAMILY_MONEY to Text.anyOf(listOf("numero nuevo", "mi hijo", "mi hija", "mi nieto", "mi nieta", "mi sobrino", "mi sobrina", "un familiar",
            "familiar", "mama soy yo", "papa soy yo", "new number", "my son", "my daughter", "my grandson", "my granddaughter", "family member",
            "a relative", "mum it s me", "mom it s me", "dad it s me")),
        Topic.BIZUM to Text.anyOf(listOf("bizum")),
        Topic.PARCEL to Text.anyOf(listOf("paquete", "correos", "envio", "aduana", "aduanas", "mensajeria", "repartidor", "parcel", "package", "delivery", "customs", "courier")),
        Topic.PRIZE to Text.anyOf(listOf("premio", "sorteo", "he ganado", "loteria", "regalo", "prize", "lottery", "giveaway", "i won", "you won", "free gift")),
        Topic.ROMANCE to Text.anyOf(listOf("conocido por internet", "conoci por internet", "conocida por internet", "novio por internet", "novia por internet",
            "pareja por internet", "app de citas", "met online", "online boyfriend", "online girlfriend", "dating", "romance")),
        Topic.INVESTMENT to Text.anyOf(listOf("invertir", "inversion", "inversiones", "criptomoneda", "criptomonedas", "cripto", "bitcoin", "ganar dinero",
            "invest", "investment", "crypto", "trading", "make money")),
        Topic.FAKE_AUTHORITY to Text.anyOf(listOf("policia", "guardia civil", "hacienda", "multa", "juzgado", "seguridad social", "police", "tax office",
            "taxes", "a fine", "court", "social security")),
        Topic.SHOPPING to Text.anyOf(listOf("comprar", "compra", "compras", "tienda", "chollo", "oferta", "wallapop", "segunda mano", "buy", "buying",
            "shop", "shopping", "bargain", "second hand", "marketplace")),
        Topic.LINK to Text.anyOf(listOf("enlace", "enlaces", "link", "links", "pagina web", "web", "url", "website")),
        Topic.PASSWORDS to Text.anyOf(listOf("contrasena", "contrasenas", "clave", "claves", "password", "passwords")),
        Topic.UNKNOWN_CALL to Text.anyOf(listOf("numero desconocido", "numero que no conozco", "desconocido", "llamada rara", "unknown number",
            "number i don t know", "strange call", "spam call")),
        Topic.WHAT_IS_A_SCAM to Text.anyOf(listOf("phishing", "smishing", "vishing", "timo", "timos", "estafa", "estafas", "fraude", "scam", "scams",
            "fraud", "seguridad", "safe online", "security")),
    )

    /** The topic of a question or a situation; null if it isn't one, or none fits. */
    fun of(clean: String): Topic? = if (asking.containsMatchIn(clean)) topicOnly(clean) else null

    /** The topic of a few words alone, when they are nothing else Peri understands. */
    fun topicOnly(clean: String): Topic? = topics.firstOrNull { (_, words) -> words.containsMatchIn(clean) }?.first
}
