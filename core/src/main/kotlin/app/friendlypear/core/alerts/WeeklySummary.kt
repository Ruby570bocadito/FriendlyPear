package app.friendlypear.core.alerts

import app.friendlypear.core.model.Lang

/** One thing Peri did, exactly as the guardians already see it: a warning (a scam or a doubt) or a safety pause. */
sealed interface FamilyEvent {
    val at: Long

    data class Warning(override val at: Long, val scam: Boolean) : FamilyEvent

    /** [answer]: "call_family", "leave" or "continue", as the person chose in the pause. */
    data class Pause(override val at: Long, val answer: String) : FamilyEvent
}

/**
 * Once a week, each guardian gets a short SMS from the person's Peri: how many scams and doubts she warned about and
 * how many times she paused them before a money app. Only those counts, the same thing the guardians' page shows;
 * a quiet week says so too, because "all calm" is good news for a family. Sent only while sharing is on.
 */
object WeeklySummary {
    const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L

    /** A week since the last summary, or since sharing was turned on if none was sent yet. */
    fun due(now: Long, lastSent: Long?, sharingSince: Long): Boolean = now - maxOf(lastSent ?: 0L, sharingSince) >= WEEK_MILLIS

    fun text(events: List<FamilyEvent>, now: Long, lang: Lang): String {
        val week = events.filter { now - it.at in 0 until WEEK_MILLIS }
        val scams = week.count { it is FamilyEvent.Warning && it.scam }
        val doubts = week.count { it is FamilyEvent.Warning && !it.scam }
        val pauses = week.filterIsInstance<FamilyEvent.Pause>()
        val carriedOn = pauses.count { it.answer == "continue" }
        return if (lang == Lang.ES) spanish(scams, doubts, pauses.size, carriedOn) else english(scams, doubts, pauses.size, carriedOn)
    }

    private fun spanish(scams: Int, doubts: Int, pauses: Int, carriedOn: Int): String {
        if (scams + doubts + pauses == 0) return "Peri: semana tranquila. No he tenido que avisar de ningún timo. Nunca veo ni cuento sus mensajes."
        val parts = buildList {
            if (scams > 0) add(if (scams == 1) "avisé de 1 mensaje que era un timo" else "avisé de $scams mensajes que eran timos")
            if (doubts > 0) add(if (doubts == 1) "pedí comprobar 1 mensaje dudoso" else "pedí comprobar $doubts mensajes dudosos")
            if (pauses > 0) add(if (pauses == 1) "le paré 1 vez antes de una app de dinero" else "le paré $pauses veces antes de una app de dinero")
        }
        val call = if (carriedOn > 0) " Una de las veces decidió seguir: quizá quieras llamarle." else " Si quieres, llámale para charlar."
        return "Peri, resumen de la semana: ${join(parts, "y")}." + call + " Nunca veo ni cuento sus mensajes."
    }

    private fun english(scams: Int, doubts: Int, pauses: Int, carriedOn: Int): String {
        if (scams + doubts + pauses == 0) return "Peri: a quiet week. I didn't have to warn about any scam. I never see or share their messages."
        val parts = buildList {
            if (scams > 0) add(if (scams == 1) "I warned about 1 message that was a scam" else "I warned about $scams messages that were scams")
            if (doubts > 0) add(if (doubts == 1) "asked to check 1 doubtful message" else "asked to check $doubts doubtful messages")
            if (pauses > 0) add(if (pauses == 1) "paused them once before a money app" else "paused them $pauses times before a money app")
        }
        val call = if (carriedOn > 0) " Once they chose to carry on: you may want to give them a call." else " Give them a call for a chat, if you like."
        return "Peri, your weekly summary: ${join(parts, "and")}." + call + " I never see or share their messages."
    }

    private fun join(parts: List<String>, and: String): String =
        if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " $and " + parts.last()
}
