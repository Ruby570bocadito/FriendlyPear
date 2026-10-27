package app.friendlypear.core.alerts

import app.friendlypear.core.model.Lang
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeeklySummaryTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day

    @Test
    fun `due a week after the last summary, or after sharing was turned on`() {
        assertFalse(WeeklySummary.due(now, lastSent = now - 6 * day, sharingSince = 0))
        assertTrue(WeeklySummary.due(now, lastSent = now - 7 * day, sharingSince = 0))
        assertFalse(WeeklySummary.due(now, lastSent = null, sharingSince = now - 2 * day))
        assertTrue(WeeklySummary.due(now, lastSent = null, sharingSince = now - 8 * day))
    }

    @Test
    fun `a quiet week is good news too`() {
        assertEquals(
            "Peri: semana tranquila. No he tenido que avisar de ningún timo. Nunca veo ni cuento sus mensajes.",
            WeeklySummary.text(emptyList(), now, Lang.ES),
        )
        assertTrue(WeeklySummary.text(emptyList(), now, Lang.EN).startsWith("Peri: a quiet week."))
    }

    @Test
    fun `only this week's counts, in plain words`() {
        val events = listOf(
            FamilyEvent.Warning(now - day, scam = true),
            FamilyEvent.Warning(now - 2 * day, scam = true),
            FamilyEvent.Warning(now - 3 * day, scam = false),
            FamilyEvent.Pause(now - 3 * day, "leave"),
            FamilyEvent.Warning(now - 9 * day, scam = true),
        )
        assertEquals(
            "Peri, resumen de la semana: avisé de 2 mensajes que eran timos, pedí comprobar 1 mensaje dudoso y le paré 1 vez " +
                "antes de una app de dinero. Si quieres, llámale para charlar. Nunca veo ni cuento sus mensajes.",
            WeeklySummary.text(events, now, Lang.ES),
        )
    }

    @Test
    fun `carrying on after a pause suggests a call`() {
        val text = WeeklySummary.text(listOf(FamilyEvent.Pause(now - day, "continue")), now, Lang.EN)
        assertEquals(
            "Peri, your weekly summary: paused them once before a money app. Once they chose to carry on: you may want " +
                "to give them a call. I never see or share their messages.",
            text,
        )
    }
}
