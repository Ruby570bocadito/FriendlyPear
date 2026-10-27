package app.friendlypear.core.voice

import app.friendlypear.core.shield.Text

/** Spanish diminutives and nicknames: "Pablito" is Pablo, "Pepe" is José. Inputs are folded words. */
internal object Diminutives {
    private val nicknames = mapOf(
        "pepe" to "jose", "pepito" to "jose", "paco" to "francisco", "paquito" to "francisco", "curro" to "francisco",
        "manolo" to "manuel", "manolito" to "manuel", "lola" to "dolores", "loli" to "dolores", "charo" to "rosario",
        "concha" to "concepcion", "conchi" to "concepcion", "nacho" to "ignacio", "quique" to "enrique", "toni" to "antonio",
        "tono" to "antonio", "chema" to "jose", "merche" to "mercedes", "pili" to "pilar", "maite" to "teresa",
        "tere" to "teresa", "chus" to "jesus", "chuchi" to "jesus", "rafa" to "rafael", "nando" to "fernando",
        "fer" to "fernando", "isa" to "isabel", "susi" to "susana", "javi" to "javier", "alex" to "alejandro",
        "edu" to "eduardo", "dani" to "daniel", "migue" to "miguel", "luisito" to "luis", "juanito" to "juan",
        "juanma" to "juan", "mari" to "maria", "marisa" to "maria", "chelo" to "consuelo", "puri" to "purificacion",
        "inma" to "inmaculada", "nati" to "natividad", "rosi" to "rosa", "lupe" to "guadalupe", "santi" to "santiago",
    )

    private val suffixes = listOf("cito", "cita", "ecito", "ecita", "ito", "ita", "illo", "illa", "ico", "ica", "in", "ina", "ete", "eta", "i", "y")

    /** How sure we are that a spoken word is a name; a stronger match hides the weaker ones. */
    enum class Match { NONE, SIMILAR, DIMINUTIVE, EXACT }

    fun match(spoken: String, name: String): Match = when {
        spoken == name -> Match.EXACT
        nicknames[spoken] == name -> Match.DIMINUTIVE
        stem(name).length >= 2 && stems(spoken).any { it == stem(name) || it == name } -> Match.DIMINUTIVE
        spoken.length >= 5 && name.length >= 5 && Text.editDistance(spoken, name) <= 1 -> Match.SIMILAR
        else -> Match.NONE
    }

    private fun stem(word: String): String = if (word.last() in "aeo") word.dropLast(1) else word

    /** "pablito" → pabl, "carmencita" → carmen, "paquito" → pac, "lauri" → laur. */
    private fun stems(word: String): List<String> =
        suffixes.filter { word.endsWith(it) && word.length - it.length >= 2 }
            .map { word.dropLast(it.length) }
            .flatMap { listOf(it, it.replace(Regex("qu$"), "c").replace(Regex("gu$"), "g")) }
            .distinct()
}
