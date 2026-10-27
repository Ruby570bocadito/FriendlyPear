package app.friendlypear.core.trust

/**
 * Changing who the guardians are is how a scammer, or an abusive relative, would take over the family's
 * protection. So the first guardian is set in person at once, and every later change needs a yes from a
 * current guardian (all of them are told) and then waits 48 hours. The protected person can cancel it any time.
 */
data class PendingCircleChange(
    /** The guardians after the change. Only guardians wait; relations, nicknames and the family word don't. */
    val guardians: Set<String>,
    val requestedAt: Long,
    val approved: Boolean = false,
    /** The backend request the guardians answer, so waiting can resume if the phone restarts. */
    val approvalId: String? = null,
) {
    val effectiveAt: Long get() = requestedAt + CircleRules.COOLING_OFF_MILLIS
}

object CircleRules {
    const val COOLING_OFF_MILLIS = 48L * 60 * 60 * 1000

    /** True when this change touches the guardians of an already set-up circle. */
    fun needsGuardians(current: FamilyConfig, proposed: FamilyConfig): Boolean =
        current.guardians.isNotEmpty() && current.guardians != proposed.guardians

    fun propose(proposed: FamilyConfig, now: Long) = PendingCircleChange(proposed.guardians, now)

    /** The configuration in force at [now]: a change applies only once approved and 48 hours old. */
    fun effective(current: FamilyConfig, pending: PendingCircleChange?, now: Long): FamilyConfig =
        if (pending != null && pending.approved && now >= pending.effectiveAt) current.copy(guardians = pending.guardians) else current

    fun isDue(pending: PendingCircleChange?, now: Long): Boolean = pending != null && pending.approved && now >= pending.effectiveAt

    /** Who is being added or removed, to tell the guardians in plain words. */
    fun added(current: FamilyConfig, pending: PendingCircleChange): Set<String> = pending.guardians - current.guardians

    fun removed(current: FamilyConfig, pending: PendingCircleChange): Set<String> = current.guardians - pending.guardians
}
