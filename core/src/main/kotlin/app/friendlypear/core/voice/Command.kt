package app.friendlypear.core.voice

/** What the person asked for, before Peri resolves contacts or asks for confirmation. */
sealed interface Command {
    /** "llama al chico", "ponme con Pablito": [target] is the spoken words left after the verb. */
    data class Call(val target: String) : Command

    /** "dile a Laura que llego tarde": [text] keeps the person's own words, accents included. */
    data class SendMessage(val target: String, val text: String) : Command

    /** "léeme los mensajes"; [onlyLast] for "léeme el último". */
    data class ReadMessages(val onlyLast: Boolean = false) : Command

    /** "me he caído", "socorro": starts the countdown, never dials an emergency number by itself. */
    data object Emergency : Command

    /** "Le he dado el código", "he hecho un bizum": something already given away to a scammer. */
    data class Gave(val what: app.friendlypear.core.agent.Breach) : Command

    /** "Me ha llamado el banco, ¿qué hago?": a question about staying safe, answered with reviewed advice. */
    data class Advise(val topic: app.friendlypear.core.agent.Topic) : Command

    /** "Hola", "gracias", "cuéntame un chiste": what people say to a friend, answered in Peri's own words. */
    data class Chat(val kind: app.friendlypear.core.agent.SmallTalk) : Command

    data object Yes : Command
    data object No : Command
    data object Cancel : Command
    data object Repeat : Command

    data class Unknown(val utterance: String) : Command
}
