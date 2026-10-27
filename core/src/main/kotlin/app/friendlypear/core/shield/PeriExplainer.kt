package app.friendlypear.core.shield

import app.friendlypear.core.model.Level

/** Peri's sentence: what is wrong and what to do, kindly, without reading out links or codes. */
internal object PeriExplainer {
    /** Strongest first; Peri names at most two. */
    private val reasons = listOf(
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

    fun explain(level: Level, hardRule: HardRule?, signals: Set<Signal>, action: SafeAction, brand: String?): String {
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
            val extra = top(signals - Signal.UNKNOWN_SENDER, 1).firstOrNull()
            return if (level == Level.SCAM && extra != null) {
                "Dice ser de tu familia desde un número nuevo y $extra: no hagas nada todavía, ¿llamamos a su número de siempre?"
            } else {
                "Dice ser de tu familia desde un número nuevo: ¿llamamos a su número de siempre para comprobarlo?"
            }
        }
        val why = top(signals, 2).ifEmpty { listOf("tiene cosas raras") }.joinToString(" y ")
        return if (level == Level.SCAM) {
            "Es una trampa: ${why}; ${advice(action, brand)}."
        } else {
            "Este mensaje tiene algo raro, $why: ¿lo comprobamos antes de hacer nada?"
        }
    }

    private fun top(signals: Set<Signal>, n: Int): List<String> = reasons.filter { it.first in signals }.take(n).map { it.second }

    private fun advice(action: SafeAction, brand: String?): String = when (action) {
        SafeAction.USE_OFFICIAL_APP -> "no toques nada y, si tienes dudas, entra tú en la app oficial de ${brand ?: "la empresa"}"
        SafeAction.DONT_TOUCH_LINK -> "no toques el enlace"
        SafeAction.HANG_UP -> "cuelga"
        else -> "no hagas nada y, si quieres, se lo preguntamos a tu familia"
    }
}
