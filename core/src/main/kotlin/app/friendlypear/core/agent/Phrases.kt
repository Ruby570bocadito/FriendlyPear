package app.friendlypear.core.agent

import app.friendlypear.core.model.Lang

/** Everything Peri says outside the shield's verdicts, in each language. Kind, short, one idea per sentence. */
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
    override fun offerCall(explanation: String, name: String) = "$explanation Si me dices que sí, llamo a $name."
    override fun callUsual(name: String) = "¿Llamo a $name a su número de siempre?"
    override fun whoToCall(names: String) = "¿A quién llamo: $names?"
    override fun whichOne(names: String) = "¿A quién: $names?"
    override fun confirmCall(name: String) = "¿Llamo a $name?"
    override fun confirmSms(name: String, text: String) = "¿Le mando a $name este mensaje: «$text»?"
    override fun yesOrNo(question: String) = "Dime «sí» o «no». $question"
    override val doingNothing = "Vale, no hago nada."
    override val ok = "Vale."
    override val whatToDo = "¿Qué quieres que haga?"
    override val nothingSaidYet = "No te había dicho nada todavía."
    override val help = "No te he entendido. Puedes decirme, por ejemplo, «llama a Laura» o «léeme los mensajes»."
    override val notFound = "No encuentro a esa persona en tu agenda. ¿Cómo se llama?"
    override fun noNumber(names: String) = "No tengo el número de $names."
    override val needsGuardian = "Mandar un código es arriesgado: eso tiene que aprobarlo alguien de tu familia, así que no lo mando."
    override fun calling(name: String) = "Llamo a $name."
    override fun callFailed(name: String, phone: String) = "No he podido llamar a $name. Su número es $phone; puedes marcarlo tú."
    override fun smsSent(name: String) = "Mensaje enviado a $name."
    override fun smsFailedOfferCall(name: String) = "No he podido mandar el mensaje a $name. ¿Quieres que llame?"
    override val noMessages = "No tienes mensajes nuevos."
    override fun youHave(count: Int) = "Tienes $count mensajes. "
    override fun from(who: String) = "De $who:"
    override val careful = "Ojo:"
    override val notReading = "no te lo leo."
    override val unknownEmail = "una dirección de correo que no está en tu agenda"
    override val unknownNumber = "un número que no está en tu agenda"
    override fun countdown(who: String?) =
        "¿Estás bien? Si estás bien, di «cancelar». Si no, en diez segundos aviso a ${who ?: "tu familia"}."
    override val sayCancelIfFine = "Si estás bien, di «cancelar»."
    override val emergencyCancelled = "Vale, me alegro de que estés bien. No aviso a nadie."
    override val noFamily = "No tengo a nadie de tu familia guardado para avisar. Si puedes, pide ayuda en voz alta."
    override fun alerting(name: String) = "Aviso a $name."
    override val couldNotCallAnyone = "No he podido llamar a nadie. Les he mandado un mensaje; si puedes, pide ayuda en voz alta."
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
    override fun offerCall(explanation: String, name: String) = "$explanation If you say yes, I'll call $name."
    override fun callUsual(name: String) = "Shall I call $name on their usual number?"
    override fun whoToCall(names: String) = "Who shall I call: $names?"
    override fun whichOne(names: String) = "Which one: $names?"
    override fun confirmCall(name: String) = "Shall I call $name?"
    override fun confirmSms(name: String, text: String) = "Shall I send $name this message: \"$text\"?"
    override fun yesOrNo(question: String) = "Please say \"yes\" or \"no\". $question"
    override val doingNothing = "OK, I won't do anything."
    override val ok = "OK."
    override val whatToDo = "What would you like me to do?"
    override val nothingSaidYet = "I hadn't said anything yet."
    override val help = "I didn't understand. You can say, for example, \"call Laura\" or \"read me my messages\"."
    override val notFound = "I can't find that person in your contacts. What's their name?"
    override fun noNumber(names: String) = "I don't have a number for $names."
    override val needsGuardian = "Sending a code is risky: someone in your family has to approve it, so I won't send it."
    override fun calling(name: String) = "Calling $name."
    override fun callFailed(name: String, phone: String) = "I couldn't call $name. Their number is $phone; you can dial it yourself."
    override fun smsSent(name: String) = "Message sent to $name."
    override fun smsFailedOfferCall(name: String) = "I couldn't send the message to $name. Shall I call instead?"
    override val noMessages = "You have no new messages."
    override fun youHave(count: Int) = "You have $count messages. "
    override fun from(who: String) = "From $who:"
    override val careful = "Careful:"
    override val notReading = "I won't read it to you."
    override val unknownEmail = "an email address that isn't in your contacts"
    override val unknownNumber = "a number that isn't in your contacts"
    override fun countdown(who: String?) =
        "Are you OK? If you're fine, say \"cancel\". If not, in ten seconds I'll alert ${who ?: "your family"}."
    override val sayCancelIfFine = "If you're fine, say \"cancel\"."
    override val emergencyCancelled = "OK, I'm glad you're fine. I won't alert anyone."
    override val noFamily = "I have no family member saved to alert. If you can, call out for help."
    override fun alerting(name: String) = "Alerting $name."
    override val couldNotCallAnyone = "I couldn't call anyone. I've sent them a message; if you can, call out for help."
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
