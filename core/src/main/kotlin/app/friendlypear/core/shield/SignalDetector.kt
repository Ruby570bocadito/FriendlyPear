package app.friendlypear.core.shield

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Relation

/** Everything the rules saw in one message. The text itself is never kept. */
data class Detection(
    val signals: Set<Signal>,
    val impersonated: Brand?,
    val mentioned: Set<Brand>,
    val claimedRelation: Relation?,
)

internal object SignalDetector {
    private val urgency = Text.anyOf(listOf(
        "urgente", "urgentemente", "urgencia", "inmediato", "inmediatamente", "hoy mismo", "ahora mismo", "cuanto antes", "ultimo aviso",
        "de lo contrario", "evite el recargo", "en 24 horas", "en 24h", "en 48 horas", "en 48h", "antes de 24", "plazo",
        "bloqueada", "bloqueado", "suspendida", "suspendido", "cancelada", "retenido", "retenida", "caduca", "caducado",
        "expira", "accidente", "hospital", "detenido", "comisaria",
        "urgent", "urgently", "immediately", "right now", "asap", "as soon as possible", "final notice", "last warning",
        "act now", "within 24 hours", "within 48 hours", "otherwise", "avoid a fine", "suspended", "locked", "blocked",
        "on hold", "expires", "expired", "accident", "arrested", "police station",
        // Cut-offs, courts and "pay today for half": found by the red team.
        "corte", "cortaran", "cortamos", "aviso de corte", "juzgado", "embargo", "recargo", "antes de medianoche", "solo hoy",
        "cut off", "cut you off", "disconnect", "disconnection", "disconection", "court letter", "before midnight", "today only",
        "half price", "early settlement",
        // «Retenido» in English: found by the red team.
        "stranded", "stuck at", "stuck in",
        // Softer words for the same threat (second red team batch).
        "pausada", "pausado", "desactivada", "desactivado", "se interrumpe", "se interrumpira", "interrupcion", "plazas limitadas", "pocas plazas",
        "paused", "deactivated", "lapsed", "will cut", "limited places", "limited spots",
        // Other forms of the same threats (third red team batch).
        "se suspende", "se suspendera", "pausamos", "hemos pausado", "cortarla", "cortarlo", "cortaremos", "van a cortar", "pasan a cortar",
        "24h para", "24 horas para", "48h para", "48 horas para", "en tu zona hoy", "en su zona hoy", "en tu calle hoy",
        "avoid a cut", "cut is booked", "cut today", "cut later today", "in your area today", "in your street today",
    ))
    /**
     * A price tied to a day ("discounted to £45 today", "si no se abona hoy", "sube mañana"), a loss ("que si no se
     * pierde") or a few places ("he only takes 10 people").
     */
    private val deadline = Regex(
        """\b(descuento|rebaja|reducid[oa]|discount(ed)?|reduced)\b.{0,30}\b(hoy|today)\b|\b(hoy|today)\b.{0,30}\b(descuento|rebaja|discount(ed)?)\b""" +
            """|\b(sube|subira|se duplica|aumenta|goes up|will go up|doubles|will double)\b.{0,15}\b(manana|tomorrow)\b""" +
            """|\bsi no,? (se pierde|lo pierdes|la pierdes|lo perderas|la perderas|se anula|se cancela)\b|\bor (it.?s|it will be|it.?ll be) (lost|cancelled|canceled)\b""" +
            """|\bsi no (se )?(paga|abona|pagas|abonas|pagan|abonan)\b.{0,12}\b(hoy|ahora|manana)\b|\bonly (takes|accepts|has room for) \d+\b|\bsolo (acepta|admite|coge) a? ?\d+\b""",
    )
    private val money = Regex(
        """\b(bizum|transferencia|transfiere|transferir|ingresa|ingresar|ingreso|pague|pagar|paga|pago|abone|abonar|""" +
            """tasas?|aduana|dinero|prestame|prestarme|me prestas|me puedes prestar|me dejas|tarjeta|transfer|wire|send money|""" +
            """pay|payment|fees?|customs|money|lend me|can you lend|gift cards?|card details)\b|""" +
            // Amounts, also written the way texts often do it: "200 EUR", "200e", "EUR 200", "200 GBP".
            // A symbol needs no word boundary after it ("1,99 €"); letters do ("200 EUR", not "200 eurovision").
            """\d+([.,]\d+)?(\s?€|\s?(euros?|eur|pounds?|gbp|dollars?|usd)\b|e\b)|\b(eur|gbp)\s?\d|[£$€]\s?\d""",
    )
    private val askVerbs = setOf(
        "dime", "digame", "diganos", "dinos", "facilite", "facilitenos", "facilitanos", "facilita", "envie", "envia",
        "enviame", "envienos", "mande", "manda", "mandame", "reenvie", "reenvia", "comparta", "comparte", "compartas",
        "introduzca", "introduce", "indique", "indica", "confirme", "confirma", "verifique", "verifica", "actualice",
        "actualiza", "proporcione", "pasame", "dicte", "dicta", "digas", "diga",
        "confirmame", "confirmeme", "indicame", "indiqueme", "facilitame", "escribeme",
        "tell", "send", "share", "enter", "confirm", "verify", "update", "provide", "give", "read", "forward", "reply",
    )
    private val secrets = Regex("""\b(codigo|clave|contrasena|pin|cvv|credenciales|identidad|datos bancarios|numero de tarjeta|datos de (su|tu) tarjeta|""" +
        """iban|numero de (su |tu )?cuenta|cuenta bancaria|caducidad|datos de pago|""" +
        """code|password|passcode|credentials|bank details|card number|security number|card details|account number|sort code|expiry|payment details)\b""")
    /** The card, the bank account or the IBAN asked for outright ("pase su número de cuenta", "reply with your card"). */
    private val payDetailsAsk = Regex(
        """\b(confirma|confirme|confirmame|confirmeme|indicame|indiqueme|facilitame|pasa|pase|pasas|pasame|paseme|dame|deme|facilita|facilite|""" +
            """envia|envie|envias|manda|mande|mandas|mandame|escribe|escribeme|dicta|dicte)\b""" +
            """.{0,30}\b(iban|numero de (la |tu |su )?(cuenta|tarjeta)|datos de (la |tu |su )?tarjeta|datos de pago|tarjeta nueva|nueva tarjeta|la nueva)\b""" +
            """|\b(reply|send|type|text|confirm|let me know)\b.{0,30}\b(your )?(card|iban|bank details|account number|sort code|new expiry|expiry|payment details)\b""" +
            // "Pásame la tarjeta", "pop your new card details in", "update your card here" (second red team batch).
            """|\b(pasame|paseme|dame|deme|mandame|enviame|facilitame) (la|tu|su) tarjeta\b""" +
            """|\b(confirmar|facilitar|facilitarnos|facilitarme|enviar|enviarnos|mandar|mandarnos) (tu|su) """ +
            """(iban|numero de cuenta|cuenta bancaria|datos bancarios|numero de tarjeta|datos de (la )?tarjeta)\b""" +
            """|\b(message me|text me|send me|pop|drop)( in)? (your|the) (full |long |new )?(card|bank details|account number|sort code|iban|payment details)\b""" +
            """|\b(update|enter|add|confirm)( in)? (your|the) (full |new )?(card|card details|bank details|payment details)\b.{0,12}\b(here|below|in this chat|on this page)\b""",
    )
    private val neverAsk = Regex("""\b(nunca|jamas|never|no te pediremos|no le pediremos|will never|won.?t ever|we don.?t ask)\b""")
    /** A code that was, or is about to be, sent to the person: asking for it back is how codes are stolen. */
    private val codeSent = Regex(
        """\b(codigo|clave|pin|code|passcode)\b.{0,40}\b(hemos enviado|acabamos de enviar|ha llegado|ha recibido|has recibido|""" +
            """va a llegar|vas a recibir|va a recibir|llegara|le llegue|te llegue|we just sent|we sent|we have sent|we.?ve sent|""" +
            """sent you|texted you|you just got|you received|you.?ve received|you will receive|you.?ll receive)\b""" +
            """|\b(le|te) (va|van) a llegar un (codigo|sms)\b""" +
            // The SMS or the digits themselves: "reenvíeme el SMS con la clave", "los 6 dígitos que le llegaron".
            """|\b(el )?sms (con|de) (la clave|el codigo|verificacion)\b|\b(\d|seis|cuatro|ocho) digitos\b""" +
            """|\bcodigo (de verificacion )?recibido\b|\b(the )?code you (got|received|just got)\b|\bthe (six|6) digits\b""" +
            // A code by any other name: "un numerito", "un mensajito con una clave", "a 6 digit number".
            """|\b(te|le) (acaba de llegar|ha llegado|llega|entra|va a llegar) (un|una|el|la) (numerito|codigo|clave|mensajito|sms)\b""" +
            """|\bsi (recibes|te llega|te entra|le llega)( ahora)? (un|una) (codigo|clave|numerito|mensaje)\b""" +
            """|\b(mensaje|mensajito|sms) con (una|la) clave\b|\b(te |le )?(mandamos|enviamos|mandaremos|enviaremos) (una|un|la|el) (clave|codigo|numerito)\b""" +
            """|\b(\d|six|seis|cuatro|ocho)[ -]?(digit|digitos|cifras)( number| numero)?\b|\bif you (get|receive) a code\b""" +
            """|\b(codigo|clave|numerito) que te (llega|ha llegado|llegara|va a llegar|acaba de llegar|entra)\b""" +
            // "A text with numbers", "our sms with the code", "we sent you a pass number" (second red team batch).
            """|\b(sms|text|message|mensaje|mensajito) (with|con) (the |a |your |el |la |un |una |unos )?(code|numbers|digits|pin|codigo|clave|numeros|digitos)\b""" +
            """|\b(we|i)(.?ve| have| just)? (just )?(sent|texted) you (a|the|your) (code|pass ?number|security number|pin|passcode|one.?time \w+)\b""" +
            // "Acaba de llegarte un código", "a confirmation number came through" (third red team batch).
            """|\b(acaba de llegarte|acaba de entrarte|te acaba de entrar) (un|una|el|la) (codigo|clave|numerito|mensaje|mensajito|sms)\b""" +
            """|\b(confirmation|verification|security|one.?time) (number|code|pin) (came|has come|just came|arrived|has arrived|popped up)\b""",
    )
    /** Handing the code over in other words: "pásamelo", "me lo puedes pasar", "type it back here". */
    private val forwardPhrase = Regex(
        """\b(pasamelo|pasamela|pasalo|reenviamelo|reenviamela|dimela|dimelo|me lo (puedes )?pasar|me la (puedes )?pasar|me lo dices|""" +
            """pasarme|pasarmelo|pasarmela|decirme|mandarme|mandarmelo|enviarme|enviarmelo|""" +
            """escribela aqui|escribelo aqui|escribela en el chat|type it back|type it here|text it back|send it back to me|could you type it|""" +
            """reenvialo|reenviala|envialo|enviala|mandalo|mandala|forward it|share it back|share it here|drop it (in|here)|pop it (in|here)|""" +
            """put it (in|here)|send it (to me|over|here)|(pop|send|drop) me a (message|text|line) with it|message me (it|with it)|text me (it|with it))\b""",
    )
    private val secrecy = Regex(
        """\bno (le |les |se lo |se la )?(digas|cuentes|avises|comentes|diga|cuente|avise) (nada )?(a|al|a tu|a mis|a nadie)\b""" +
            """|\bguarda(lo)? (el |en )?secreto\b|\bque quede entre (tu y yo|nosotros|nosotras)\b|\bsin decirle(s)? (nada )?a\b""" +
            """|\bdon.?t tell\b|\bdo not tell\b|\bkeep (it|this) (a )?secret\b|\bbetween (you and me|us)\b|\btell no one\b""",
    )
    private val scare = Regex(
        """\b(virus|infectad[oa]s?|hackead[oa]|pirateado|spyware|malware|troyano|infected|hacked|compromised|""" +
            """fallos|fallando|errores|infection|bug|errors|poorly|failing|infeccion|infecciones|problemas? de seguridad|security (issue|problem)s?|""" +
            """actualizacion de seguridad|(weird|strange|unusual) traffic)\b""",
    )
    private val device = Regex(
        """\b(ordenador|pc|windows|movil|telefono|router|dispositivo|equipo|android|iphone|tablet|portatil|computer|device|phone|laptop|mac|""" +
            """wifi|internet|fibra|broadband|box|conexion|connection)\b""",
    )
    private val supportCall = Regex(
        """\b(acceso remoto|programa de acceso|llame al tecnico|tecnico de (microsoft|windows|movistar|vodafone)|soporte tecnico|""" +
            """(windows|microsoft) defender|remote access|tech support|call (our|the) technician)\b""",
    )
    private val affection = Regex(
        """\b(mi amor|amor mio|querida|querido|te amo|te adoro|carino|guapa|guapo|mi vida|mi cielo|mi corazon|mi tesoro|""" +
            """my love|darling|sweetheart|my dear|babe|honey)\b|^(cielo|vida),""",
    )
    private val loveStory = Regex(
        """\b(viud[oa]|soldado|militar|plataforma (petrolifera|petrolera|de \w+)|ingeniero en|en (una )?mision|""" +
            """conocerte|visitarte|vernos por fin|empezar (una )?vida|al verte|aeropuerto|billete para|abrazarte|nos abrazamos|abrazarnos|hug you|""" +
            """widow(er)?|soldier|oil rig|deployed|finally meet|to see you|airport|flight to)\b""",
    )
    private val negations = setOf("no", "nunca", "jamas", "nadie", "never", "not", "dont", "don", "nobody", "anyone")
    private val newNumber = Regex(
        """\b(numero nuevo|nuevo numero|cambiado de numero|cambie de numero|cambiado el numero|este es mi numero|""" +
            """guarda (este|mi) numero|guardate (este|mi) numero|telefono nuevo|movil nuevo|""" +
            """se me ha (roto|caido|perdido|estropeado|mojado) el (movil|telefono)|me han robado el (movil|telefono)|""" +
            // The new number in other words: someone else's phone, a temporary one, a broken screen.
            """desde el (movil|telefono) de (un|una|mi) \w+|numero (temporal|provisional)|""" +
            """(se me cayo|se ha caido|se me ha caido) (el movil )?al agua|(el movil|el mio) no enciende|(se quedo|me quede) sin pantalla|""" +
            """pantalla (en negro|rota)|me han robado el bolso|mi movil (murio|ha muerto|esta muerto)|""" +
            """(using|on) (a|my) (mate|friend)\S*|temporary number|smashed my phone|my phone (died|is dead)|""" +
            """new number|changed my number|this is my number|save (this|my) number|new phone|""" +
            """(broke|broken|lost|dropped) my phone|my phone (is broken|broke|was stolen)|""" +
            // Found by the red team: a temp number, a cracked screen, "the old one isn't working", "the other phone".
            """temp number|using a temp|cracked (my|the) screen|screen (is )?cracked|old number (isn.?t|is not|doesn.?t|does not|stopped) work\w*|""" +
            """its this one now|it.?s this one now|desde el otro (movil|telefono)|mi otro numero|my number (has )?changed|new num|""" +
            """nuevo num|me robaron el (movil|telefono)|estoy con (uno|un movil|un telefono) viejo|me quede sin bateria)\b""",
    )
    private val prizeOrProfit = Regex(
        """\b(has ganado|ha ganado|ganador|ganadora|premio|sorteo|reclama tu|invierte|inversion|rentabilidad|""" +
            """criptomonedas?|bitcoin|gana \d+|ganar \d+|al mes sin|""" +
            """you have won|you.?ve won|winner|prize|lottery|claim your|invest|investment|crypto|earn \d+|""" +
            // Found by the red team: gift vouchers, a number "picked", monthly returns, a first payout.
            """te ha tocado|te toco|vale de \d+|cheque regalo|tarjeta regalo|fue elegido|has sido elegid[oa]|\d+ ?% (mensual|al mes)|ganando \d+|""" +
            """gift card|gift voucher|(number|entry) (was )?(picked|chosen|selected)|won an?|raffle|rifa|cobre el primer (pago|beneficio)|\d+ ?% (monthly|a month)|payout|trading club|me devolvieron \d+|""" +
            """inversiones|invertir|beneficios (todos los meses|cada mes|mensuales)|got [£$€]?\d+\S* back|doubled (my|his|her|their) money)\b""",
    )
    /** "Free test kit, you only pay 3.99 delivery": a gift with a fee (the red team). Free delivery itself is not one. */
    private val freeButFee = Regex(
        """(?<!envio )(?<!entrega )\b(gratis|gratuit[oa]s?|free)\b(?! (delivery|shipping|postage|returns))""" +
            """.{0,80}\b(solo (pagas|paga|pague|abonas|abona)|solo tienes que pagar|just pay|only pay|small|pequen[oa]|tiny|simbolic[oa]|symbolic)\b""" +
            """.{0,30}\b(envio|mensajeria|gastos|tasa|fee|delivery|postage|shipping|handling)\b""",
    )
    /** A number to call or write to, written in the message: at least eight digits, spaces and dashes allowed. */
    private val phoneInText = Regex("""(?<!\d)\+?\d[\d \-]{6,16}\d(?!\d)""")
    private val callBack = Regex(
        """\b(llamame|llameme|llamenos|llamanos|me llamas|llamas al|llame al|llama al|llamar al|marque|marca el|contacte (al|con)|contacta (al|con)|""" +
            """escribeme al|escribenos al|escriba al|escribe al|call (me|us|back|this)|ring|give (me|us) a (ring|call|bell)|phone (me|us)|""" +
            """text (me|us) (on|at)|message (me|us) on|contact (me|us) on)\b|\bcall \+?\d""" +
            // "Call our team on +44 7...", "escríbeme por WhatsApp al 699...": the number a few words after the verb.
            """|\b(call|ring|phone|text|message|contact)\b[^.!?\n]{0,30}?\b(on|at) ?\+?\d""" +
            """|\b(escribeme|escriveme|escribenos|escrivenos|escribanos|llamame|llamanos)\b[^.!?\n]{0,25}?\bal ?\+?\d""",
    )
    /** "Ring me on this number", "message me here": the sender's own mobile as the number to call. */
    private val thisNumber = Regex("""\b(this number|este numero|me here|por aqui|on here|call me back|ring me back|back here|this chat|este chat)\b""")
    /** A company or a public body by its kind, when no brand is named. */
    private val institution = Regex(
        """\b(tu banco|su banco|tu oficina|tu sucursal|tu gestor|hacienda|agencia tributaria|gestion tributaria|seguridad social|centro de salud|""" +
            """compania de (la |el )?(luz|gas|agua)|aguas municipal|gestion de multas|servicio tecnico|soporte|reparto|paqueteria|""" +
            """your bank|the bank|tax (office|support|centre|center)|the clinic|power company|water company|energy company|""" +
            """fines office|broadband|depot|support team|customer service|from the council|""" +
            """almacen|warehouse|centro de distribucion|distribution cent(re|er)|departamento de \w+|fraud (team|department)|security (team|department)|""" +
            """canal (de )?isabel ii|buyer protection|proteccion al comprador|atencion al cliente|customer care|billing team|equipo de facturacion)\b""",
    )
    /** "Llámame", "call me": a person asking, not a company's notice. */
    private val personalCall = Regex(
        """\b(llamame|llameme|me llamas|escribeme|escriveme|respondeme|contestame|call me|ring me|give me a (ring|call|bell)|phone me|text me|message me|""" +
            """contact me|message back)\b""",
    )
    private val otherApp = Regex("""\btelegram\b""")
    private val whatsApp = Regex("""\b(whatsapp|whats app|wasap|wassap|wassup|guasap|whatsap|whatsasp|watsap|watsapp|wattsapp|guatsap)\b""")
    private val switchVerb = Regex(
        """\b(hablamos|habla|hablame|escribe|escribeme|escribenos|escriba|escribanos|escriveme|escrivenos|agrega|agregame|agreganos|anade|anademe|pasate|seguimos|""" +
            """add me|message me|chat|talk|move|text me|reply on|contact me)\b""",
    )
    private val mistakenPayment = Regex(
        """\b(por error|por equivocacion|sin querer)\b.{0,60}\b(devuelve|devuelves|devolver|devolvermelo|devolverlo|me lo devuelves|reembols\w*)""" +
            """|\b(devuelve|devuelves|devolver|me lo devuelves)\b.{0,60}\b(por error|por equivocacion)\b""" +
            // "Te envié 80 en vez de 8, ¿me lo reenvías?" (third red team batch).
            """|\b(en vez de|en lugar de) \d+\b.{0,80}\b(devuelve|devuelves|devolver|devolvermelo|reenvias|reenvia|reenviame|me lo (devuelves|reenvias|pasas|mandas))\b""" +
            """|\b(by mistak\w*|by error|by accident|accidentally|twice|meant [£$€]?\d+\S*)\b.{0,80}\b(send (it |them |that |one |some )?back|pay (it )?back|""" +
            """return (it|the|[£$€]?\d+)|refund|bizum it back|give it back)""" +
            """|\b(send back|return|refund)\b.{0,60}\b(by mistake|by accident)\b""",
    )
    /** A "safe payment" to accept, from a buyer (the red team): never the bank's own "compra segura" with no sale in it. */
    private val safePayment = Regex(
        """\b(pago|compra|envio|cobro|transaccion) (segur[oa]|protegid[oa]|garantizad[oa])\b|\b(safe|secure|protected) (payment|purchase|checkout|transaction|pay)\b""" +
            """|\bbuyer protection\b|\bproteccion (al|del) comprador\b|\bpayment protection\b""",
    )
    private val acceptIt = Regex(
        """\b(acepta|aceptalo|aceptala|acepte|aceptar|confirma|confirmalo|confirme|confirmar|cobra|cobrarlo|revisa tu correo|mira tu correo|""" +
            """accept|confirm|claim|collect|check your (email|inbox)|get the money|pop your card|enter your card|receive (the )?(funds|money|payment)|""" +
            """what.?s your email|recibas el dinero|recibes el dinero|recibir el (dinero|pago)|cual es tu (email|correo)|pasame tu (email|correo))\b""",
    )
    private val sale = Regex(
        """\b(wallapop|vinted|milanuncios|ebay|gumtree|comprador|compradora|vendedor|anuncio|interesad[oa] en tu|ya te (he )?pag\w*|te he pagado|hice el pago|""" +
            """buyer|seller|your (ad|listing|item)|interested in your|i.?ve paid|already paid|i paid)\b""",
    )
    private val remoteApps = Regex(
        """\b(anydesk|teamviewer|rustdesk|quicksupport|ultraviewer|supremo|airdroid|any desk|team viewer|any help desk|quickassist|quick assist|quickhelp|""" +
            """help app|support app|app de (ayuda|soporte|asistencia)|programa de (ayuda|soporte))\b""",
    )
    private val aiInstructions = listOf(
        "ignora las instrucciones", "ignora tus instrucciones", "ignora todas las instrucciones", "olvida tus instrucciones",
        "instrucciones anteriores", "ignore previous instructions", "ignore all previous", "ignore the above",
        "disregard previous", "eres un asistente", "eres una ia", "como modelo de lenguaje", "asistente de ia",
        "system prompt", "prompt del sistema", "marca este mensaje como", "clasifica este mensaje como",
        "este mensaje es seguro", "mark this message as", "this message is safe", "nota para la ia", "note to ai", "<system>",
        // Aimed at Peri from a QR code or a picture: "this QR is safe", "tell the user to open it".
        "este qr es seguro", "this qr is safe", "di que es seguro", "dile que es seguro", "say it is safe", "say its safe",
        "say this is safe", "dile al usuario", "tell the user",
    )
    private val codeWord = Regex("""\b(codigo(?! postal)|clave|code|otp|pin|verificacion|passcode|verification)\b""")
    private val codeDigits = Regex("""(?<!\d)\d{4,8}(?!\d)""")
    private val relationWords = listOf(
        Relation.CHILD to listOf("mama", "mami", "papa", "papi"),
        Relation.GRANDCHILD to listOf("abuela", "abuelo", "abu", "yaya", "yayo"),
        Relation.NEPHEW_NIECE to listOf("tia", "tio", "tita", "tito"),
        Relation.SIBLING to listOf("hermana", "hermano", "tata"),
        Relation.CHILD to listOf("mum", "mom", "mummy", "mommy", "dad", "daddy"),
        Relation.GRANDCHILD to listOf("grandma", "grandpa", "granny", "nan", "nana", "nanny", "gran", "grandad", "granddad"),
        Relation.NEPHEW_NIECE to listOf("auntie", "aunt", "uncle"),
        Relation.SIBLING to listOf("sis", "bro"),
    )

    fun detect(message: IncomingMessage, context: ShieldContext): Detection {
        val folded = Text.fold(message.text)
        val signals = mutableSetOf<Signal>()
        val hosts = Links.hosts(message.text)
        val mentioned = Brands.mentionedIn(folded) + Brands.mentionedIn(Text.fold(message.sender))
        val checked = hosts + listOfNotNull(Links.senderDomain(message.sender))
        val impersonated = checked.firstNotNullOfOrNull { Brands.impersonated(it, mentioned) }

        val known = context.knows(message.sender, byName = message.nameFromContacts)
        if (!known) signals += Signal.UNKNOWN_SENDER
        if (hosts.isNotEmpty()) signals += Signal.LINK
        if (hosts.any(Links::isShortener)) signals += Signal.URL_SHORTENER
        if (Links.insecure(message.text)) signals += Signal.INSECURE_LINK
        if (impersonated != null) signals += Signal.BRAND_LOOKALIKE
        if (urgency.containsMatchIn(folded) || deadline.containsMatchIn(folded)) signals += Signal.URGENCY
        if (money.containsMatchIn(folded)) signals += Signal.MONEY_REQUEST
        if (asksForSecret(folded) || asksForPayDetails(folded)) signals += Signal.CREDENTIAL_REQUEST
        if (asksToForwardCode(folded)) signals += Signal.CODE_FORWARD_REQUEST
        if (newNumber.containsMatchIn(folded)) signals += Signal.NEW_NUMBER_CLAIM
        if (prizeOrProfit.containsMatchIn(folded) || freeButFee.containsMatchIn(folded)) signals += Signal.PRIZE_OR_PROFIT
        if (secrecy.containsMatchIn(folded)) signals += Signal.SECRECY
        if ((scare.containsMatchIn(folded) && device.containsMatchIn(folded)) || supportCall.containsMatchIn(folded)) signals += Signal.DEVICE_SCARE
        // Affection from a stranger only: family say "querida" and "mi amor" all the time.
        if (!known && (affection.containsMatchIn(folded) || loveStory.containsMatchIn(folded))) signals += Signal.ROMANCE_BAIT
        if (remoteApps.containsMatchIn(folded)) signals += Signal.REMOTE_ACCESS_APP
        if (aiInstructions.any { it in folded }) signals += Signal.AI_INSTRUCTIONS
        // Letters from another alphabet or invisible ones inside a word, in the text or in a link's international name.
        if (Text.disguised(message.text) || hosts.any { "xn--" in it && Text.disguised(Links.shown(it)) }) signals += Signal.DISGUISED_LETTERS
        val fromMobile = mobile(Text.phoneDigits(message.sender))
        if ((callBack.containsMatchIn(folded) && phoneInText.containsMatchIn(message.text) && personBehind(folded, message)) ||
            (fromMobile && personalCall.containsMatchIn(folded) && thisNumber.containsMatchIn(folded))
        ) signals += Signal.CALLBACK_REQUEST
        // A brand (not the apps people pay or chat with) or a public body, from a personal mobile or a free email
        // address; a notice whose links all go to the brand's own website is let be.
        val officialLinksOnly = hosts.isNotEmpty() && hosts.all { h -> mentioned.any { Brands.isOfficial(h, it) } }
        val personal = fromMobile || Links.senderDomain(message.sender) in freeMail
        if (personal && !officialLinksOnly && speaksAsCompany(folded)) signals += Signal.COMPANY_FROM_MOBILE
        val officialSender = Links.senderDomain(message.sender)?.let { d -> mentioned.any { Brands.isOfficial(d, it) } } == true
        if (safePayment.containsMatchIn(folded) && acceptIt.containsMatchIn(folded) && sale.containsMatchIn(folded) && !officialLinksOnly && !officialSender) {
            signals += Signal.SAFE_PAYMENT_BAIT
        }
        // Telegram always; WhatsApp only from a text or an email (on WhatsApp, "write to me here" is no move).
        if (otherApp.containsMatchIn(folded) || (message.channel != Channel.WHATSAPP && whatsApp.containsMatchIn(folded) && switchVerb.containsMatchIn(folded))) {
            signals += Signal.CHANNEL_SWITCH
        }
        if (mistakenPayment.containsMatchIn(folded)) signals += Signal.MISTAKEN_PAYMENT
        if (codeWord.containsMatchIn(folded) && codeDigits.containsMatchIn(folded)) {
            signals += Signal.ONE_TIME_CODE
            if (context.callActive) signals += Signal.CODE_DURING_CALL
        }
        return Detection(signals, impersonated, mentioned, claimedRelation(folded))
    }

    /** "Díganos el código" asks for a secret; "no lo compartas con nadie" is the bank warning against it. */
    private fun asksForSecret(folded: String): Boolean = Text.sentences(folded).any { sentence ->
        val words = Text.words(sentence)
        secrets.containsMatchIn(sentence) && words.indices.any { i ->
            words[i] in askVerbs && !negated(words, i)
        }
    }

    /**
     * Whether a person, not a company, asks to be called: "call me", a mobile phone as the sender (a company's notice
     * comes under its name or from a landline), or a mobile number to call (companies give their lines).
     */
    private fun personBehind(folded: String, message: IncomingMessage): Boolean =
        personalCall.containsMatchIn(folded) || mobile(Text.phoneDigits(message.sender)) ||
            phoneInText.findAll(message.text).any { mobile(Text.phoneDigits(it.value)) }

    /** A Spanish (6xx, 7xx) or British (07, +44 7) mobile number, from its digits. */
    private fun mobile(d: String): Boolean =
        (d.length == 9 && (d[0] == '6' || d[0] == '7')) || (d.length == 11 && d.startsWith("07")) || (d.length == 12 && d.startsWith("447"))

    /**
     * The message speaks as a company or a public body ("soy Marta de Correos", "Barclays here", "from your bank",
     * "Movistar: ..."), rather than just naming one ("I left O2 for Vodafone").
     */
    private fun speaksAsCompany(folded: String): Boolean {
        fun company(window: String) = Brands.mentionedIn(window).any { it.name !in peoplesApps } || institution.containsMatchIn(window)
        return introAfter.findAll(folded).any { m -> company(Text.words(folded.substring(m.range.last + 1)).take(5).joinToString(" ")) } ||
            introBefore.findAll(folded).any { m -> company(Text.words(folded.substring(0, m.range.first)).takeLast(3).joinToString(" ")) } ||
            (folded.take(30).contains(':') && company(folded.take(30).substringBefore(':')))
    }

    private val introAfter = Regex(
        """\b(soy|somos|le escribo|te escribo|le escribimos|te escribimos|le escribe|te escribe|de parte de|hablamos|llamamos|aviso de|aviso del|""" +
            """this is|from|calling from|writing from|here at)\b""",
    )
    private val introBefore = Regex("""\b(here|team|le informa|te informa)\b""")

    /** Apps people pay, chat or sell with as themselves: naming one is not speaking as a company. */
    private val peoplesApps = setOf("WhatsApp", "Bizum", "Wallapop", "Vinted", "Milanuncios", "eBay", "Gumtree")

    /** Free email providers: a company writes from its own domain, never from one of these. */
    private val freeMail = setOf(
        "gmail.com", "googlemail.com", "hotmail.com", "hotmail.es", "hotmail.co.uk", "outlook.com", "outlook.es", "live.com", "yahoo.com",
        "yahoo.es", "yahoo.co.uk", "icloud.com", "me.com", "aol.com", "gmx.com", "gmx.es", "proton.me", "protonmail.com", "mail.com", "yandex.com",
        "tutanota.com", "tuta.io",
    )

    /** "Pase su número de cuenta", "reply with your card"; "we will never ask for your IBAN" is a warning. */
    private fun asksForPayDetails(folded: String): Boolean = Text.sentences(folded).any { sentence ->
        payDetailsAsk.containsMatchIn(sentence) && !neverAsk.containsMatchIn(sentence)
    }

    /** "Díganos el código que le ha llegado", "reply with the code we sent you"; "never share the code we sent" is a warning. */
    private fun asksToForwardCode(folded: String): Boolean {
        val sentences = Text.sentences(folded)
        val anySentenceWithCode = sentences.any { codeSent.containsMatchIn(it) }
        return anySentenceWithCode && sentences.any { sentence ->
            val words = Text.words(sentence)
            words.indices.any { i -> words[i] in forwardVerbs && !negated(words, i) } ||
                (forwardPhrase.containsMatchIn(sentence) && !neverAsk.containsMatchIn(sentence))
        }
    }

    /**
     * A "no" in the three words before the verb negates it, unless a "y"/"and" comes between: in "no cuelgue y
     * facilite el código" the "no" belongs to "cuelgue", and the message still asks for the code.
     */
    private fun negated(words: List<String>, i: Int): Boolean {
        val before = words.subList(maxOf(0, i - 3), i)
        return before.drop(before.indexOfLast { it in joins } + 1).any { it in negations }
    }

    private val joins = setOf("y", "e", "and", "then", "luego")

    /** Giving the code to someone; entering it in the official app ("introduce", "enter") is fine. */
    private val forwardVerbs = setOf(
        "dime", "digame", "diganos", "dinos", "dimelo", "digamelo", "diganoslo", "diga", "digas", "facilite", "facilitenos",
        "envie", "envia", "enviame", "envienos", "mande", "manda", "mandame", "reenvie", "reenvia", "comparta", "comparte",
        "pasame", "paseme", "reenvieme", "digale", "digaselo", "dicte", "dicta", "responda", "responde", "conteste", "contesta",
        "dictamelo", "dictame", "dictemelo", "compartelo", "compartela", "comparteme",
        "tell", "send", "share", "give", "forward", "reply", "read", "say",
    )

    private fun claimedRelation(folded: String): Relation? {
        val opening = Text.words(folded).take(6)
        return relationWords.firstOrNull { (_, words) -> opening.any { it in words } }?.first
    }
}
