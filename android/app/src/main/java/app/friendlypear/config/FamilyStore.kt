package app.friendlypear.config

import android.content.Context
import app.friendlypear.core.model.Relation
import app.friendlypear.core.trust.FamilyConfig
import app.friendlypear.core.trust.PendingCircleChange
import org.json.JSONArray
import org.json.JSONObject

/** The family setup, kept only on this phone. It never travels to the backend or to the guardians. */
class FamilyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("family", Context.MODE_PRIVATE)

    fun load(): FamilyConfig {
        val raw = prefs.getString(KEY, null) ?: return FamilyConfig()
        return try {
            val o = JSONObject(raw)
            FamilyConfig(
                guardians = o.optJSONArray("guardians").strings().toSet(),
                relations = o.optJSONObject("relations").entries()
                    .mapNotNull { (id, r) -> runCatching { id to Relation.valueOf(r) }.getOrNull() }.toMap(),
                aliases = o.optJSONObject("aliases")?.let { a -> a.keys().asSequence().associateWith { a.optJSONArray(it).strings() } }.orEmpty(),
                hasFamilyWord = o.optBoolean("hasFamilyWord", false),
            )
        } catch (e: Exception) {
            FamilyConfig()
        }
    }

    fun save(config: FamilyConfig) {
        val o = JSONObject()
            .put("guardians", JSONArray(config.guardians.toList()))
            .put("relations", JSONObject(config.relations.mapValues { it.value.name }))
            .put("aliases", JSONObject(config.aliases.mapValues { JSONArray(it.value) }))
            .put("hasFamilyWord", config.hasFamilyWord)
        prefs.edit().putString(KEY, o.toString()).apply()
    }

    /** A change of guardians waiting for a guardian's yes and its 48 hours. */
    fun loadPending(): PendingCircleChange? {
        val raw = prefs.getString(PENDING, null) ?: return null
        return try {
            val o = JSONObject(raw)
            PendingCircleChange(
                o.optJSONArray("guardians").strings().toSet(),
                o.getLong("requestedAt"),
                o.optBoolean("approved", false),
                o.optString("approvalId").takeIf { it.isNotEmpty() },
            )
        } catch (e: Exception) {
            null
        }
    }

    fun savePending(pending: PendingCircleChange?) {
        if (pending == null) return prefs.edit().remove(PENDING).apply()
        val o = JSONObject()
            .put("guardians", JSONArray(pending.guardians.toList()))
            .put("requestedAt", pending.requestedAt)
            .put("approved", pending.approved)
            .put("approvalId", pending.approvalId ?: "")
        prefs.edit().putString(PENDING, o.toString()).apply()
    }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun JSONObject?.entries(): List<Pair<String, String>> =
        if (this == null) emptyList() else keys().asSequence().map { it to getString(it) }.toList()

    private companion object {
        const val KEY = "config"
        const val PENDING = "pending_circle_change"
    }
}
