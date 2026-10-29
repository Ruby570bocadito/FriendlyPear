package app.friendlypear.config

import android.content.Context
import app.friendlypear.core.agent.Milestone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The milestones reached on this phone ([Milestone]): their badges stay, and [pending] is the next one Peri hasn't
 * celebrated yet. Only their names, on this phone.
 */
class Milestones(context: Context) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val earned: Set<Milestone> get() = named(prefs.getStringSet(EARNED, null).orEmpty()).toSet()

    val pending: Milestone? get() = queue().firstOrNull()

    /** What the shields and the calm days reach now: earned for good, and the highest of each kind waits for Peri. */
    fun update(shields: Int, calmDays: Int) {
        val before = earned
        val new = Milestone.reached(shields, calmDays, before)
        if (new.isEmpty()) return
        prefs.edit()
            .putStringSet(EARNED, (before + new).map { it.name }.toSet())
            .putString(PENDING, (queue() + Milestone.toCelebrate(new)).joinToString(",") { it.name })
            .apply()
    }

    fun celebrated(milestone: Milestone) {
        prefs.edit().putString(PENDING, (queue() - milestone).joinToString(",") { it.name }).apply()
    }

    /** Calm days in a row: since the last scare in Peri's [diary], or since she arrived on this phone. */
    fun calmDays(diary: Diary, zone: ZoneId = ZoneId.systemDefault()): Int {
        fun day(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val arrived = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }.getOrDefault(System.currentTimeMillis())
        val scare = diary.all().firstOrNull { it.kind in SCARES }?.at
        return Milestone.calmDays(LocalDate.now(zone), day(arrived), scare?.let { day(it) })
    }

    private fun queue(): List<Milestone> = named(prefs.getString(PENDING, "").orEmpty().split(','))

    private fun named(names: Collection<String>): List<Milestone> = names.mapNotNull { name -> Milestone.entries.firstOrNull { it.name == name } }

    private companion object {
        const val PREFS = "milestones"
        const val EARNED = "earned"
        const val PENDING = "pending"

        /** What ends a calm streak: a scam, an app on the screen, a stranger on the family's line, a money app left in a call. */
        val SCARES = setOf(Diary.Kind.SCAM_STOPPED, Diary.Kind.SCREEN_INTRUDER, Diary.Kind.LINE_ALERT, Diary.Kind.PAUSE_LEFT)
    }
}
