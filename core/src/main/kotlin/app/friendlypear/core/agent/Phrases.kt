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
    fun unknownCaller(hasFamilyWord: Boolean): String
    val whoClaimed: String
    fun notTheirNumber(name: String, hasFamilyWord: Boolean): String
    fun notAnyOf(names: String): String
    val officialCaller: String
    val callerAdvice: String
    fun askingGuardian(names: String): String
    val guardianApproved: String
    val guardianRejected: String
    val guardianSilent: String
    val couldNotAsk: String
    fun guardianSms(toName: String, url: String): String
    fun remoteAppInstalled(app: String, onCall: Boolean): String
    fun tellFamily(name: String): String
    fun remoteAppFamilyText(app: String): String
    fun callWithoutPermission(name: String, phone: String): String
    fun smsWithoutPermission(name: String): String
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
    override fun unknownCaller(hasFamilyWord: Boolean) =
        "Te llama un número que no está en tu agenda. Si dice ser de tu familia, no des dinero ni códigos" +
            if (hasFamilyWord) " y pregúntale la palabra de la familia." else "; al colgar lo comprobamos juntos."
    override val whoClaimed = "Ese número no está en tu agenda. ¿Quién decía ser?"
    override fun notTheirNumber(name: String, hasFamilyWord: Boolean) =
        "Ese número no es el de $name. " +
            (if (hasFamilyWord) "Si vuelve a llamar, pregúntale la palabra de la familia. "
            else "Os vendría bien acordar una palabra de la familia, una palabra secreta para saber que es de verdad quien llama. ") +
            "¿Llamo a $name a su número de siempre para comprobarlo?"
    override fun notAnyOf(names: String) = "Ese número no es el de ninguno de ellos. ¿A quién llamo para comprobarlo, a $names?"
    override val officialCaller =
        "Si decía ser del banco o de una empresa, lo más seguro es colgar y llamar tú al número oficial, el de tu tarjeta o tu factura. Nunca des claves por teléfono."
    override val callerAdvice = "Vale. Si alguien te pide dinero o códigos por teléfono, cuelga y llámale tú a su número de siempre."
    override fun askingGuardian(names: String) =
        "Mandar un código es delicado, así que se lo pregunto a $names. En cuanto me conteste, te digo. Mientras, aquí sigo."
    override val guardianApproved = "¡Buenas noticias! Tu familia lo ha aprobado."
    override val guardianRejected =
        "Tu familia prefiere que no lo mandemos, así que no lo envío. Seguro que tienen sus motivos; si quieres, les llamamos."
    override val guardianSilent = "Tu familia no ha contestado a tiempo, así que no lo mando. Lo más seguro es esperar y preguntarles luego."
    override val couldNotAsk = "No he podido preguntar a tu familia ahora mismo, así que no lo mando. Lo intentamos más tarde."
    override fun guardianSms(toName: String, url: String) =
        "Peri: tu familiar quiere mandar a $toName un mensaje con un código y te pide permiso. Decide aquí (caduca en 15 minutos): $url"
    override fun remoteAppInstalled(app: String, onCall: Boolean) =
        (if (onCall) "Cuelga ahora mismo, por favor. " else "") +
            "Se acaba de instalar $app, un programa para que otra persona maneje tu móvil desde lejos. " +
            "¿Alguien te ha pedido que lo instales? Nadie de tu banco ni de ninguna empresa te lo pedirá nunca. " +
            "No le des acceso a nadie y, si quieres, lo borramos juntos."
    override fun tellFamily(name: String) = "¿Se lo cuento a $name?"
    override fun remoteAppFamilyText(app: String) =
        "Peri: en el móvil de tu familiar se acaba de instalar $app, un programa para controlarlo desde lejos. Llámale para ver si alguien le está guiando por teléfono."
    override fun callWithoutPermission(name: String, phone: String) =
        "Vale, sin ese permiso no llamo yo. El número de $name es $phone; si quieres, márcalo tú. Puedes darme el permiso cuando quieras."
    override fun smsWithoutPermission(name: String) =
        "Vale, sin ese permiso no mando mensajes. Puedes escribir a $name desde tu app de mensajes. Puedes darme el permiso cuando quieras."
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
    override fun unknownCaller(hasFamilyWord: Boolean) =
        "This number isn't in your contacts. If they say they're family, don't give money or codes" +
            if (hasFamilyWord) " and ask them for the family word." else "; we'll check together when you hang up."
    override val whoClaimed = "That number isn't in your contacts. Who did they say they were?"
    override fun notTheirNumber(name: String, hasFamilyWord: Boolean) =
        "That number isn't $name's. " +
            (if (hasFamilyWord) "If they call again, ask them for the family word. "
            else "It would be a good idea to agree on a family word, a secret word to know it's really them calling. ") +
            "Shall I call $name on their usual number to check?"
    override fun notAnyOf(names: String) = "That number isn't any of theirs. Who shall I call to check: $names?"
    override val officialCaller =
        "If they said they were your bank or a company, the safest thing is to hang up and call the official number yourself, the one on your card or bill. Never give passwords over the phone."
    override val callerAdvice = "OK. If anyone asks you for money or codes on the phone, hang up and call them back on their usual number."
    override fun askingGuardian(names: String) =
        "Sending a code is delicate, so I'm asking $names. As soon as they answer, I'll tell you. I'm still here meanwhile."
    override val guardianApproved = "Good news! Your family has approved it."
    override val guardianRejected =
        "Your family would rather we didn't send it, so I won't. I'm sure they have their reasons; we can call them if you like."
    override val guardianSilent = "Your family didn't answer in time, so I won't send it. The safest thing is to wait and ask them later."
    override val couldNotAsk = "I couldn't ask your family right now, so I won't send it. Let's try again later."
    override fun guardianSms(toName: String, url: String) =
        "Peri: your relative wants to send $toName a message with a code and asks for your OK. Decide here (expires in 15 minutes): $url"
    override fun remoteAppInstalled(app: String, onCall: Boolean) =
        (if (onCall) "Please hang up right now. " else "") +
            "$app has just been installed, a program that lets someone else control your phone from far away. " +
            "Did someone ask you to install it? Nobody from your bank or any company will ever ask you to. " +
            "Don't give anyone access and, if you like, we'll remove it together."
    override fun tellFamily(name: String) = "Shall I let $name know?"
    override fun remoteAppFamilyText(app: String) =
        "Peri: $app, a program to control the phone from far away, has just been installed on your relative's phone. Give them a call to check nobody is guiding them over the phone."
    override fun callWithoutPermission(name: String, phone: String) =
        "OK, without that permission I won't call. $name's number is $phone, if you'd like to dial it yourself. You can give me the permission any time."
    override fun smsWithoutPermission(name: String) =
        "OK, without that permission I won't send messages. You can write to $name from your messages app. You can give me the permission any time."
    override val or = "or"
    override fun quote(text: String) = "\"$text\""
    override val link = "a link"
    override val email = "an email address"
    override val digits = "some numbers"
    override val moneyWords = listOf("€", "euro", "$", "£", "dollar", "pound")
}
