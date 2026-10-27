package app.friendlypear.core.voice

import app.friendlypear.core.shield.Text

/**
 * Spanish voice orders as older people say them: with fillers ("oye Peri, por favor"),
 * without accents from the transcriber, and with short answers to Peri's questions.
 */
object VoiceCommands {
    private val fillers = Text.anyOf(listOf("oye", "peri", "por favor", "porfa", "anda", "venga ya", "hazme el favor de", "puedes", "podrias"))

    private val callVerbs = Regex(
        "\\b(llamame|llamale|llamala|llama|llamar|llames|telefonea|marca|ponme con|pasame con|quiero hablar con|habla con|hablar con)\\b(.*)",
    )
    private val send = Regex(
        "\\b(?:dile|di|manda|mandale|envia|enviale|escribe|escribele)\\s+(?:un\\s+(?:mensaje|whatsapp|sms)\\s+)?(?:a|al)\\s+(.+?)" +
            "\\s+(?:que|diciendo que|diciendole que|poniendo que|poniendole que)\\s+(.+)$",
    )
    private val trailingPlease = Regex("(?i)[,\\s]*por favor[.!\\s]*$")
    private val emergency = Text.anyOf(
        listOf("me he caido", "me caido", "me he caio", "socorro", "auxilio", "emergencia", "me encuentro mal", "no me puedo levantar"),
    )
    private val readVerbs = Regex("\\b(leeme|lee|leer|lees|que dice|que dicen|que me han|tengo)\\b")
    private val messageWords = Text.anyOf(listOf("mensaje", "mensajes", "whatsapp", "whatsapps", "sms", "correo", "correos"))
    private val lastWords = Text.anyOf(listOf("ultimo", "ultima", "el nuevo"))
    private val cancel = Text.anyOf(listOf("cancela", "cancelar", "cancelalo", "dejalo", "para", "parar", "olvidalo", "estoy bien"))
    private val repeat = Text.anyOf(listOf("repite", "repitelo", "otra vez", "como", "no te he entendido", "que has dicho"))
    private val yes = setOf("si", "vale", "venga", "claro", "de acuerdo", "si si", "si claro", "si por favor", "adelante", "hazlo", "llama", "ok", "okey", "eso", "exacto")
    private val no = setOf("no", "no no", "no gracias", "mejor no", "no llames", "nada", "ahora no")

    fun parse(utterance: String): Command {
        val folded = Text.fold(utterance)
        val clean = fillers.replace(folded, " ").replace(Regex("[^a-z0-9 ]"), " ").replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return Command.Unknown(utterance)

        if (emergency.containsMatchIn(clean)) return Command.Emergency
        if (clean in yes) return Command.Yes
        if (clean in no) return Command.No
        if (cancel.containsMatchIn(clean) && clean.split(" ").size <= 3) return Command.Cancel

        sendMessage(utterance, folded)?.let { return it }
        callVerbs.find(clean)?.let { m ->
            val target = m.groupValues[2].trim()
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
        val bodyRange = m.groups[2]!!.range
        val body = if (folded.length == utterance.length) utterance.substring(bodyRange.first, bodyRange.last + 1) else m.groupValues[2]
        val text = trailingPlease.replace(body, "").trim().trimEnd('.', '!')
        if (text.isEmpty()) return null
        return Command.SendMessage(m.groupValues[1].trim(), text)
    }
}
