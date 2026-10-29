package app.friendlypear.core.agent

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Peri's milestones, each with its badge: shields earned (the first, five, twenty) and calm days in a row (three, a
 * week, a month). A scare starts the calm days again; a badge, once earned, stays.
 */
enum class Milestone(val track: Track, val needs: Int, val badge: Badge) {
    FIRST_SHIELD(Track.SHIELDS, 1, Badge.BRONZE),
    FIVE_SHIELDS(Track.SHIELDS, 5, Badge.SILVER),
    TWENTY_SHIELDS(Track.SHIELDS, 20, Badge.GOLD),
    CALM_DAYS(Track.CALM, 3, Badge.BRONZE),
    CALM_WEEK(Track.CALM, 7, Badge.SILVER),
    CALM_MONTH(Track.CALM, 30, Badge.GOLD);

    enum class Track { SHIELDS, CALM }

    enum class Badge { BRONZE, SILVER, GOLD }

    companion object {
        /** The milestones [shields] earned and [calmDays] in a row reach that weren't [earned] before. */
        fun reached(shields: Int, calmDays: Int, earned: Set<Milestone>): List<Milestone> =
            entries.filter { it !in earned && (if (it.track == Track.SHIELDS) shields else calmDays) >= it.needs }

        /** The one to celebrate of each kind: back after a month, Peri celebrates the month, not also the week. */
        fun toCelebrate(reached: List<Milestone>): List<Milestone> =
            reached.groupBy { it.track }.values.map { same -> same.maxBy { it.needs } }

        /** Calm days in a row up to [today], counting it: since the day after the last scare, or since Peri arrived. */
        fun calmDays(today: LocalDate, arrived: LocalDate, lastScare: LocalDate?): Int {
            val start = listOfNotNull(arrived, lastScare?.plusDays(1)).max()
            return if (start > today) 0 else ChronoUnit.DAYS.between(start, today).toInt() + 1
        }
    }
}
