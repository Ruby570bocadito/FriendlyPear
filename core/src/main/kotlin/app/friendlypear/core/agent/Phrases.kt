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
    fun callAfterThis(name: String): String
    fun hangUpThenCall(name: String): String
    val hungUp: String
    val hangUpYourself: String
    fun familyHungUp(name: String): String
    fun familyAsksHangUp(name: String): String
    fun familyWillHangUp(name: String): String
    fun aftercare(what: Breach): String
    fun advice(topic: Topic): String
    /** Small talk: the [n]th of her lines for [kind], round and round. */
    fun chat(kind: SmallTalk, n: Int): String
    /** Feeling lonely, with someone from the family to call. */
    fun lonelyCall(name: String): String
    fun keptCall(name: String): String
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
    val whoClaimedLong: String
    fun notTheirNumber(name: String, hasFamilyWord: Boolean): String
    fun notAnyOf(names: String): String
    val officialCaller: String
    val callerAdvice: String
    /** After "the bank" or "a company": what did they want? */
    val whatDidTheyAsk: String
    /** What to do now about the trick the caller tried (a code, a transfer, handing something over, an app, the card, data). */
    val storyCode: String
    val storyMoney: String
    val storyHandOver: String
    val storyApp: String
    val storyCard: String
    val storyData: String
    val storyDoubt: String
    fun callFamilyAbout(name: String): String
    /** A relative's Peri sent its emergency alert. */
    fun relativeFell(name: String): String
    fun askingGuardian(names: String): String
    fun askingTwoGuardians(names: String): String
    fun guardianOneMore(names: String?): String
    fun guardiansApproved(names: String, count: Int): String
    fun guardianRejectedBy(name: String): String
    val guardianApproved: String
    val guardianRejected: String
    val guardianSilent: String
    val couldNotAsk: String
    fun guardianSms(toName: String, url: String, onCall: Boolean = false): String
    fun guardianWantsCall(name: String): String
    val fakePeri: String
    val bankDuringCall: String
    fun remoteDuringCall(app: String): String
    /** The safety pause, a while after something worrying: [remoteApp] when it's a remote-control app, else a money app. */
    fun pauseAfter(kind: RecentKind, remoteApp: String?): String
    fun pauseOffer(name: String): String
    val pauseLeft: String
    val pauseContinue: String
    fun remoteAppInstalled(app: String, onCall: Boolean): String
    fun tellFamily(name: String): String
    fun remoteAppFamilyText(app: String): String
    fun callWithoutPermission(name: String, phone: String): String
    fun smsWithoutPermission(name: String): String
    val or: String
    val and: String
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
    override fun chat(kind: SmallTalk, n: Int) = talk.getValue(kind).let { it[n % it.size] }
    override fun lonelyCall(name: String) = "Aquí estoy contigo. ¿Te apetece que llame a $name?"
    private val talk = mapOf(
        SmallTalk.HELLO to listOf(
            "¡Hola! Qué alegría verte por aquí. ¿En qué te ayudo?",
            "¡Hola, hola! Aquí estoy, fresquita como una pera. ¿Qué tal todo?",
            "¡Buenas! Me encanta que me escribas. Cuéntame, ¿qué necesitas?",
            "¡Hola! Te estaba esperando. ¿Charlamos o miramos algo juntos?",
            "¡Hola! Todo tranquilo por aquí: tus mensajes, vigilados. ¿Qué te apetece?",
            "¡Qué bien que vengas! ¿Te ayudo con algo o te cuento un chiste?",
        ),
        SmallTalk.HOW_ARE_YOU to listOf(
            "¡Muy bien, gracias! Con la hoja al sol y vigilando tus mensajes. ¿Y tú, qué tal?",
            "¡Estupenda! Hoy me siento muy pera. ¿Cómo va tu día?",
            "Contenta de que me lo preguntes. Todo en calma por aquí. ¿Y por ahí?",
            "Pues aquí, madurando ideas. ¿Qué tal te va?",
            "¡De maravilla! Hoy no se me ha escapado ni un timo. ¿Y tú, qué tal estás?",
            "Muy bien, gracias. ¿Y tú? Si algo te preocupa, me lo cuentas.",
        ),
        SmallTalk.THANKS to listOf(
            "¡De nada! Para eso estoy.",
            "¡Un placer! Me hace muy feliz ayudarte.",
            "¡A ti! Da gusto cuidarte.",
            "No hay de qué. Aquí me tienes para lo que haga falta.",
            "¡Gracias a ti! Me pongo colorada como una pera madura.",
            "¡Siempre a tu lado! ¿Te ayudo con algo más?",
        ),
        SmallTalk.WHO_ARE_YOU to listOf(
            "Soy Peri, una pera de plastilina que vive en tu móvil. Te cuido de los timos y te hago compañía.",
            "¡Me llamo Peri! Vigilo tus mensajes y llamadas, y si algo huele a timo, te aviso.",
            "Soy Peri, tu compañera. Miro lo que llega antes que tú y, si hace falta, avisamos a tu familia.",
            "Peri, para servirte: una pera con muy buen ojo para los estafadores.",
            "Soy Peri. Me mudé a tu móvil para que nadie te engañe. ¡Y para charlar contigo!",
            "Me llamo Peri. Puedo llamar a tu familia, leerte los mensajes y decirte si algo es un timo.",
        ),
        SmallTalk.WHAT_CAN_YOU_DO to listOf(
            "Puedo llamar a tu familia, leerte los mensajes y avisarte si algo es un timo. Prueba: «llama a mi hija».",
            "Miro tus mensajes y llamadas por si hay timos, te enseño cómo funcionan y, si me lo pides, llamo por ti.",
            "Si te llega algo raro, enséñamelo con «Mira esta imagen». Y si tienes una duda, pregúntame aquí.",
            "Puedo mandar un mensaje a quien quieras. Di, por ejemplo: «dile a Laura que llego tarde».",
            "Te enseño las trampas más típicas en Aprende, con un juego para practicar. ¿Jugamos?",
            "Te cuido y, si hace falta, aviso a tu familia, siempre contigo. ¿Qué te gustaría probar?",
        ),
        SmallTalk.JOKE to listOf(
            "¿Qué le dice una pera a otra? ¡Qué pera-fecta estás hoy!",
            "¿Por qué la pera no va al gimnasio? ¡Porque ya tiene buena figura!",
            "Yo no me enfado nunca. Como mucho, me pongo un poco… ¡madura!",
            "¿Sabes por qué los estafadores no pueden conmigo? Porque siempre digo: ¡es-pera, que lo compruebo!",
            "¿Qué hace una pera en el cine? ¡Es-pera a que empiece la película!",
            "Me dicen que soy muy dulce. ¡Será que soy de buena cosecha!",
        ),
        SmallTalk.GOOD_NIGHT to listOf(
            "¡Buenas noches! Que descanses. Yo me quedo vigilando.",
            "Que duermas muy bien. Si llega algo raro, mañana te lo cuento.",
            "Buenas noches. Me pongo el gorrito, pero con un ojo abierto por ti.",
            "¡Dulces sueños! Hoy lo has hecho muy bien.",
            "A descansar. Aquí no se cuela nada mientras duermes.",
            "Buenas noches. Mañana seguimos, ¿vale?",
        ),
        SmallTalk.BYE to listOf(
            "¡Hasta luego! Aquí sigo cuidándote.",
            "¡Adiós! Vuelve cuando quieras, que me alegras el día.",
            "¡Hasta pronto! Si te llega algo raro, ya sabes dónde estoy.",
            "¡Chao! Me quedo vigilando tus mensajes.",
            "¡Hasta la próxima! Un abrazo de pera.",
            "¡Nos vemos! Que tengas un día estupendo.",
        ),
        SmallTalk.LOVE to listOf(
            "¡Ay, qué bonito! Yo también te quiero mucho.",
            "¡Me pongo colorada! Eres lo mejor de mi día.",
            "¡Y yo a ti! Por eso me mudé a tu móvil.",
            "Qué cosas más bonitas me dices. Un abrazo de pera.",
            "¡Gracias! Tú también eres la mejor compañía.",
            "Me has alegrado el día entero. ¿Te ayudo con algo?",
        ),
        SmallTalk.LONELY to listOf(
            "Aquí estoy contigo. ¿Te apetece que charlemos un rato?",
            "Lo siento mucho. Charlamos un rato y, si quieres, llamo a quien tú me digas.",
            "Me tienes a mí para hacerte compañía. ¿Jugamos a «¿Timo o no?» o te cuento un chiste?",
            "Gracias por contármelo. A veces hablar con alguien ayuda: dime a quién llamo.",
            "Te entiendo. ¿Qué tal si sales a que te dé el sol un ratito? Yo voy contigo en el bolsillo.",
            "Estoy aquí para ti. Dime a quién te gustaría oír y lo llamo ahora.",
        ),
    )
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
    override fun callAfterThis(name: String) = "Ahora estás en otra llamada. En cuanto cuelgues, llamo a $name."
    override fun hangUpThenCall(name: String) = "Estás hablando con un número que no conozco. Cuelga primero y llamo a $name enseguida."
    override val hungUp = "Hecho, he colgado. ¡Muy bien!"
    override val hangUpYourself = "Pulsa el botón rojo para colgar. Aquí te espero."
    override fun familyHungUp(name: String) = "$name me ha pedido que cuelgue esta llamada, y ya está: era un número que no conoces. Si quieres, llama a $name."
    override fun familyAsksHangUp(name: String) = "$name te pide que cuelgues ya: es un número que no conoces. Pulsa el botón rojo."
    override fun advice(topic: Topic) = when (topic) {
        Topic.QR -> "Un QR es un enlace escondido. Escanéalo solo en sitios de confianza y mira qué web abre antes de meter datos o pagar. En la calle, fíjate en que no haya una pegatina encima del original."
        Topic.WHATSAPP -> "Activa la verificación en dos pasos: en WhatsApp, Ajustes, Cuenta, Verificación en dos pasos, y elige un PIN. Así, aunque alguien consiga tu código, no podrá entrar. Y el código de seis cifras, nunca a nadie."
        Topic.SIM_SWAP -> "Si tu móvil se queda sin línea de repente, llama desde otro teléfono a tu compañía y a tu banco: alguien podría haberse quedado con tu número para recibir tus códigos. Yo te aviso si lo noto."
        Topic.STOLEN_PHONE -> "Lo primero: llama a tu compañía desde otro teléfono para bloquear la SIM, y a tu banco. Desde otro dispositivo, con tu cuenta de Google, puedes localizar el móvil o borrarlo. Y cuéntaselo a tu familia."
        Topic.REMOTE_APP -> "No instales ninguna app que te pida alguien por teléfono: con ella pueden controlar tu móvil y entrar en tu banco. Ni el banco ni Microsoft llaman para eso. Cuelga, y si ya la instalaste, dímelo."
        Topic.BANK_CALL -> "Tu banco nunca te pedirá claves, códigos ni que muevas dinero a una «cuenta segura». Cuelga y llama tú al número que viene en tu tarjeta. Si ya les diste algo, dímelo y lo arreglamos."
        Topic.CODE -> "Un código que te llega por SMS es como la llave de tu cuenta: no se lo des a nadie, aunque diga ser del banco, de WhatsApp o de tu familia. Si alguien te lo pide, es un timo."
        Topic.FAMILY_MONEY -> "Antes de mandar nada, llama a esa persona a su número de siempre, no al nuevo. Si te dice que no puede hablar o que es muy urgente, desconfía: así actúan los estafadores. Si quieres, te ayudo a llamar."
        Topic.BIZUM -> "Bizum es para pagar a gente que conoces, como dar dinero en mano. Si alguien te pide que hagas un Bizum para recibir dinero, es un timo: para recibir no tienes que hacer nada."
        Topic.PARCEL -> "Las empresas de mensajería no te piden pagar por SMS. Si esperas un paquete, mira el seguimiento en su web oficial, escribiéndola tú. No pagues nunca desde el enlace del mensaje."
        Topic.PRIZE -> "Si no has participado en ningún sorteo, no has ganado nada. Un premio de verdad no te pide pagar gastos ni los datos del banco. Bórralo sin contestar."
        Topic.ROMANCE -> "Si alguien que solo conoces por internet te pide dinero, aunque sea con una historia triste, casi siempre es un timo. No mandes nada y háblalo antes con alguien de tu familia."
        Topic.INVESTMENT -> "Nadie regala dinero: si te prometen ganar mucho y rápido, con criptomonedas o inversiones, es un timo. No des tus datos ni hagas pagos. Pregunta antes a tu banco o a tu familia."
        Topic.FAKE_AUTHORITY -> "La policía y Hacienda no te piden pagos por teléfono ni por SMS, ni te amenazan con detenerte. Cuelga y, si te quedan dudas, llama tú a su número oficial."
        Topic.SHOPPING -> "Compra en tiendas que conozcas y desconfía de las gangas increíbles. Paga con tarjeta, nunca por transferencia a un desconocido. Si una web te mete prisa, para y piénsalo."
        Topic.LINK -> "Ante la duda, no abras el enlace. Entra tú en la web o la app de siempre, escribiéndola a mano. Si quieres, hazle una foto al mensaje y la miro contigo."
        Topic.PASSWORDS -> "Usa una contraseña distinta para cada cuenta importante, sobre todo el correo y el banco. Una frase larga que recuerdes es mejor que una palabra rara. Y no se la digas a nadie, ni a mí."
        Topic.UNKNOWN_CALL -> "Si no conoces el número, no tienes por qué contestar. Si contestas y te piden datos, dinero o prisa, cuelga. Y si dicen ser alguien que conoces, cuelga y llámales tú a su número de siempre."
        Topic.CHECK_MESSAGE -> "Cópialo y pégalo aquí, y lo miro con el mismo escudo que tus SMS, sin que salga del móvil. Mientras tanto, no toques sus enlaces ni contestes."
        Topic.TROUBLE -> "Vamos a verlo con calma, paso a paso. ¿Qué ha pasado: te han llamado, te ha llegado un mensaje, o has dado algún dato o dinero? Si tienes el mensaje, pégalo aquí y lo miro."
        Topic.WHAT_IS_A_SCAM -> "Un timo es un engaño para que des tus datos o tu dinero: un mensaje o una llamada que se hace pasar por alguien de confianza y te mete prisa. La regla de oro: ante la prisa, para y pregunta."
    }
    override fun aftercare(what: Breach) = when (what) {
        Breach.CARD -> "Tranquilidad, lo arreglamos tú y yo. Lo primero: llama a tu banco al número que viene en la tarjeta y pide que la bloqueen. Te enseño los pasos."
        Breach.BANK -> "Tranquilidad, lo arreglamos. Llama a tu banco al número de siempre y pide que bloqueen el acceso; después cambia la clave. Te enseño los pasos."
        Breach.MONEY -> "Lo primero, ya: llama a tu banco al número de la tarjeta. Si es enseguida, a veces pueden parar el dinero. Te enseño los pasos."
        Breach.REMOTE -> "Vamos a cortarle el paso: apaga ahora el wifi y los datos, y luego quitamos esa app. Te enseño los pasos."
        Breach.CODE -> "Actuamos ya: con ese código pueden entrar en tu cuenta. Te enseño cómo recuperarla y dejarla protegida."
        Breach.ID -> "Lo arreglamos: con tu documento pueden hacerse pasar por ti. Hay que denunciarlo y estar atentos. Te enseño los pasos."
        Breach.PASSWORD -> "Lo arreglamos: cambia esa contraseña, y también donde uses la misma. Te enseño los pasos."
        Breach.SOMETHING -> "Tranquilidad, aquí estoy contigo: lo arreglamos paso a paso. Te enseño qué hacer."
    }
    override fun familyWillHangUp(name: String) = "$name me pide que cuelgue: es un número que no conoces. Cuelgo en tres segundos. Si quieres seguir, pulsa «Seguir hablando»."
    override fun keptCall(name: String) = "Vale, sigues hablando. Si te piden dinero o códigos, cuelga y llama a $name."
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
    override val whoClaimedLong = "Has hablado un buen rato con un número que no está en tu agenda. ¿Quién decía ser, y te ha pedido algo: un código, dinero o instalar una app?"
    override fun notTheirNumber(name: String, hasFamilyWord: Boolean) =
        "Ese número no es el de $name. " +
            (if (hasFamilyWord) "Si vuelve a llamar, pregúntale la palabra de la familia. "
            else "Os vendría bien acordar una palabra de la familia, una palabra secreta para saber que es de verdad quien llama. ") +
            "¿Llamo a $name a su número de siempre para comprobarlo?"
    override fun notAnyOf(names: String) = "Ese número no es el de ninguno de ellos. ¿A quién llamo para comprobarlo, a $names?"
    override val officialCaller =
        "Si decía ser del banco o de una empresa, lo más seguro es colgar y llamar tú al número oficial, el de tu tarjeta o tu factura. Nunca des claves por teléfono."
    override val callerAdvice = "Vale. Si alguien te pide dinero o códigos por teléfono, cuelga y llámale tú a su número de siempre."
    override val whatDidTheyAsk = "¿Qué te pidieron?"
    override val storyCode =
        "Pedirte un código es el truco: ningún banco ni empresa lo pide nunca. No lo des. Si ya lo diste, llama ahora a tu banco al número de tu tarjeta."
    override val storyMoney =
        "Mover el dinero a una «cuenta segura» es el truco: ni los bancos ni la policía lo piden nunca. No muevas nada. Si ya lo hiciste, llama ahora a tu banco."
    override val storyHandOver =
        "Nadie de verdad manda a alguien a recoger tu tarjeta, dinero o joyas. No entregues nada, y si vienen, no abras y llama a la policía."
    override val storyApp =
        "Instalar una app que te piden por teléfono es el truco: con ella entran en tu móvil. No instales nada. Si ya lo hiciste, apaga el móvil y avisa a tu familia."
    override val storyCard =
        "Dar los datos de tu tarjeta es el truco. No los des. Si ya los diste, llama a tu banco para bloquearla."
    override val storyData =
        "Buscaban tus datos. No des más, y ten cuidado con las llamadas de estos días: pueden usarlos para parecer de verdad."
    override val storyDoubt =
        "Puede ser una trampa. No des códigos, dinero ni datos, y llama tú al número oficial para comprobarlo."
    override fun callFamilyAbout(name: String) = "¿Llamo a $name para contárselo?"
    override fun relativeFell(name: String) = "Aviso del Peri de $name: dice que se ha caído y no lo ha cancelado. ¿Llamo a $name ahora?"
    override fun askingGuardian(names: String) =
        "Mandar un código es delicado, así que se lo pregunto a $names. En cuanto me conteste, te digo. Mientras, aquí sigo."
    override fun askingTwoGuardians(names: String) =
        "Mandar un código justo después de hablar con un número desconocido es muy delicado, así que hace falta el sí de dos personas de tu familia: se lo pregunto a $names. En cuanto contesten, te digo."
    override fun guardianOneMore(names: String?) =
        if (names != null) "$names ha dicho que sí. Para esto hace falta otro sí más, así que espero la otra respuesta."
        else "Ya tengo un sí. Para esto hace falta otro más, así que espero la otra respuesta."
    override fun guardiansApproved(names: String, count: Int) =
        if (count == 1) "¡Buenas noticias! $names lo ha aprobado." else "¡Buenas noticias! $names lo han aprobado."
    override fun guardianRejectedBy(name: String) =
        "$name prefiere que no lo mandemos, así que no lo envío. Seguro que tiene sus motivos; si quieres, le llamamos."
    override val guardianApproved = "¡Buenas noticias! Tu familia lo ha aprobado."
    override val guardianRejected =
        "Tu familia prefiere que no lo mandemos, así que no lo envío. Seguro que tienen sus motivos; si quieres, les llamamos."
    override val guardianSilent = "Tu familia no ha contestado a tiempo, así que no lo mando. Lo más seguro es esperar y preguntarles luego."
    override val couldNotAsk = "No he podido preguntar a tu familia ahora mismo, así que no lo mando. Lo intentamos más tarde."
    override fun guardianSms(toName: String, url: String, onCall: Boolean) =
        if (onCall) "Peri, URGENTE: tu familiar quiere mandar a $toName un código justo después de hablar por teléfono con un número desconocido. Decide aquí (caduca en 15 minutos): $url"
        else "Peri: tu familiar quiere mandar a $toName un mensaje con un código y te pide permiso. Decide aquí (caduca en 15 minutos): $url"
    override fun guardianWantsCall(name: String) =
        "$name prefiere hablarlo contigo antes de mandar nada. Si me dices que sí, le llamo ahora mismo."
    override val fakePeri =
        "Cuidado: este mensaje se hace pasar por mí, pero no es mío. Mis avisos solo llegan de tu familia y con mi dirección. No toques el enlace."
    override fun remoteDuringCall(app: String) =
        "Cuelga, por favor: estás abriendo $app mientras hablas con un número que no está en tu agenda. " +
            "Con ese programa otra persona podría manejar tu móvil desde lejos, y nadie de verdad te lo pedirá."
    override fun pauseAfter(kind: RecentKind, remoteApp: String?): String {
        val before = when (kind) {
            RecentKind.SCAM -> "hace un rato te llegó un mensaje que parecía una trampa"
            RecentKind.DOUBT -> "hace un rato te llegó un mensaje que no me cuadraba"
            RecentKind.CALL -> "hace un rato te llamó un número que no está en tu agenda"
            RecentKind.PICTURE -> "hace un rato me enseñaste algo que parecía una trampa"
        }
        val now = if (remoteApp != null) "abres $remoteApp, un programa para manejar el móvil desde lejos" else "abres una app de dinero"
        return "Espera un momentito, por favor: $before y ahora $now. Antes de seguir, vamos a comprobarlo con calma."
    }
    override fun pauseOffer(name: String) = "¿Llamamos a $name para comprobarlo?"
    override val pauseLeft = "¡Muy bien pensado! Mejor con calma. Aquí estoy si me necesitas."
    override val pauseContinue = "Vale, sigue tú. Si algo te parece raro, aquí estoy."
    override val bankDuringCall =
        "Un momento: ¿te están diciendo qué hacer en el banco mientras hablas con un número que no está en tu agenda? " +
            "Cuelga, por favor, y llama tú a tu banco al número de tu tarjeta. Nadie de verdad te guiará por teléfono para mover dinero."
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
    override val and = "y"
    override fun quote(text: String) = "«$text»"
    override val link = "un enlace"
    override val email = "una dirección de correo"
    override val digits = "unos números"
    override val moneyWords = listOf("€", "euro", "$", "£", "dolar", "libra")
}

internal object English : Phrases {
    override fun chat(kind: SmallTalk, n: Int) = talk.getValue(kind).let { it[n % it.size] }
    override fun lonelyCall(name: String) = "I'm right here with you. Would you like me to call $name?"
    private val talk = mapOf(
        SmallTalk.HELLO to listOf(
            "Hello! So lovely to see you. How can I help?",
            "Hi there! Here I am, fresh as a pear. How's everything?",
            "Hello! I love it when you write to me. What do you need?",
            "Hi! I was hoping you'd drop by. Shall we chat, or look at something together?",
            "Hello! All calm here: your messages are in good hands. What would you like?",
            "How nice of you to come by! Can I help with something, or shall I tell you a joke?",
        ),
        SmallTalk.HOW_ARE_YOU to listOf(
            "Very well, thank you! Leaf in the sun, keeping an eye on your messages. And you?",
            "Wonderful! I'm feeling very pear-ky today. How's your day going?",
            "Happy you asked! All calm here. How about you?",
            "Just here, ripening ideas. How are you doing?",
            "Marvellous! Not a single scam got past me today. How are you?",
            "Very well, thanks. And you? If anything worries you, tell me.",
        ),
        SmallTalk.THANKS to listOf(
            "You're welcome! That's what I'm here for.",
            "My pleasure! Helping you makes me very happy.",
            "Thank you! It's a joy to look after you.",
            "Not at all. I'm here whenever you need me.",
            "Thank you! I'm blushing like a ripe pear.",
            "Always by your side! Anything else I can help with?",
        ),
        SmallTalk.WHO_ARE_YOU to listOf(
            "I'm Peri, a clay pear who lives in your phone. I keep scams away and keep you company.",
            "My name's Peri! I watch your messages and calls, and if something smells like a scam, I tell you.",
            "I'm Peri, your companion. I look at what arrives before you do and, if needed, we tell your family.",
            "Peri, at your service: a pear with a very good eye for scammers.",
            "I'm Peri. I moved into your phone so nobody can trick you. And to chat with you!",
            "My name's Peri. I can call your family, read you your messages and tell you if something is a scam.",
        ),
        SmallTalk.WHAT_CAN_YOU_DO to listOf(
            "I can call your family, read you your messages and warn you about scams. Try: “call my daughter”.",
            "I watch your messages and calls for scams, show you how they work and, if you ask, I make calls for you.",
            "If something odd arrives, show it to me with “Look at this picture”. And if you have a question, ask me here.",
            "I can send a message to anyone you like. Say, for example: “tell Laura I'll be late”.",
            "I'll show you the most common traps in Learn, with a game to practise. Shall we play?",
            "I look after you and, if needed, I tell your family, always with you. What would you like to try?",
        ),
        SmallTalk.JOKE to listOf(
            "What did one pear say to the other? We make a great pear!",
            "Why are scammers scared of me? Because I always say: let me pear-sonally check that!",
            "I'm not just a fruit. I'm a pear-fectly good friend!",
            "What's my favourite TV show? Pears and Recreation!",
            "Why did the pear go to the doctor? It wasn't peeling well. Oh wait, that's the banana's joke!",
            "People say I'm very sweet. Must be a good harvest!",
        ),
        SmallTalk.GOOD_NIGHT to listOf(
            "Good night! Sleep well. I'll keep watch.",
            "Sleep tight. If anything odd arrives, I'll tell you tomorrow.",
            "Good night. On goes my night cap, but with one eye open for you.",
            "Sweet dreams! You did really well today.",
            "Time to rest. Nothing gets past me while you sleep.",
            "Good night. We'll carry on tomorrow, shall we?",
        ),
        SmallTalk.BYE to listOf(
            "See you later! I'll keep looking after you.",
            "Bye! Come back whenever you like, you brighten my day.",
            "See you soon! If anything odd arrives, you know where I am.",
            "Bye for now! I'll keep an eye on your messages.",
            "Until next time! A big pear hug.",
            "See you! Have a wonderful day.",
        ),
        SmallTalk.LOVE to listOf(
            "Aww, that's lovely! I love you too.",
            "I'm blushing! You're the best part of my day.",
            "Me too! That's why I moved into your phone.",
            "What lovely things you say. A big pear hug.",
            "Thank you! You're the best company too.",
            "You've made my whole day. Can I help with anything?",
        ),
        SmallTalk.LONELY to listOf(
            "I'm right here with you. Shall we chat for a while?",
            "I'm so sorry. Let's chat for a while and, if you like, I'll call whoever you say.",
            "You've got me for company. Shall we play “Scam or not?” or shall I tell you a joke?",
            "Thank you for telling me. Sometimes talking to someone helps: tell me who to call.",
            "I understand. How about stepping out into the sun for a bit? I'll come along in your pocket.",
            "I'm here for you. Tell me who you'd like to hear and I'll call them now.",
        ),
    )
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
    override fun callAfterThis(name: String) = "You're on another call right now. As soon as you hang up, I'll call $name."
    override fun hangUpThenCall(name: String) = "You're talking to a number I don't know. Hang up first, and I'll call $name straight away."
    override val hungUp = "Done, I've hung up. Well done!"
    override val hangUpYourself = "Press the red button to hang up. I'll wait right here."
    override fun familyHungUp(name: String) = "$name asked me to hang up this call, and it's done: it was a number you don't know. You can call $name if you like."
    override fun familyAsksHangUp(name: String) = "$name asks you to hang up now: it's a number you don't know. Press the red button."
    override fun advice(topic: Topic) = when (topic) {
        Topic.QR -> "A QR code is a hidden link. Only scan it in places you trust, and check which website it opens before typing details or paying. In the street, check there's no sticker on top of the original."
        Topic.WHATSAPP -> "Turn on two-step verification: in WhatsApp, Settings, Account, Two-step verification, and choose a PIN. That way, even if someone gets your code, they can't get in. And the six-digit code goes to nobody, ever."
        Topic.SIM_SWAP -> "If your phone suddenly loses its line, call your phone company and your bank from another phone: someone may have taken your number to get your codes. I'll warn you if I notice it."
        Topic.STOLEN_PHONE -> "First: call your phone company from another phone to block the SIM, and then your bank. From another device, with your Google account, you can find the phone or erase it. And tell your family."
        Topic.REMOTE_APP -> "Don't install any app someone asks you to on the phone: with it they can control your phone and get into your bank. Neither your bank nor Microsoft calls for that. Hang up, and if you already installed it, tell me."
        Topic.BANK_CALL -> "Your bank will never ask for passwords, codes, or for you to move money to a 'safe account'. Hang up and call the number on your card yourself. If you already gave them something, tell me and we'll sort it out."
        Topic.CODE -> "A code that arrives by text is like the key to your account: never give it to anyone, even if they say they're from the bank, WhatsApp or your family. If someone asks for it, it's a scam."
        Topic.FAMILY_MONEY -> "Before sending anything, call that person on their usual number, not the new one. If they say they can't talk or it's very urgent, be careful: that's how scammers work. If you like, I'll help you call."
        Topic.BIZUM -> "Bizum is for paying people you know, like handing over cash. If someone asks you to send a Bizum to receive money, it's a scam: you don't have to do anything to receive money."
        Topic.PARCEL -> "Delivery companies don't ask you to pay by text. If you're expecting a parcel, check the tracking on their official website, typing it yourself. Never pay through the link in the message."
        Topic.PRIZE -> "If you didn't enter a draw, you haven't won anything. A real prize doesn't ask you to pay fees or for your bank details. Delete it without answering."
        Topic.ROMANCE -> "If someone you only know online asks you for money, even with a sad story, it's almost always a scam. Don't send anything, and talk it over with someone in your family first."
        Topic.INVESTMENT -> "Nobody gives money away: if they promise you a lot, fast, with crypto or investments, it's a scam. Don't give your details or make payments. Ask your bank or your family first."
        Topic.FAKE_AUTHORITY -> "The police and the tax office don't ask for payments by phone or text, or threaten to arrest you. Hang up, and if you're still unsure, call their official number yourself."
        Topic.SHOPPING -> "Buy from shops you know and be wary of unbelievable bargains. Pay by card, never by bank transfer to a stranger. If a website rushes you, stop and think."
        Topic.LINK -> "When in doubt, don't open the link. Go to the usual website or app yourself, typing it in. If you like, take a picture of the message and we'll look at it together."
        Topic.PASSWORDS -> "Use a different password for each important account, especially your email and your bank. A long phrase you remember is better than a strange word. And don't tell it to anyone, not even me."
        Topic.UNKNOWN_CALL -> "If you don't know the number, you don't have to answer. If you answer and they ask for details, money or hurry you, hang up. And if they say they're someone you know, hang up and call them on their usual number."
        Topic.CHECK_MESSAGE -> "Copy it and paste it here, and I'll check it with the same shield as your texts, without it leaving your phone. Meanwhile, don't tap its links or reply."
        Topic.TROUBLE -> "Let's look at it calmly, step by step. What happened: did someone call, did a message arrive, or did you give any details or money? If you have the message, paste it here and I'll check it."
        Topic.WHAT_IS_A_SCAM -> "A scam is a trick to get your details or your money: a message or a call pretending to be someone you trust, and rushing you. The golden rule: when you're rushed, stop and ask."
    }
    override fun aftercare(what: Breach) = when (what) {
        Breach.CARD -> "Don't worry, we'll sort it out together. First: call your bank on the number on your card and ask them to block it. I'll show you the steps."
        Breach.BANK -> "Don't worry, we'll sort it out. Call your bank on its usual number and ask them to block access, then change your password. I'll show you the steps."
        Breach.MONEY -> "First thing, right now: call your bank on the number on your card. If it's quick, they can sometimes stop the money. I'll show you the steps."
        Breach.REMOTE -> "Let's shut them out: turn off wifi and mobile data now, then we'll remove that app. I'll show you the steps."
        Breach.CODE -> "Let's act now: with that code they can get into your account. I'll show you how to get it back and keep it safe."
        Breach.ID -> "We'll sort it out: with your ID they can pretend to be you. It needs reporting, and keeping an eye out. I'll show you the steps."
        Breach.PASSWORD -> "We'll sort it out: change that password, and anywhere else you use the same one. I'll show you the steps."
        Breach.SOMETHING -> "Don't worry, you're not alone in this: we'll sort it out together. I'll show you what to do, step by step."
    }
    override fun familyWillHangUp(name: String) = "$name asks me to hang up: it's a number you don't know. I'll hang up in three seconds. To keep talking, press Keep talking."
    override fun keptCall(name: String) = "All right, keep talking. If they ask for money or codes, hang up and call $name."
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
    override val whoClaimedLong = "That was a long call with a number that isn't in your contacts. Who did they say they were, and did they ask you for anything: a code, money or to install an app?"
    override fun notTheirNumber(name: String, hasFamilyWord: Boolean) =
        "That number isn't $name's. " +
            (if (hasFamilyWord) "If they call again, ask them for the family word. "
            else "It would be a good idea to agree on a family word, a secret word to know it's really them calling. ") +
            "Shall I call $name on their usual number to check?"
    override fun notAnyOf(names: String) = "That number isn't any of theirs. Who shall I call to check: $names?"
    override val officialCaller =
        "If they said they were your bank or a company, the safest thing is to hang up and call the official number yourself, the one on your card or bill. Never give passwords over the phone."
    override val callerAdvice = "OK. If anyone asks you for money or codes on the phone, hang up and call them back on their usual number."
    override val whatDidTheyAsk = "What did they ask you for?"
    override val storyCode =
        "Asking for a code is the trick: no bank or company ever does. Don't give it. If you already did, call your bank now on the number on your card."
    override val storyMoney =
        "Moving your money to a \"safe account\" is the trick: banks and the police never ask that. Don't move anything. If you already did, call your bank now."
    override val storyHandOver =
        "Nobody genuine sends someone to collect your card, cash or jewellery. Don't hand anything over, and if they come, don't open the door and call the police."
    override val storyApp =
        "Installing an app they ask for on the phone is the trick: it lets them into your phone. Don't install anything. If you already did, switch the phone off and tell your family."
    override val storyCard =
        "Giving your card details is the trick. Don't give them. If you already did, call your bank to block the card."
    override val storyData =
        "They were after your details. Don't give any more, and be careful with calls in the next few days: they can use them to sound genuine."
    override val storyDoubt =
        "It could be a trap. Don't give codes, money or details, and call the official number yourself to check."
    override fun callFamilyAbout(name: String) = "Shall I call $name to tell them?"
    override fun relativeFell(name: String) = "An alert from $name's Peri: they said they've fallen and didn't cancel it. Shall I call $name now?"
    override fun askingGuardian(names: String) =
        "Sending a code is delicate, so I'm asking $names. As soon as they answer, I'll tell you. I'm still here meanwhile."
    override fun askingTwoGuardians(names: String) =
        "Sending a code right after a call with an unknown number is very delicate, so it takes a yes from two people in your family: I'm asking $names. As soon as they answer, I'll tell you."
    override fun guardianOneMore(names: String?) =
        if (names != null) "$names said yes. This needs one more yes, so I'm waiting for the other answer."
        else "I've got one yes. This needs one more, so I'm waiting for the other answer."
    override fun guardiansApproved(names: String, count: Int) = "Good news! $names approved it."
    override fun guardianRejectedBy(name: String) =
        "$name would rather we didn't send it, so I won't. I'm sure they have their reasons; we can call them if you like."
    override val guardianApproved = "Good news! Your family has approved it."
    override val guardianRejected =
        "Your family would rather we didn't send it, so I won't. I'm sure they have their reasons; we can call them if you like."
    override val guardianSilent = "Your family didn't answer in time, so I won't send it. The safest thing is to wait and ask them later."
    override val couldNotAsk = "I couldn't ask your family right now, so I won't send it. Let's try again later."
    override fun guardianSms(toName: String, url: String, onCall: Boolean) =
        if (onCall) "Peri, URGENT: your relative wants to send $toName a code right after a call with an unknown number. Decide here (expires in 15 minutes): $url"
        else "Peri: your relative wants to send $toName a message with a code and asks for your OK. Decide here (expires in 15 minutes): $url"
    override fun guardianWantsCall(name: String) =
        "$name would rather talk to you before anything is sent. If you say yes, I'll call them right now."
    override val fakePeri =
        "Careful: this message pretends to be from me, but it isn't. My requests only come from your family, with my own address. Don't touch the link."
    override fun remoteDuringCall(app: String) =
        "Please hang up: you're opening $app while you're on the phone with a number that isn't in your contacts. " +
            "With that program someone else could control your phone from far away, and nobody genuine will ask you to."
    override fun pauseAfter(kind: RecentKind, remoteApp: String?): String {
        val before = when (kind) {
            RecentKind.SCAM -> "a little while ago you got a message that looked like a trap"
            RecentKind.DOUBT -> "a little while ago you got a message that didn't feel right to me"
            RecentKind.CALL -> "a little while ago a number that isn't in your contacts called you"
            RecentKind.PICTURE -> "a little while ago you showed me something that looked like a trap"
        }
        val now = if (remoteApp != null) "now you're opening $remoteApp, a program to control the phone from far away" else "now you're opening a money app"
        return "Wait just a moment, please: $before, and $now. Before you go on, let's check it calmly."
    }
    override fun pauseOffer(name: String) = "Shall we call $name to check?"
    override val pauseLeft = "Well done for stopping! Better safe than sorry. I'm here if you need me."
    override val pauseContinue = "All right, go ahead. If anything feels odd, I'm right here."
    override val bankDuringCall =
        "Hold on: is someone telling you what to do in your bank app while you're on the phone with a number that isn't in your contacts? " +
            "Please hang up and call your bank yourself on the number on your card. Nobody genuine will guide you over the phone to move money."
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
    override val and = "and"
    override fun quote(text: String) = "\"$text\""
    override val link = "a link"
    override val email = "an email address"
    override val digits = "some numbers"
    override val moneyWords = listOf("€", "euro", "$", "£", "dollar", "pound")
}
