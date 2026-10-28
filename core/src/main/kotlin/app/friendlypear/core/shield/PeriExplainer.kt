package app.friendlypear.core.shield

import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Level

/** Peri's sentence: what is wrong and what to do, warmly (docs/PERI_VOZ.md), without reading out links or codes. */
internal object PeriExplainer {
    /** Strongest first; Peri names at most two. */
    private val order = listOf(
        Signal.AI_INSTRUCTIONS,
        Signal.REMOTE_ACCESS_APP,
        Signal.DEVICE_SCARE,
        Signal.CODE_FORWARD_REQUEST,
        Signal.CREDENTIAL_REQUEST,
        Signal.MISTAKEN_PAYMENT,
        Signal.SAFE_PAYMENT_BAIT,
        Signal.MONEY_REQUEST,
        Signal.SECRECY,
        Signal.CHANNEL_SWITCH,
        Signal.COMPANY_FROM_MOBILE,
        Signal.ROMANCE_BAIT,
        Signal.PRIZE_OR_PROFIT,
        Signal.CALLBACK_REQUEST,
        Signal.URGENCY,
        Signal.URL_SHORTENER,
        Signal.INSECURE_LINK,
        Signal.UNKNOWN_SENDER,
        Signal.LINK,
    )

    private val reasonsEs = mapOf(
        Signal.AI_INSTRUCTIONS to "trae instrucciones escondidas para engañar a las máquinas",
        Signal.REMOTE_ACCESS_APP to "te pide instalar un programa para controlar tu móvil",
        Signal.CODE_FORWARD_REQUEST to "te pide que le pases un código que te han mandado",
        Signal.CREDENTIAL_REQUEST to "te pide un código, una clave o datos de tu banco",
        Signal.MONEY_REQUEST to "te pide dinero",
        Signal.MISTAKEN_PAYMENT to "dice que te pagó por error y quiere que se lo devuelvas",
        Signal.SAFE_PAYMENT_BAIT to "habla de un «pago seguro» que tienes que aceptar o confirmar",
        Signal.CHANNEL_SWITCH to "quiere llevarte a hablar por otra aplicación",
        Signal.COMPANY_FROM_MOBILE to "dice ser de una empresa pero escribe desde un móvil o un correo particular",
        Signal.CALLBACK_REQUEST to "te pide que llames a un número que trae el propio mensaje",
        Signal.PRIZE_OR_PROFIT to "promete regalos, premios o ganancias",
        Signal.SECRECY to "te pide que no se lo cuentes a tu familia",
        Signal.DEVICE_SCARE to "te asusta con que tu aparato tiene un virus",
        Signal.ROMANCE_BAIT to "usa el cariño de alguien que no conoces para pedirte cosas",
        Signal.URGENCY to "te mete prisa",
        Signal.URL_SHORTENER to "lleva un enlace que esconde a dónde va",
        Signal.INSECURE_LINK to "lleva un enlace sin candado, que no es seguro",
        Signal.UNKNOWN_SENDER to "no viene de nadie de tu agenda",
        Signal.LINK to "lleva un enlace",
    )

    private val reasonsEn = mapOf(
        Signal.AI_INSTRUCTIONS to "it hides instructions meant to fool machines",
        Signal.REMOTE_ACCESS_APP to "it asks you to install a program to control your phone",
        Signal.CODE_FORWARD_REQUEST to "it asks you to pass on a code you were sent",
        Signal.CREDENTIAL_REQUEST to "it asks for a code, a password or your bank details",
        Signal.MONEY_REQUEST to "it asks for money",
        Signal.MISTAKEN_PAYMENT to "it says it paid you by mistake and wants it back",
        Signal.SAFE_PAYMENT_BAIT to "it talks about a “safe payment” you have to accept or confirm",
        Signal.CHANNEL_SWITCH to "it wants to move you to another app",
        Signal.COMPANY_FROM_MOBILE to "it claims to be a company but writes from a personal phone or email",
        Signal.CALLBACK_REQUEST to "it asks you to call a number written in the message",
        Signal.PRIZE_OR_PROFIT to "it promises gifts, prizes or profits",
        Signal.SECRECY to "it asks you to keep it from your family",
        Signal.DEVICE_SCARE to "it scares you with a virus on your device",
        Signal.ROMANCE_BAIT to "it uses a stranger's affection to ask you for things",
        Signal.URGENCY to "it rushes you",
        Signal.URL_SHORTENER to "it has a link that hides where it goes",
        Signal.INSECURE_LINK to "its link has no padlock, so it isn't safe",
        Signal.UNKNOWN_SENDER to "it doesn't come from anyone in your contacts",
        Signal.LINK to "it has a link",
    )

    fun explain(level: Level, hardRule: HardRule?, signals: Set<Signal>, action: SafeAction, brand: String?, lang: Lang): String =
        when (lang) {
            Lang.ES -> spanish(level, hardRule, signals, action, brand)
            Lang.EN -> english(level, hardRule, signals, action, brand?.removePrefix("la ")?.removePrefix("el "))
        }

    private fun spanish(level: Level, hardRule: HardRule?, signals: Set<Signal>, action: SafeAction, brand: String?): String {
        when (hardRule) {
            HardRule.CODE_DURING_CALL ->
                return "Cuelga ahora mismo, por favor: te ha llegado un código mientras hablas por teléfono y nadie de verdad te lo pedirá nunca. No se lo digas a nadie; yo estoy contigo."
            HardRule.BRAND_LOOKALIKE -> {
                val who = brand ?: "una empresa conocida"
                return "Ojo, que esto es una trampa: dice ser de $who, pero no lo es. No toques nada y, si te quedas con dudas, entra tú en su app oficial."
            }
            null -> Unit
        }
        if (level == Level.OK) return "Parece un mensaje normal. ¡Todo bien!"
        if (action == SafeAction.CALL_SAVED_CONTACT) {
            val extra = top(signals - Signal.UNKNOWN_SENDER, 1, reasonsEs).firstOrNull()
            return if (level == Level.SCAM && extra != null) {
                "Cuidado: dice ser de tu familia desde un número nuevo y $extra. No hagas nada todavía; mejor lo comprobamos llamando a su número de siempre."
            } else {
                "Dice ser de tu familia desde un número nuevo. Para quedarnos tranquilos, ¿llamamos a su número de siempre?"
            }
        }
        val why = top(signals, 2, reasonsEs).ifEmpty { listOf("tiene cosas raras") }.joinToString(" y ")
        return if (level == Level.SCAM) {
            "Ojo, que esto es una trampa: $why. ${adviceEs(action, brand)}."
        } else {
            "Este mensaje tiene algo raro: $why. ¿Lo comprobamos juntos antes de hacer nada?"
        }
    }

    private fun english(level: Level, hardRule: HardRule?, signals: Set<Signal>, action: SafeAction, brand: String?): String {
        when (hardRule) {
            HardRule.CODE_DURING_CALL ->
                return "Please hang up right now: you got a code while you're on the phone, and nobody genuine will ever ask for it. Don't share it with anyone; I'm right here with you."
            HardRule.BRAND_LOOKALIKE -> {
                val who = brand ?: "a well-known company"
                return "Careful, this is a trap: it says it's from $who, but it isn't. Don't touch anything and, if you're unsure, open their official app yourself."
            }
            null -> Unit
        }
        if (level == Level.OK) return "It looks like a normal message. All good!"
        if (action == SafeAction.CALL_SAVED_CONTACT) {
            val extra = top(signals - Signal.UNKNOWN_SENDER, 1, reasonsEn).firstOrNull()?.removePrefix("it ")
            return if (level == Level.SCAM && extra != null) {
                "Careful: it claims to be family from a new number and $extra. Don't do anything yet; let's check by calling their usual number."
            } else {
                "It claims to be family from a new number. To be on the safe side, shall we call their usual number?"
            }
        }
        val why = top(signals, 2, reasonsEn).ifEmpty { listOf("it has odd things") }.joinToString(" and ")
        return if (level == Level.SCAM) {
            "Careful, this is a trap: $why. ${adviceEn(action, brand)}."
        } else {
            "Something's a bit odd about this message: $why. Shall we check it together before doing anything?"
        }
    }

    /** A plain "it has a link" says nothing more once a shortened or unsafe link was named: it's left out then. */
    private fun top(signals: Set<Signal>, n: Int, reasons: Map<Signal, String>): List<String> {
        val linkSaid = Signal.URL_SHORTENER in signals || Signal.INSECURE_LINK in signals
        return order.filter { it in signals && !(it == Signal.LINK && linkSaid) }.take(n).map { reasons.getValue(it) }
    }

    private fun adviceEs(action: SafeAction, brand: String?): String = when (action) {
        SafeAction.USE_OFFICIAL_APP -> "No toques nada y, si te quedas con dudas, entra tú en la app oficial de ${brand ?: "la empresa"}"
        SafeAction.DONT_TOUCH_LINK -> "No toques el enlace"
        SafeAction.HANG_UP -> "Cuelga, por favor"
        else -> "No hagas nada y, si quieres, se lo preguntamos a tu familia"
    }

    private fun adviceEn(action: SafeAction, brand: String?): String = when (action) {
        SafeAction.USE_OFFICIAL_APP -> "Don't touch anything and, if you're unsure, open the official ${brand ?: "company"} app yourself"
        SafeAction.DONT_TOUCH_LINK -> "Don't touch the link"
        SafeAction.HANG_UP -> "Please hang up"
        else -> "Don't do anything and, if you like, we can ask your family"
    }
}
