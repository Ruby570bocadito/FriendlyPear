package app.friendlypear.config

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Numbers someone else in the family blocked from a scam's card, as fingerprints only: HMAC-SHA256 of the number
 * (its last nine digits, or a sender's name, see [BlockList.key]) keyed with the family's salt. The family's server
 * keeps the salt and the fingerprints and never sees a number; this phone keeps a sealed copy, so a call can be turned
 * away before it rings, offline too.
 */
class FamilyBlocks(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var kept: Pair<String, Set<String>>? = null

    /** What the family's server said last: its salt and the other phones' fingerprints. */
    fun save(salt: String, prints: Collection<String>) {
        kept = salt to prints.toSet()
        prefs.edit().putSealed(KEY, JSONObject().put("salt", salt).put("prints", JSONArray(prints.toList())).toString()).apply()
    }

    fun has(sender: String): Boolean {
        val (salt, prints) = load() ?: return false
        return prints.isNotEmpty() && print(salt, sender) in prints
    }

    private fun load(): Pair<String, Set<String>>? {
        kept?.let { return it }
        val json = runCatching { JSONObject(prefs.getSealed(KEY) ?: return null) }.getOrNull() ?: return null
        val salt = json.optString("salt").ifEmpty { return null }
        val list = json.optJSONArray("prints") ?: JSONArray()
        return (salt to (0 until list.length()).map { list.optString(it) }.toSet()).also { kept = it }
    }

    companion object {
        private const val PREFS = "family_blocks"
        private const val KEY = "blocks"

        /** [sender]'s fingerprint under the family's [salt]; null for a sender too short to block. */
        fun print(salt: String, sender: String): String? {
            val key = BlockList.key(sender) ?: return null
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(salt.toByteArray(), "HmacSHA256"))
            return mac.doFinal(key.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
