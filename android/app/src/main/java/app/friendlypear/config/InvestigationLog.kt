package app.friendlypear.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One look the investigator took: when, what it concluded and why. Never the message or who sent it. */
data class InvestigationEntry(val at: Long, val verdict: String, val evidence: List<String>)

/** The last investigations, on the phone only, for the family to see in the family setup. */
class InvestigationLog(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("investigations", Context.MODE_PRIVATE).apply { sealOld(KEY) }

    fun add(entry: InvestigationEntry) {
        val kept = (listOf(entry) + load()).take(MAX_ENTRIES)
        val json = JSONArray(kept.map { JSONObject().put("at", it.at).put("verdict", it.verdict).put("evidence", JSONArray(it.evidence)) })
        prefs.edit().putSealed(KEY, json.toString()).apply()
    }

    fun load(): List<InvestigationEntry> {
        val json = runCatching { JSONArray(prefs.getSealed(KEY) ?: "[]") }.getOrNull() ?: return emptyList()
        return (0 until json.length()).mapNotNull { i ->
            val o = json.optJSONObject(i) ?: return@mapNotNull null
            val lines = o.optJSONArray("evidence") ?: JSONArray()
            InvestigationEntry(o.optLong("at"), o.optString("verdict"), (0 until lines.length()).map { lines.optString(it) })
        }
    }

    private companion object {
        const val KEY = "entries"
        const val MAX_ENTRIES = 10
    }
}
