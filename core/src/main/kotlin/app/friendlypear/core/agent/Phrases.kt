package app.friendlypear.core.agent

import app.friendlypear.core.model.Lang

/** Everything Peri says outside the shield's verdicts, in each language. Warm, short, one idea per sentence: see docs/PERI_VOZ.md. */
internal interface Phrases {
    fun offerCall(explanation: String, name: String): String
    fun callUsual(name: String): String
    fun whoToCall(names: String): String
    fun whichOne(names: String): String
    fun confirmCall(name: String): String
    fun confirmSms(name: String, text: String): String
    fun yesOrNo(question: String): String
    val doingNothing: String
    val ok: String
    val whatToDo: String
    val nothingSaidYet: String
    val help: String
    val notFound: String
    fun noNumber(names: String): String
    val needsGuardian: String
    fun calling(name: String): String
    fun callFailed(name: String, phone: String): String
    fun smsSent(name: String): String
    fun smsFailedOfferCall(name: String): String
    val noMessages: String
    fun youHave(count: Int): String
    fun from(who: String): String
    val careful: String
    val notReading: String
    val unknownEmail: String
    val unknownNumber: String
    fun countdown(who: String?): String
    val sayCancelIfFine: String
    val emergencyCancelled: String
    val noFamily: String
    fun alerting(name: String): String
    val couldNotCallAnyone: String
    fun emergencySms(location: String?): String
    val or: String
    fun quote(text: String): String
    val link: String
    val email: String
    val digits: String
    /** Words after a number that make it an amount of money, which is fine to read. */
    val moneyWords: List<String>

    companion object {
        fun of(lang: Lang): Phrases = when (lang) {
            Lang.ES -> Spanish
            Lang.EN -> English
        }
    }
}

private fun mapsLink(location: String) = "https://maps.google.com/?q=$location"

internal object Spanish : Phrases {
    override fun offerCall(explanation: String, name: String) = "$explanation Si me dices «sí», llamo ahora mismo a $name."
    override fun callUsual(name: String) = "¿Llamo a $name a su número de siempre?"
    override fun whoToCall(names: String) = "¿A quién llamo, a $names?"
    override fun whichOne(names: String) = "¿A quién te refieres, a $names?"
    override fun confirmCall(name: String) = "¡Claro! ¿Llamo a $name?"
    override fun confirmSms(name: String, text: String) = "¡Claro! ¿Le mando a $name este mensaje: «$text»?"
    override fun yesOrNo(question: String) = "Perdona, no te he oído bien. Dime «sí» o «no»: $question"
    override val doingNothing = "Vale, no hago nada. Aquí sigo si me necesitas."
    override val ok = "Vale, aquí estoy."
    override val whatToDo = "¡Dime! ¿En qué te ayudo?"
    override val nothingSaidYet = "Todavía no te había dicho nada. ¿En qué te ayudo?"
    override val help = "Perdona, no te he entendido. Puedes decirme, por ejemplo, «llama a Laura» o «léeme los mensajes»."
    override val notFound = "Vaya, no encuentro a esa persona en tu agenda. ¿Me dices cómo se llama?"
    override fun noNumber(names: String) = "Vaya, no tengo el número de $names."
    override val needsGuardian = "Mandar un código es delicado, así que prefiero que lo apruebe alguien de tu familia. De momento no lo mando."
    override fun calling(name: String) = "¡Allá voy! Llamo a $name."
    override fun callFailed(name: String, phone: String) = "Vaya, no he podido llamar a $name. Su número es $phone; si quieres, márcalo tú."
    override fun smsSent(name: String) = "¡Listo! Mensaje enviado a $name."
    override fun smsFailedOfferCall(name: String) = "Vaya, no he podido mandar el mensaje a $name. ¿Prefieres que llame?"
    override val noMessages = "No tienes mensajes nuevos. ¡Todo tranquilo!"
    override fun youHave(count: Int) = "Tienes $count mensajes. "
    override fun from(who: String) = "De $who:"
    override val careful = "Ojo:"
    override val notReading = "este no te lo leo, que trae trampa."
    override val unknownEmail = "una dirección de correo que no está en tu agenda"
    override val unknownNumber = "un número que no está en tu agenda"
    override fun countdown(who: String?) =
        "¿Estás bien? Estoy contigo. Si estás bien, di «cancelar». Si no, en diez segundos aviso a ${who ?: "tu familia"}."
    override val sayCancelIfFine = "Si estás bien, di «cancelar»; si no, enseguida pido ayuda."
    override val emergencyCancelled = "¡Qué alivio! Me alegro de que estés bien. No aviso a nadie."
    override val noFamily = "No tengo a nadie de tu familia guardado para avisar. Si puedes, pide ayuda en voz alta; yo sigo aquí."
    override fun alerting(name: String) = "Ya aviso a $name. Estoy contigo."
    override val couldNotCallAnyone = "No he podido llamar a nadie, pero les he mandado un mensaje. Si puedes, pide ayuda en voz alta."
    override fun emergencySms(location: String?) =
        "Aviso de Peri: tu familiar ha dicho que se ha caído y no ha cancelado el aviso." +
            (location?.let { " Ubicación: ${mapsLink(it)}" } ?: " No tengo su ubicación.")
    override val or = "o"
    override fun quote(text: String) = "«$text»"
    override val link = "un enlace"
    override val email = "una dirección de correo"
    override val digits = "unos números"
    override val moneyWords = listOf("€", "euro", "$", "£", "dolar", "libra")
}

internal object English : Phrases {
    override fun offerCall(explanation: String, name: String) = "$explanation If you say \"yes\", I'll call $name right away."
    override fun callUsual(name: String) = "Shall I call $name on their usual number?"
    override fun whoToCall(names: String) = "Who shall I call: $names?"
    override fun whichOne(names: String) = "Who do you mean: $names?"
    override fun confirmCall(name: String) = "Sure! Shall I call $name?"
    override fun confirmSms(name: String, text: String) = "Sure! Shall I send $name this message: \"$text\"?"
    override fun yesOrNo(question: String) = "Sorry, I didn't quite catch that. Just say \"yes\" or \"no\": $question"
    override val doingNothing = "OK, I won't do anything. I'm here if you need me."
    override val ok = "OK, I'm here."
    override val whatToDo = "Tell me! How can I help?"
    override val nothingSaidYet = "I haven't said anything yet. How can I help?"
    override val help = "Sorry, I didn't understand. You can say, for example, \"call Laura\" or \"read me my messages\"."
    override val notFound = "Hmm, I can't find that person in your contacts. What's their name?"
    override fun noNumber(names: String) = "Hmm, I don't have a number for $names."
    override val needsGuardian = "Sending a code is delicate, so I'd rather someone in your family approved it. I won't send it for now."
    override fun calling(name: String) = "On it! Calling $name."
    override fun callFailed(name: String, phone: String) = "Oh dear, I couldn't call $name. Their number is $phone, if you'd like to dial it yourself."
    override fun smsSent(name: String) = "Done! Message sent to $name."
    override fun smsFailedOfferCall(name: String) = "Oh dear, I couldn't send the message to $name. Would you rather I called?"
    override val noMessages = "You have no new messages. All quiet!"
    override fun youHave(count: Int) = "You have $count messages. "
    override fun from(who: String) = "From $who:"
    override val careful = "Careful:"
    override val notReading = "I won't read this one, it's a trap."
    override val unknownEmail = "an email address that isn't in your contacts"
    override val unknownNumber = "a number that isn't in your contacts"
    override fun countdown(who: String?) =
        "Are you OK? I'm here with you. If you're fine, say \"cancel\". If not, in ten seconds I'll alert ${who ?: "your family"}."
    override val sayCancelIfFine = "If you're fine, say \"cancel\"; if not, I'll get help in a moment."
    override val emergencyCancelled = "Phew, what a relief! I'm glad you're fine. I won't alert anyone."
    override val noFamily = "I have no family member saved to alert. If you can, call out for help; I'm still here."
    override fun alerting(name: String) = "I'm alerting $name now. I'm here with you."
    override val couldNotCallAnyone = "I couldn't call anyone, but I've sent them a message. If you can, call out for help."
    override fun emergencySms(location: String?) =
        "Alert from Peri: your relative said they have fallen and did not cancel the alert." +
            (location?.let { " Location: ${mapsLink(it)}" } ?: " I don't have their location.")
    override val or = "or"
    override fun quote(text: String) = "\"$text\""
    override val link = "a link"
    override val email = "an email address"
    override val digits = "some numbers"
    override val moneyWords = listOf("€", "euro", "$", "£", "dollar", "pound")
}
