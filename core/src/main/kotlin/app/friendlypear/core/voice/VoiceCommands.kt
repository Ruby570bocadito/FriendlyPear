package app.friendlypear.core.voice

import app.friendlypear.core.shield.Text

/**
 * Voice orders in Spanish and English as older people say them: with fillers ("oye Peri, por favor",
 * "hey Peri, please"), without accents from the transcriber, and with short answers to Peri's questions.
 * Both languages are always understood: families mix them.
 */
object VoiceCommands {
    private val fillers = Text.anyOf(
        listOf(
            "oye", "peri", "por favor", "porfa", "anda", "venga ya", "hazme el favor de", "puedes", "podrias",
            "hey", "please", "can you", "could you", "would you", "will you",
        ),
    )

    private val callVerbs = Regex(
        "\\b(llamame|llamale|llamala|llama|llamar|llames|telefonea|marca|ponme con|pasame con|quiero hablar con|habla con|hablar con|" +
            "call|ring|phone|dial|put me through to|i want to talk to|i want to speak to|talk to|speak to)\\b(.*)",
    )
    private val send = Regex(
        "\\b(?:dile|di|manda|mandale|envia|enviale|escribe|escribele)\\s+(?:un\\s+(?:mensaje|whatsapp|sms)\\s+)?(?:a|al)\\s+(.+?)" +
            "\\s+(?:que|diciendo que|diciendole que|poniendo que|poniendole que)\\s+(.+)$" +
            "|\\b(?:tell|text|message|write to|send a (?:text|message) to)\\s+(.+?)\\s+(?:that|saying)\\s+(.+)$",
    )
    /** "Pide ayuda a mamá", "ask my daughter for help": a call to that person, after Peri's usual "shall I?". */
    private val askForHelp = Regex(
        "\\b(?:pide|pidele|pedir|pideles|busca|buscame)\\s+ayuda\\s+(?:a|al|de)\\s+(.+)$" +
            "|\\b(?:avisa|avisale|avisar)\\s+(?:a|al)\\s+(.+)$" +
            "|\\b(?:ask|get)\\s+(.+?)\\s+(?:for help|to help me)$" +
            "|\\b(?:get help from|alert)\\s+(.+)$",
    )
    /** What follows the person in a call for help: "llama a mamá, que necesito ayuda" calls mamá. */
    private val helpTail = Regex(
        "\\s+(?:que|para que|porque|for|because|i)\\s+(?:me ayude|me ayuden|necesito ayuda|me encuentro mal|help|need help|help me|i need help).*$" +
            "|\\s+(?:for help|to help me|please help)$",
    )
    private val trailingPlease = Regex("(?i)[,\\s]*(por favor|please)[.!\\s]*$")
    private val emergency = Text.anyOf(
        listOf(
            "me he caido", "me caido", "me he caio", "socorro", "auxilio", "emergencia", "me encuentro mal", "no me puedo levantar",
            "i ve fallen", "i have fallen", "i fell", "i can t get up", "i cannot get up", "emergency", "i don t feel well",
        ),
    )
    /** Too common inside other orders ("help me call Laura") to count unless said almost alone. */
    private val shortCry = setOf("help", "help me", "help help", "ayuda", "ayudame")
    private val readVerbs = Regex("\\b(leeme|lee|leer|lees|que dice|que dicen|que me han|tengo|read|any|what|do i have)\\b")
    private val messageWords = Text.anyOf(
        listOf("mensaje", "mensajes", "whatsapp", "whatsapps", "sms", "correo", "correos", "message", "messages", "texts", "emails", "email"),
    )
    private val lastWords = Text.anyOf(listOf("ultimo", "ultima", "el nuevo", "last", "latest", "newest"))
    private val cancel = Text.anyOf(
        listOf(
            "cancela", "cancelar", "cancelalo", "dejalo", "para", "parar", "olvidalo", "estoy bien",
            "cancel", "stop", "forget it", "i m fine", "i m ok", "i am fine", "i am ok", "never mind",
        ),
    )
    private val repeat = Text.anyOf(
        listOf("repite", "repitelo", "otra vez", "como", "no te he entendido", "que has dicho", "repeat", "say that again", "again", "what", "pardon", "sorry", "come again"),
    )
    private val yes = setOf(
        "si", "vale", "venga", "claro", "de acuerdo", "si si", "si claro", "adelante", "hazlo", "llama", "ok", "okey", "eso", "exacto",
        "yes", "yeah", "yep", "sure", "go ahead", "do it", "right", "okay", "call", "call her", "call him", "of course", "alright", "all right", "yes yes",
    )
    private val no = setOf(
        "no", "no no", "no gracias", "mejor no", "no llames", "nada", "ahora no",
        "nope", "no thanks", "no thank you", "not now", "don t", "dont", "don t call", "better not", "nothing",
    )

    fun parse(utterance: String): Command {
        val folded = Text.fold(utterance)
        val clean = fillers.replace(folded, " ").replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return Command.Unknown(utterance)

        if (emergency.containsMatchIn(clean) || clean in shortCry) return Command.Emergency
        if (clean in yes) return Command.Yes
        if (clean in no) return Command.No
        if (cancel.containsMatchIn(clean) && clean.split(" ").size <= 3) return Command.Cancel
        // Already given away: before calls ("he dado el código a Pablo" isn't a call to Pablo).
        app.friendlypear.core.agent.Breaches.of(clean)?.let { return Command.Gave(it) }

        sendMessage(utterance, folded)?.let { return it }
        askForHelp.find(clean)?.let { m ->
            val target = helpTail.replace(m.groupValues.drop(1).first { it.isNotEmpty() }, "").trim()
            if (target.isNotEmpty()) return Command.Call(target)
        }
        callVerbs.find(clean)?.let { m ->
            val target = helpTail.replace(m.groupValues[2], "").trim()
            if (target.isNotEmpty()) return Command.Call(target)
        }
        if (messageWords.containsMatchIn(clean) && (readVerbs.containsMatchIn(clean) || clean.split(" ").size <= 3)) {
            return Command.ReadMessages(onlyLast = lastWords.containsMatchIn(clean))
        }
        if (repeat.containsMatchIn(clean) && clean.split(" ").size <= 4) return Command.Repeat
        return Command.Unknown(utterance)
    }

    /** The body is cut from the original words: folding keeps the length, so the match ranges line up. */
    private fun sendMessage(utterance: String, folded: String): Command.SendMessage? {
        val m = send.find(folded) ?: return null
        val (targetGroup, bodyGroup) = if (m.groups[1] != null) 1 to 2 else 3 to 4
        val bodyRange = m.groups[bodyGroup]!!.range
        val body = if (folded.length == utterance.length) utterance.substring(bodyRange.first, bodyRange.last + 1) else m.groupValues[bodyGroup]
        val text = trailingPlease.replace(body, "").trim().trimEnd('.', '!')
        if (text.isEmpty()) return null
        return Command.SendMessage(m.groupValues[targetGroup].trim(), text)
    }
}
