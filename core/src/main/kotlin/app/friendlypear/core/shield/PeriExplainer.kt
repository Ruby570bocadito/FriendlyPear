package app.friendlypear.core.shield

import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Level

/** Peri's sentence: what is wrong and what to do, kindly, without reading out links or codes. */
internal object PeriExplainer {
    /** Strongest first; Peri names at most two. */
    private val order = listOf(
        Signal.AI_INSTRUCTIONS,
        Signal.REMOTE_ACCESS_APP,
        Signal.CREDENTIAL_REQUEST,
        Signal.MONEY_REQUEST,
        Signal.PRIZE_OR_PROFIT,
        Signal.URGENCY,
        Signal.URL_SHORTENER,
        Signal.UNKNOWN_SENDER,
        Signal.LINK,
    )

    private val reasonsEs = mapOf(
        Signal.AI_INSTRUCTIONS to "trae instrucciones escondidas para engañar a las máquinas",
        Signal.REMOTE_ACCESS_APP to "te pide instalar un programa para controlar tu móvil",
        Signal.CREDENTIAL_REQUEST to "te pide un código o una clave",
        Signal.MONEY_REQUEST to "te pide dinero",
        Signal.PRIZE_OR_PROFIT to "promete premios o ganancias",
        Signal.URGENCY to "te mete prisa",
        Signal.URL_SHORTENER to "lleva un enlace que esconde a dónde va",
        Signal.UNKNOWN_SENDER to "no viene de nadie de tu agenda",
        Signal.LINK to "lleva un enlace",
    )

    private val reasonsEn = mapOf(
        Signal.AI_INSTRUCTIONS to "it hides instructions meant to fool machines",
        Signal.REMOTE_ACCESS_APP to "it asks you to install a program to control your phone",
        Signal.CREDENTIAL_REQUEST to "it asks for a code or a password",
        Signal.MONEY_REQUEST to "it asks for money",
        Signal.PRIZE_OR_PROFIT to "it promises prizes or profits",
        Signal.URGENCY to "it rushes you",
        Signal.URL_SHORTENER to "it has a link that hides where it goes",
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
                return "Te ha llegado un código mientras hablas por teléfono: nadie de verdad te lo pedirá, así que cuelga y no se lo digas a nadie."
            HardRule.BRAND_LOOKALIKE -> {
                val who = brand ?: "una empresa conocida"
                return "Dice ser de $who, pero no lo es: es una trampa, así que no toques nada y, si tienes dudas, entra tú en su app oficial."
            }
            null -> Unit
        }
        if (level == Level.OK) return "Parece un mensaje normal."
        if (action == SafeAction.CALL_SAVED_CONTACT) {
            val extra = top(signals - Signal.UNKNOWN_SENDER, 1, reasonsEs).firstOrNull()
            return if (level == Level.SCAM && extra != null) {
                "Dice ser de tu familia desde un número nuevo y $extra: no hagas nada todavía, ¿llamamos a su número de siempre?"
            } else {
                "Dice ser de tu familia desde un número nuevo: ¿llamamos a su número de siempre para comprobarlo?"
            }
        }
        val why = top(signals, 2, reasonsEs).ifEmpty { listOf("tiene cosas raras") }.joinToString(" y ")
        return if (level == Level.SCAM) {
            "Es una trampa: ${why}; ${adviceEs(action, brand)}."
        } else {
            "Este mensaje tiene algo raro, $why: ¿lo comprobamos antes de hacer nada?"
        }
    }

    private fun english(level: Level, hardRule: HardRule?, signals: Set<Signal>, action: SafeAction, brand: String?): String {
        when (hardRule) {
            HardRule.CODE_DURING_CALL ->
                return "You got a code while you're on the phone: nobody genuine will ever ask for it, so hang up and don't tell it to anyone."
            HardRule.BRAND_LOOKALIKE -> {
                val who = brand ?: "a well-known company"
                return "It says it's from $who, but it isn't: it's a trap, so don't touch anything and, if in doubt, open their official app yourself."
            }
            null -> Unit
        }
        if (level == Level.OK) return "It looks like a normal message."
        if (action == SafeAction.CALL_SAVED_CONTACT) {
            val extra = top(signals - Signal.UNKNOWN_SENDER, 1, reasonsEn).firstOrNull()?.removePrefix("it ")
            return if (level == Level.SCAM && extra != null) {
                "It claims to be family from a new number and $extra: don't do anything yet. Shall we call their usual number?"
            } else {
                "It claims to be family from a new number: shall we call their usual number to check?"
            }
        }
        val why = top(signals, 2, reasonsEn).ifEmpty { listOf("it has odd things") }.joinToString(" and ")
        return if (level == Level.SCAM) {
            "It's a trap: ${why}; ${adviceEn(action, brand)}."
        } else {
            "Something is odd about this message, $why: shall we check before doing anything?"
        }
    }

    private fun top(signals: Set<Signal>, n: Int, reasons: Map<Signal, String>): List<String> =
        order.filter { it in signals }.take(n).map { reasons.getValue(it) }

    private fun adviceEs(action: SafeAction, brand: String?): String = when (action) {
        SafeAction.USE_OFFICIAL_APP -> "no toques nada y, si tienes dudas, entra tú en la app oficial de ${brand ?: "la empresa"}"
        SafeAction.DONT_TOUCH_LINK -> "no toques el enlace"
        SafeAction.HANG_UP -> "cuelga"
        else -> "no hagas nada y, si quieres, se lo preguntamos a tu familia"
    }

    private fun adviceEn(action: SafeAction, brand: String?): String = when (action) {
        SafeAction.USE_OFFICIAL_APP -> "don't touch anything and, if in doubt, open the official ${brand ?: "company"} app yourself"
        SafeAction.DONT_TOUCH_LINK -> "don't touch the link"
        SafeAction.HANG_UP -> "hang up"
        else -> "don't do anything and, if you like, we can ask your family"
    }
}
