package app.friendlypear.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Peri's diary: what she did and when, in a few words, for the "Diary" section. A scam stopped (and its kind), a
 * doubtful message checked, a money app left in time, the family asked, a website silenced, a game of "Scam or
 * not?". Never what a message said or who sent it. It stays on this phone (no backups) and keeps the last [MAX].
 */
class Diary(context: Context) {
    enum class Kind { SCAM_STOPPED, DOUBT_CHECKED, PAUSE_LEFT, FAMILY_CALLED, FAMILY_ASKED, WEB_SILENCED, QUIZ, FAMILY_CHECKED, BLOCKED_CALL, BLOCKED_TEXT, SCREEN_INTRUDER, LINE_ALERT }

    /** [detail] is a word or two at most: the kind of trap ("parcel"), or a score ("5/6"). */
    data class Entry(val at: Long, val kind: Kind, val detail: String = "")

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).apply { sealOld(KEY) }

    @Synchronized
    fun add(kind: Kind, detail: String = "", now: Long = System.currentTimeMillis()) {
        val list = listOf(Entry(now, kind, detail.take(MAX_DETAIL))) + all()
        val json = JSONArray()
        list.take(MAX).forEach { e -> json.put(JSONObject().put("at", e.at).put("kind", e.kind.name).put("detail", e.detail)) }
        prefs.edit().putSealed(KEY, json.toString()).apply()
    }

    /** Newest first. */
    fun all(): List<Entry> {
        val json = runCatching { JSONArray(prefs.getSealed(KEY) ?: "[]") }.getOrNull() ?: return emptyList()
        return (0 until json.length()).mapNotNull { i ->
            val o = json.optJSONObject(i) ?: return@mapNotNull null
            val kind = runCatching { Kind.valueOf(o.optString("kind")) }.getOrNull() ?: return@mapNotNull null
            Entry(o.optLong("at"), kind, o.optString("detail"))
        }
    }

    /** How many times something of [kind] happened, among the entries kept. */
    fun count(kind: Kind): Int = all().count { it.kind == kind }

    companion object {
        /** The detail of a blocked call or text when someone else in the family blocked that number. */
        const val BY_FAMILY = "family"
        private const val PREFS = "diary"
        private const val KEY = "entries"
        private const val MAX = 60
        private const val MAX_DETAIL = 16
    }
}
