package app.friendlypear.core.trust

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircleRulesTest {
    private val hour = 60L * 60 * 1000
    private val withLaura = FamilyConfig(guardians = setOf("laura"))

    @Test
    fun `the first guardian is set at once`() {
        assertFalse(CircleRules.needsGuardians(FamilyConfig(), withLaura))
    }

    @Test
    fun `adding or removing a guardian later needs the family`() {
        assertTrue(CircleRules.needsGuardians(withLaura, withLaura.withGuardian("marisa", true)))
        assertTrue(CircleRules.needsGuardians(withLaura, withLaura.withGuardian("laura", false)))
    }

    @Test
    fun `nicknames, relations and the family word don't wait`() {
        assertFalse(CircleRules.needsGuardians(withLaura, withLaura.withAliases("pablo", "el chico").copy(hasFamilyWord = true)))
    }

    @Test
    fun `an approved change applies only after 48 hours`() {
        val pending = CircleRules.propose(withLaura.withGuardian("marisa", true), now = 0).copy(approved = true)
        assertEquals(setOf("laura"), CircleRules.effective(withLaura, pending, now = 47 * hour).guardians)
        assertFalse(CircleRules.isDue(pending, 47 * hour))
        assertEquals(setOf("laura", "marisa"), CircleRules.effective(withLaura, pending, now = 48 * hour).guardians)
        assertTrue(CircleRules.isDue(pending, 48 * hour))
    }

    @Test
    fun `without a guardian's yes it never applies, however long it waits`() {
        val pending = CircleRules.propose(withLaura.withGuardian("laura", false), now = 0)
        assertEquals(setOf("laura"), CircleRules.effective(withLaura, pending, now = 1_000 * hour).guardians)
    }

    @Test
    fun `other settings survive the change`() {
        val current = withLaura.withAliases("pablo", "el chico")
        val pending = CircleRules.propose(current.withGuardian("marisa", true), now = 0).copy(approved = true)
        assertEquals(listOf("el chico"), CircleRules.effective(current, pending, 48 * hour).aliases["pablo"])
    }

    @Test
    fun `it says who is added and who is removed`() {
        val pending = CircleRules.propose(FamilyConfig(guardians = setOf("marisa")), now = 0)
        assertEquals(setOf("marisa"), CircleRules.added(withLaura, pending))
        assertEquals(setOf("laura"), CircleRules.removed(withLaura, pending))
    }
}
