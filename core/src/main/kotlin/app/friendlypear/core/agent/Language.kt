package app.friendlypear.core.agent

import app.friendlypear.core.model.Lang
import app.friendlypear.core.shield.Text

/**
 * The language a chat message is written in, so Peri answers in it even on a phone set to the other one ("Tengo un
 * problema" on an English phone got an English answer). Accents, ñ and ¿¡ say Spanish at once; otherwise the short
 * words each language can't do without, and only those that aren't also words of the other ("me", "a", "no", "he").
 * Null when it can't tell (a name, "ok", a number): the conversation keeps its language.
 */
object Language {
    private val spanishLetters = Regex("[ñ¿¡áéíóú]")
    private val spanish = Regex(
        "\\b(que|de|el|la|los|las|un|una|unos|unas|mi|mis|tu|tus|su|sus|es|son|esta|estoy|estas|tengo|tiene|tienes|hay|" +
            "por|para|con|pero|como|cuando|donde|y|o|en|del|al|lo|le|les|se|nos|algo|nada|todo|muy|bien|mal|ahora|hoy|ayer|" +
            "hola|gracias|vale|si|ha|han|hemos|habia|puedo|puedes|quiero|necesito|llamado|llamo|llamada|mensaje|dinero|" +
            "banco|hijo|hija|nieto|nieta|ayuda|ayudame|problema|timo|estafa|cuenta|codigo|enlace|movil|telefono|pasa|paso)\\b",
    )
    private val english = Regex(
        "\\b(the|an|my|i|i m|im|is|are|was|were|it|its|it s|you|your|what|how|why|can|could|should|would|have|had|do|does|" +
            "did|don t|didn t|this|that|with|for|from|of|to|and|or|in|on|at|just|got|get|someone|somebody|hello|hi|hey|thanks|" +
            "thank|please|yes|called|call|message|text|money|bank|son|daughter|help|problem|scam|link|account|code|phone|" +
            "happened|weird|strange|think)\\b",
    )

    fun of(text: String): Lang? {
        if (spanishLetters.containsMatchIn(text.lowercase())) return Lang.ES
        val folded = Text.fold(text)
        val es = spanish.findAll(folded).count()
        val en = english.findAll(folded).count()
        return when {
            es > en -> Lang.ES
            en > es -> Lang.EN
            else -> null
        }
    }
}
