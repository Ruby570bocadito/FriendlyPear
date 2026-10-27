package app.friendlypear.config

import android.content.Context

/**
 * The little shields Peri gives: one each time a scam is stopped, and each time the person asks before doing
 * something risky (shows her a picture, checks with the family). Only counts, on this phone. [unseen] are the
 * ones Peri hasn't congratulated the person for yet.
 */
class Shields(context: Context) {
    enum class Kind { STOPPED, ASKED, FAMILY }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun count(kind: Kind): Int = prefs.getInt(kind.name, 0)

    val total: Int get() = Kind.entries.sumOf { count(it) }

    val unseen: Int get() = prefs.getInt(UNSEEN, 0)

    fun add(kind: Kind) {
        prefs.edit().putInt(kind.name, count(kind) + 1).putInt(UNSEEN, unseen + 1).apply()
    }

    fun seen() {
        prefs.edit().putInt(UNSEEN, 0).apply()
    }

    private companion object {
        const val PREFS = "shields"
        const val UNSEEN = "unseen"
    }
}
