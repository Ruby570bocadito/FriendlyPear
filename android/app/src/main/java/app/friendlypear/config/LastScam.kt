package app.friendlypear.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * The last scam Peri stopped, kept on the phone for [KEEP_MS] so the person can report it ("Report it"). It leaves
 * the phone only if they ask for the report. Hiding the offer, or a newer scam, replaces it.
 */
class LastScam(context: Context) {
    /** [kind] is the kind of trap, for "How the trap worked" ([app.friendlypear.ui.peri.TrapActivity.kindOf]). */
    data class Scam(val channel: String, val sender: String, val text: String, val at: Long, val reason: String, val signals: List<String>, val kind: String = "link")

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(scam: Scam) {
        val json = JSONObject()
            .put("channel", scam.channel)
            .put("sender", scam.sender)
            .put("text", scam.text)
            .put("at", scam.at)
            .put("reason", scam.reason)
            .put("signals", JSONArray(scam.signals))
            .put("kind", scam.kind)
        prefs.edit().putString(KEY, json.toString()).putBoolean(HIDDEN, false).apply()
    }

    /** The scam to offer a report for: recent, and not hidden by the person. */
    fun offer(now: Long = System.currentTimeMillis()): Scam? = load(now)?.takeIf { !prefs.getBoolean(HIDDEN, false) }

    fun load(now: Long = System.currentTimeMillis()): Scam? {
        val json = runCatching { JSONObject(prefs.getString(KEY, null) ?: return null) }.getOrNull() ?: return null
        val at = json.optLong("at")
        if (now - at > KEEP_MS) return null.also { clear() }
        val signals = json.optJSONArray("signals") ?: JSONArray()
        return Scam(
            json.optString("channel"),
            json.optString("sender"),
            json.optString("text"),
            at,
            json.optString("reason"),
            (0 until signals.length()).map { signals.optString(it) },
            json.optString("kind", "link"),
        )
    }

    fun hide() {
        prefs.edit().putBoolean(HIDDEN, true).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS = "last_scam"
        const val KEY = "scam"
        const val HIDDEN = "hidden"
        const val KEEP_MS = 3 * 24 * 60 * 60_000L
    }
}
