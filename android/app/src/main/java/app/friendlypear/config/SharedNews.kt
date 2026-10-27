package app.friendlypear.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exactly what the guardians can see, kept on the person's own phone too, so they always know what their family
 * sees: [kind] "warning" with a [level] ("scam" or "doubt") and [channel], or "pause" with the person's [answer].
 */
data class NewsEntry(val at: Long, val kind: String, val level: String = "", val channel: String = "", val answer: String = "")

/** Whether the person shares Peri's news with the guardians, and the last things shared. Only on this phone. */
class SharedNews(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("shared_news", Context.MODE_PRIVATE)

    var sharing: Boolean
        get() = prefs.getBoolean(SHARING, false)
        set(value) {
            prefs.edit().putBoolean(SHARING, value).apply()
        }

    fun add(entry: NewsEntry) {
        val kept = (listOf(entry) + load()).take(MAX_ENTRIES)
        val json = JSONArray(
            kept.map {
                JSONObject().put("at", it.at).put("kind", it.kind).put("level", it.level).put("channel", it.channel).put("answer", it.answer)
            },
        )
        prefs.edit().putString(ENTRIES, json.toString()).apply()
    }

    fun load(): List<NewsEntry> {
        val json = runCatching { JSONArray(prefs.getString(ENTRIES, "[]")) }.getOrNull() ?: return emptyList()
        return (0 until json.length()).mapNotNull { i ->
            val o = json.optJSONObject(i) ?: return@mapNotNull null
            NewsEntry(o.optLong("at"), o.optString("kind"), o.optString("level"), o.optString("channel"), o.optString("answer"))
        }
    }

    fun clear() {
        prefs.edit().remove(ENTRIES).apply()
    }

    private companion object {
        const val SHARING = "sharing"
        const val ENTRIES = "entries"
        const val MAX_ENTRIES = 30
    }
}
