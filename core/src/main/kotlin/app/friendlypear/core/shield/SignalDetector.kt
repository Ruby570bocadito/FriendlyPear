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
            """tasas?|aduana|dinero|prestame|prestarme|me dejas|tarjeta|transfer|wire|send money|pay|payment|fees?|""" +
            """customs|money|lend me|gift cards?|card details)\b|\d+([.,]\d+)?\s?(€|euros?|pounds?|dollars?)|[£$]\s?\d""",
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
            """|\b(le|te) (va|van) a llegar un (codigo|sms)\b""",
    )
    private val negations = setOf("no", "nunca", "jamas", "nadie", "never", "not", "dont", "don", "nobody", "anyone")
    private val newNumber = Regex(
        """\b(numero nuevo|nuevo numero|cambiado de numero|cambie de numero|cambiado el numero|este es mi numero|""" +
            """guarda (este|mi) numero|guardate (este|mi) numero|telefono nuevo|movil nuevo|""" +
            """se me ha (roto|caido|perdido|estropeado|mojado) el (movil|telefono)|me han robado el (movil|telefono)|""" +
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
            words[i] in askVerbs && words.subList(maxOf(0, i - 3), i).none { it in negations }
        }
    }

    /** "Díganos el código que le ha llegado", "reply with the code we sent you"; "never share the code we sent" is a warning. */
    private fun asksToForwardCode(folded: String): Boolean {
        val sentences = Text.sentences(folded)
        val anySentenceWithCode = sentences.any { codeSent.containsMatchIn(it) }
        return anySentenceWithCode && sentences.any { sentence ->
            val words = Text.words(sentence)
            words.indices.any { i -> words[i] in forwardVerbs && words.subList(maxOf(0, i - 3), i).none { it in negations } }
        }
    }

    /** Giving the code to someone; entering it in the official app ("introduce", "enter") is fine. */
    private val forwardVerbs = setOf(
        "dime", "digame", "diganos", "dinos", "dimelo", "digamelo", "diganoslo", "diga", "digas", "facilite", "facilitenos",
        "envie", "envia", "enviame", "envienos", "mande", "manda", "mandame", "reenvie", "reenvia", "comparta", "comparte",
        "pasame", "dicte", "dicta", "responda", "responde", "conteste", "contesta",
        "tell", "send", "share", "give", "forward", "reply", "read", "say",
    )

    private fun claimedRelation(folded: String): Relation? {
        val opening = Text.words(folded).take(6)
        return relationWords.firstOrNull { (_, words) -> opening.any { it in words } }?.first
    }
}
