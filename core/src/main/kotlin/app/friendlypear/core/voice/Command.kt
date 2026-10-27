package app.friendlypear.core.voice

/** What the person asked for, before Peri resolves contacts or asks for confirmation. */
sealed interface Command {
    /** "llama al chico", "ponme con Pablito": [target] is the spoken words left after the verb. */
    data class Call(val target: String) : Command

    /** "léeme los mensajes"; [onlyLast] for "léeme el último". */
    data class ReadMessages(val onlyLast: Boolean = false) : Command

    /** "me he caído", "socorro": starts the countdown, never dials an emergency number by itself. */
    data object Emergency : Command

    data object Yes : Command
    data object No : Command
    data object Cancel : Command
    data object Repeat : Command

    data class Unknown(val utterance: String) : Command
}
