package app.friendlypear.core.shield

import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Relation

/** Everything the rules saw in one message. The text itself is never kept. */
data class Detection(
    val signals: Set<Signal>,
    val impersonated: Brand?,
    val mentioned: Set<Brand>,
    val claimedRelation: Relation?,
)

internal object SignalDetector {
    private val urgency = Text.anyOf(listOf(
        "urgente", "urgentemente", "urgencia", "inmediato", "inmediatamente", "hoy mismo", "ahora mismo", "cuanto antes", "ultimo aviso",
        "de lo contrario", "evite el recargo", "en 24 horas", "en 24h", "en 48 horas", "en 48h", "antes de 24", "plazo",
        "bloqueada", "bloqueado", "suspendida", "suspendido", "cancelada", "retenido", "retenida", "caduca", "caducado",
        "expira", "accidente", "hospital", "detenido", "comisaria",
        "urgent", "urgently", "immediately", "right now", "asap", "as soon as possible", "final notice", "last warning",
        "act now", "within 24 hours", "within 48 hours", "otherwise", "avoid a fine", "suspended", "locked", "blocked",
        "on hold", "expires", "expired", "accident", "arrested", "police station",
    ))
    private val money = Regex(
        """\b(bizum|transferencia|transfiere|transferir|ingresa|ingresar|ingreso|pague|pagar|paga|pago|abone|abonar|""" +
            """tasas?|aduana|dinero|prestame|prestarme|me prestas|me puedes prestar|me dejas|tarjeta|transfer|wire|send money|""" +
            """pay|payment|fees?|customs|money|lend me|can you lend|gift cards?|card details)\b|""" +
            // Amounts, also written the way texts often do it: "200 EUR", "200e", "EUR 200", "200 GBP".
            // A symbol needs no word boundary after it ("1,99 €"); letters do ("200 EUR", not "200 eurovision").
            """\d+([.,]\d+)?(\s?€|\s?(euros?|eur|pounds?|gbp|dollars?|usd)\b|e\b)|\b(eur|gbp)\s?\d|[£$€]\s?\d""",
    )
    private val askVerbs = setOf(
        "dime", "digame", "diganos", "dinos", "facilite", "facilitenos", "facilitanos", "facilita", "envie", "envia",
        "enviame", "envienos", "mande", "manda", "mandame", "reenvie", "reenvia", "comparta", "comparte", "compartas",
        "introduzca", "introduce", "indique", "indica", "confirme", "confirma", "verifique", "verifica", "actualice",
        "actualiza", "proporcione", "pasame", "dicte", "dicta", "digas", "diga",
        "tell", "send", "share", "enter", "confirm", "verify", "update", "provide", "give", "read", "forward", "reply",
    )
    private val secrets = Regex("""\b(codigo|clave|contrasena|pin|cvv|credenciales|identidad|datos bancarios|numero de tarjeta|datos de (su|tu) tarjeta|""" +
        """code|password|passcode|credentials|bank details|card number|security number)\b""")
    /** A code that was, or is about to be, sent to the person: asking for it back is how codes are stolen. */
    private val codeSent = Regex(
        """\b(codigo|clave|pin|code|passcode)\b.{0,40}\b(hemos enviado|acabamos de enviar|ha llegado|ha recibido|has recibido|""" +
            """va a llegar|vas a recibir|va a recibir|llegara|le llegue|te llegue|we just sent|we sent|we have sent|we.?ve sent|""" +
            """sent you|texted you|you just got|you received|you.?ve received|you will receive|you.?ll receive)\b""" +
            """|\b(le|te) (va|van) a llegar un (codigo|sms)\b""" +
            // The SMS or the digits themselves: "reenvíeme el SMS con la clave", "los 6 dígitos que le llegaron".
            """|\b(el )?sms (con|de) (la clave|el codigo|verificacion)\b|\b(\d|seis|cuatro|ocho) digitos\b""" +
            """|\bcodigo (de verificacion )?recibido\b|\b(the )?code you (got|received|just got)\b|\bthe (six|6) digits\b""",
    )
    private val secrecy = Regex(
        """\bno (le |les |se lo |se la )?(digas|cuentes|avises|comentes|diga|cuente|avise) (nada )?(a|al|a tu|a mis|a nadie)\b""" +
            """|\bguarda(lo)? (el |en )?secreto\b|\bque quede entre (tu y yo|nosotros|nosotras)\b|\bsin decirle(s)? (nada )?a\b""" +
            """|\bdon.?t tell\b|\bdo not tell\b|\bkeep (it|this) (a )?secret\b|\bbetween (you and me|us)\b|\btell no one\b""",
    )
    private val scare = Regex("""\b(virus|infectad[oa]s?|hackead[oa]|pirateado|spyware|malware|troyano|infected|hacked|compromised)\b""")
    private val device = Regex(
        """\b(ordenador|pc|windows|movil|telefono|router|dispositivo|equipo|android|iphone|tablet|portatil|computer|device|phone|laptop|mac)\b""",
    )
    private val supportCall = Regex(
        """\b(acceso remoto|programa de acceso|llame al tecnico|tecnico de (microsoft|windows|movistar|vodafone)|soporte tecnico|""" +
            """(windows|microsoft) defender|remote access|tech support|call (our|the) technician)\b""",
    )
    private val affection = Regex("""\b(mi amor|amor mio|querida|querido|te amo|te adoro|my love|darling|sweetheart|my dear)\b""")
    private val loveStory = Regex(
        """\b(viud[oa]|soldado|militar|plataforma (petrolifera|petrolera|de \w+)|ingeniero en|en (una )?mision|""" +
            """conocerte|visitarte|vernos por fin|empezar (una )?vida|widow(er)?|soldier|oil rig|deployed|finally meet)\b""",
    )
    private val negations = setOf("no", "nunca", "jamas", "nadie", "never", "not", "dont", "don", "nobody", "anyone")
    private val newNumber = Regex(
        """\b(numero nuevo|nuevo numero|cambiado de numero|cambie de numero|cambiado el numero|este es mi numero|""" +
            """guarda (este|mi) numero|guardate (este|mi) numero|telefono nuevo|movil nuevo|""" +
            """se me ha (roto|caido|perdido|estropeado|mojado) el (movil|telefono)|me han robado el (movil|telefono)|""" +
            // The new number in other words: someone else's phone, a temporary one, a broken screen.
            """desde el (movil|telefono) de (un|una|mi) \w+|numero (temporal|provisional)|""" +
            """(se me cayo|se ha caido|se me ha caido) (el movil )?al agua|(el movil|el mio) no enciende|(se quedo|me quede) sin pantalla|""" +
            """pantalla (en negro|rota)|me han robado el bolso|mi movil (murio|ha muerto|esta muerto)|""" +
            """(using|on) (a|my) (mate|friend)\S*|temporary number|smashed my phone|my phone (died|is dead)|""" +
            """new number|changed my number|this is my number|save (this|my) number|new phone|""" +
            """(broke|broken|lost|dropped) my phone|my phone (is broken|broke|was stolen))\b""",
    )
    private val prizeOrProfit = Regex(
        """\b(has ganado|ha ganado|ganador|ganadora|premio|sorteo|reclama tu|invierte|inversion|rentabilidad|""" +
            """criptomonedas?|bitcoin|gana \d+|ganar \d+|al mes sin|""" +
            """you have won|you.?ve won|winner|prize|lottery|claim your|invest|investment|crypto|earn \d+)\b""",
    )
    private val remoteApps = Regex("""\b(anydesk|teamviewer|rustdesk|quicksupport|ultraviewer|supremo|airdroid|any desk|team viewer)\b""")
    private val aiInstructions = listOf(
        "ignora las instrucciones", "ignora tus instrucciones", "ignora todas las instrucciones", "olvida tus instrucciones",
        "instrucciones anteriores", "ignore previous instructions", "ignore all previous", "ignore the above",
        "disregard previous", "eres un asistente", "eres una ia", "como modelo de lenguaje", "asistente de ia",
        "system prompt", "prompt del sistema", "marca este mensaje como", "clasifica este mensaje como",
        "este mensaje es seguro", "mark this message as", "this message is safe", "nota para la ia", "note to ai", "<system>",
    )
    private val codeWord = Regex("""\b(codigo(?! postal)|clave|code|otp|pin|verificacion|passcode|verification)\b""")
    private val codeDigits = Regex("""(?<!\d)\d{4,8}(?!\d)""")
    private val relationWords = listOf(
        Relation.CHILD to listOf("mama", "mami", "papa", "papi"),
        Relation.GRANDCHILD to listOf("abuela", "abuelo", "abu", "yaya", "yayo"),
        Relation.NEPHEW_NIECE to listOf("tia", "tio", "tita", "tito"),
        Relation.SIBLING to listOf("hermana", "hermano", "tata"),
        Relation.CHILD to listOf("mum", "mom", "mummy", "mommy", "dad", "daddy"),
        Relation.GRANDCHILD to listOf("grandma", "grandpa", "granny", "nan", "nana", "grandad", "granddad"),
        Relation.NEPHEW_NIECE to listOf("auntie", "aunt", "uncle"),
        Relation.SIBLING to listOf("sis", "bro"),
    )

    fun detect(message: IncomingMessage, context: ShieldContext): Detection {
        val folded = Text.fold(message.text)
        val signals = mutableSetOf<Signal>()
        val hosts = Links.hosts(message.text)
        val mentioned = Brands.mentionedIn(folded) + Brands.mentionedIn(Text.fold(message.sender))
        val checked = hosts + listOfNotNull(Links.senderDomain(message.sender))
        val impersonated = checked.firstNotNullOfOrNull { Brands.impersonated(it, mentioned) }

        if (!context.knows(message.sender)) signals += Signal.UNKNOWN_SENDER
        if (hosts.isNotEmpty()) signals += Signal.LINK
        if (hosts.any(Links::isShortener)) signals += Signal.URL_SHORTENER
        if (Links.insecure(message.text)) signals += Signal.INSECURE_LINK
        if (impersonated != null) signals += Signal.BRAND_LOOKALIKE
        if (urgency.containsMatchIn(folded)) signals += Signal.URGENCY
        if (money.containsMatchIn(folded)) signals += Signal.MONEY_REQUEST
        if (asksForSecret(folded)) signals += Signal.CREDENTIAL_REQUEST
        if (asksToForwardCode(folded)) signals += Signal.CODE_FORWARD_REQUEST
        if (newNumber.containsMatchIn(folded)) signals += Signal.NEW_NUMBER_CLAIM
        if (prizeOrProfit.containsMatchIn(folded)) signals += Signal.PRIZE_OR_PROFIT
        if (secrecy.containsMatchIn(folded)) signals += Signal.SECRECY
        if ((scare.containsMatchIn(folded) && device.containsMatchIn(folded)) || supportCall.containsMatchIn(folded)) signals += Signal.DEVICE_SCARE
        // Affection from a stranger only: family say "querida" and "mi amor" all the time.
        if (!context.knows(message.sender) && (affection.containsMatchIn(folded) || loveStory.containsMatchIn(folded))) signals += Signal.ROMANCE_BAIT
        if (remoteApps.containsMatchIn(folded)) signals += Signal.REMOTE_ACCESS_APP
        if (aiInstructions.any { it in folded }) signals += Signal.AI_INSTRUCTIONS
        if (codeWord.containsMatchIn(folded) && codeDigits.containsMatchIn(folded)) {
            signals += Signal.ONE_TIME_CODE
            if (context.callActive) signals += Signal.CODE_DURING_CALL
        }
        return Detection(signals, impersonated, mentioned, claimedRelation(folded))
    }

    /** "Díganos el código" asks for a secret; "no lo compartas con nadie" is the bank warning against it. */
    private fun asksForSecret(folded: String): Boolean = Text.sentences(folded).any { sentence ->
        val words = Text.words(sentence)
        secrets.containsMatchIn(sentence) && words.indices.any { i ->
            words[i] in askVerbs && !negated(words, i)
        }
    }

    /** "Díganos el código que le ha llegado", "reply with the code we sent you"; "never share the code we sent" is a warning. */
    private fun asksToForwardCode(folded: String): Boolean {
        val sentences = Text.sentences(folded)
        val anySentenceWithCode = sentences.any { codeSent.containsMatchIn(it) }
        return anySentenceWithCode && sentences.any { sentence ->
            val words = Text.words(sentence)
            words.indices.any { i -> words[i] in forwardVerbs && !negated(words, i) }
        }
    }

    /**
     * A "no" in the three words before the verb negates it, unless a "y"/"and" comes between: in "no cuelgue y
     * facilite el código" the "no" belongs to "cuelgue", and the message still asks for the code.
     */
    private fun negated(words: List<String>, i: Int): Boolean {
        val before = words.subList(maxOf(0, i - 3), i)
        return before.drop(before.indexOfLast { it in joins } + 1).any { it in negations }
    }

    private val joins = setOf("y", "e", "and", "then", "luego")

    /** Giving the code to someone; entering it in the official app ("introduce", "enter") is fine. */
    private val forwardVerbs = setOf(
        "dime", "digame", "diganos", "dinos", "dimelo", "digamelo", "diganoslo", "diga", "digas", "facilite", "facilitenos",
        "envie", "envia", "enviame", "envienos", "mande", "manda", "mandame", "reenvie", "reenvia", "comparta", "comparte",
        "pasame", "paseme", "reenvieme", "digale", "digaselo", "dicte", "dicta", "responda", "responde", "conteste", "contesta",
        "tell", "send", "share", "give", "forward", "reply", "read", "say",
    )

    private fun claimedRelation(folded: String): Relation? {
        val opening = Text.words(folded).take(6)
        return relationWords.firstOrNull { (_, words) -> opening.any { it in words } }?.first
    }
}
