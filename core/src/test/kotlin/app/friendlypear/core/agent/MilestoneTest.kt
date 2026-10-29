package app.friendlypear.core.agent

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** Peri's milestones: which badge comes when, what she celebrates, and the calm days in a row. */
class MilestoneTest {
    private val day = LocalDate.of(2026, 10, 29)

    @Test
    fun `the first shield brings bronze, five silver and twenty gold`() {
        assertEquals(listOf(Milestone.FIRST_SHIELD), Milestone.reached(1, 0, emptySet()))
        assertEquals(emptyList(), Milestone.reached(4, 0, setOf(Milestone.FIRST_SHIELD)))
        assertEquals(listOf(Milestone.FIVE_SHIELDS), Milestone.reached(5, 0, setOf(Milestone.FIRST_SHIELD)))
        assertEquals(Milestone.Badge.GOLD, Milestone.reached(20, 0, setOf(Milestone.FIRST_SHIELD, Milestone.FIVE_SHIELDS)).single().badge)
    }

    @Test
    fun `a badge once earned is never reached again`() {
        assertEquals(emptyList(), Milestone.reached(25, 40, Milestone.entries.toSet()))
    }

    @Test
    fun `back after a long while, only the highest of each kind is celebrated`() {
        val reached = Milestone.reached(6, 31, emptySet())
        assertEquals(5, reached.size)
        assertEquals(setOf(Milestone.FIVE_SHIELDS, Milestone.CALM_MONTH), Milestone.toCelebrate(reached).toSet())
    }

    @Test
    fun `calm days count from Peri's arrival, today included`() {
        assertEquals(1, Milestone.calmDays(day, day, null))
        assertEquals(3, Milestone.calmDays(day, day.minusDays(2), null))
        assertEquals(listOf(Milestone.CALM_DAYS), Milestone.reached(0, Milestone.calmDays(day, day.minusDays(2), null), emptySet()))
    }

    @Test
    fun `a scare starts the calm days again`() {
        val arrived = day.minusDays(40)
        assertEquals(0, Milestone.calmDays(day, arrived, day))
        assertEquals(1, Milestone.calmDays(day, arrived, day.minusDays(1)))
        assertEquals(7, Milestone.calmDays(day, arrived, day.minusDays(7)))
        // A scare from before Peri arrived (a clock put back) doesn't count.
        assertEquals(41, Milestone.calmDays(day, arrived, arrived.minusDays(3)))
    }
}
