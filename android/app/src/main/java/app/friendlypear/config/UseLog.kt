package app.friendlypear.config

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import org.json.JSONArray
import org.json.JSONObject

/**
 * What Peri did with the person's phone, so they can always check: each call she placed, each text she sent,
 * the location she shared, each picture and voice order she sent to be looked at. Only who and when, never what
 * a text said. It stays on this phone (no backups) and keeps the last [MAX] entries.
 */
class UseLog(context: Context) {
    enum class Kind { CALL, TEXT, LOCATION, PICTURE, VOICE }

    data class Entry(val at: Long, val kind: Kind, val who: String?)

    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** [phone] is turned into the contact's name here, if there is one; the same thing again within a few minutes counts once. */
    @Synchronized
    fun add(kind: Kind, phone: String? = null, now: Long = System.currentTimeMillis()) {
        val who = phone?.let { nameOf(it) ?: it }
        val list = all().toMutableList()
        val last = list.firstOrNull()
        if (last != null && last.kind == kind && last.who == who && now - last.at < SAME_MS) list.removeAt(0)
        list.add(0, Entry(now, kind, who))
        val json = JSONArray()
        list.take(MAX).forEach { e -> json.put(JSONObject().put("at", e.at).put("kind", e.kind.name).put("who", e.who ?: "")) }
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    /** Newest first. */
    fun all(): List<Entry> {
        val json = runCatching { JSONArray(prefs.getString(KEY, "[]")) }.getOrNull() ?: return emptyList()
        return (0 until json.length()).mapNotNull { i ->
            val o = json.optJSONObject(i) ?: return@mapNotNull null
            val kind = runCatching { Kind.valueOf(o.optString("kind")) }.getOrNull() ?: return@mapNotNull null
            Entry(o.optLong("at"), kind, o.optString("who").ifEmpty { null })
        }
    }

    private fun nameOf(phone: String): String? {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone))
        return runCatching {
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    private companion object {
        const val PREFS = "uses"
        const val KEY = "entries"
        const val MAX = 30
        const val SAME_MS = 5 * 60_000L
    }
}
