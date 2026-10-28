package app.friendlypear.config

import android.content.Context

/**
 * Numbers (or sender names) the person chose to block, from a scam's card: Peri turns their calls away before they
 * ring ([app.friendlypear.services.CallScreener], when Peri screens calls) and never reads their messages aloud. Kept
 * on this phone, never backed up; each can be unblocked in Settings.
 */
class BlockList(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** As the person saw them, oldest first. */
    fun all(): List<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().sorted()

    fun has(sender: String): Boolean {
        val key = key(sender) ?: return false
        return all().any { key(it) == key }
    }

    @Synchronized
    fun add(sender: String) {
        if (key(sender) == null || has(sender)) return
        prefs.edit().putStringSet(KEY, all().toSet() + sender.trim()).apply()
    }

    @Synchronized
    fun remove(sender: String) {
        val key = key(sender) ?: return
        prefs.edit().putStringSet(KEY, all().filterNot { key(it) == key }.toSet()).apply()
    }

    companion object {
        private const val PREFS = "blocked"
        private const val KEY = "senders"

        /** A number by its last nine digits (with or without the country code); a sender's name in lower case. */
        fun key(sender: String): String? {
            val digits = sender.filter { it.isDigit() }
            if (digits.length >= 6) return digits.takeLast(9)
            return sender.trim().lowercase().takeIf { it.length >= 2 }
        }
    }
}
