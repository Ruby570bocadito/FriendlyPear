package app.friendlypear.net

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The family's pears on the family's own Peri server: this phone's (the name the person chose, what their Peri
 * wears, their role, calm or attention and the kind of trap) and everyone else's, with any campaign (the same trap
 * reaching several of them); and "was it you?", the questions the family's Peris ask each other about a message from
 * a new number; and the numbers the family blocked, as fingerprints. Only a paired phone's own key works, never the shared test token. Blocking: call it off the main thread. Any failure returns null or false (and says why in the log,
 * never the key); a pear is sent once more if the first try fails, and that's all.
 */
class PearClient(private val baseUrl: String, private val ownKey: () -> String?) {
    /** [kind]: the kind of trap behind an attention, or "call" while they're on a call with a number they don't know. */
    data class Pear(val id: String, val name: String, val outfit: String, val role: String, val state: String, val updatedAt: Long, val self: Boolean, val kind: String = "")

    /** Everyone's pears, and the kind of trap reaching several of them right now, if any. */
    data class Family(val pears: List<Pear>, val campaign: String?)

    /** A question from another family phone: who asks (their pear's name) and about what. */
    data class Check(val id: String, val from: String, val kind: String)

    data class Answer(val name: String, val me: Boolean)

    /** What the family answered so far, and how many phones were asked. */
    data class Answers(val answers: List<Answer>, val asked: Int, val gone: Boolean = false)

    val enabled: Boolean get() = baseUrl.isNotBlank() && ownKey() != null

    fun publish(name: String, outfit: String, role: String, state: String, kind: String): Boolean {
        val body = JSONObject().put("name", name).put("outfit", outfit).put("role", role).put("state", state).put("kind", kind)
        if (call("PUT", "/v1/pears/self", body) != null) return true
        Thread.sleep(RETRY_MS)
        return call("PUT", "/v1/pears/self", body) != null
    }

    fun family(): Family? {
        val json = call("GET", "/v1/pears", null) ?: return null
        val list = json.optJSONArray("pears") ?: return null
        val pears = (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            Pear(o.optString("id"), o.optString("name"), o.optString("outfit"), o.optString("role"), o.optString("state"), o.optLong("updatedAt"), o.optBoolean("self"), o.optString("kind"))
        }
        return Family(pears, if (json.isNull("campaign")) null else json.optString("campaign").ifEmpty { null })
    }

    /** Asks the family's other phones "was it you?"; the question's id, or null. */
    fun ask(kind: String): String? = call("POST", "/v1/checks", JSONObject().put("kind", kind), expect = 201)?.optString("id")?.ifEmpty { null }

    /** The questions other family phones are asking this one, not yet answered. */
    fun openChecks(): List<Check>? {
        val list = call("GET", "/v1/checks", null)?.optJSONArray("checks") ?: return null
        return (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            Check(o.optString("id"), o.optString("from"), o.optString("kind"))
        }
    }

    /** Asks a family phone's Peri to hang up its stranger's call (its pear shows one). */
    fun hangUp(id: String): Boolean = call("POST", "/v1/pears/$id/hangup", JSONObject(), expect = 202) != null

    /** Who in the family asked this phone's Peri to hang up, if anyone ("" if their pear has no name); taken once. */
    fun hangUpRequest(): String? {
        val json = call("GET", "/v1/hangup", null) ?: return null
        return if (json.isNull("from")) null else json.optString("from")
    }

    fun answer(id: String, me: Boolean): Boolean = call("POST", "/v1/checks/$id/answer", JSONObject().put("answer", if (me) "me" else "not_me")) != null

    /** The answers to this phone's question so far. */
    fun answers(id: String): Answers? {
        // Gone (expired, or the server started again): nobody can answer it any more, so it isn't asked again.
        val json = call("GET", "/v1/checks/$id", null) ?: return if (status.get() == 404) Answers(emptyList(), 0, gone = true) else null
        val list = json.optJSONArray("answers") ?: return null
        val answers = (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            Answer(o.optString("name"), o.optString("answer") == "me")
        }
        return Answers(answers, json.optInt("asked"))
    }

    /** The family's salt, and the fingerprints of the numbers the family's other phones blocked. */
    data class Blocks(val salt: String, val prints: List<String>)

    fun blocks(): Blocks? {
        val json = call("GET", "/v1/blocks", null) ?: return null
        val salt = json.optString("salt").ifEmpty { return null }
        val list = json.optJSONArray("prints") ?: return null
        return Blocks(salt, (0 until list.length()).map { list.optString(it) })
    }

    /** This phone's blocked numbers, as fingerprints only, replacing the ones it sent before. */
    fun shareBlocks(prints: List<String>): Boolean = call("PUT", "/v1/blocks/self", JSONObject().put("prints", JSONArray(prints))) != null

    private fun call(method: String, path: String, body: JSONObject?, expect: Int = 200): JSONObject? {
        val key = ownKey() ?: return null
        if (baseUrl.isBlank()) return null
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 5_000
                readTimeout = 8_000
                setRequestProperty("authorization", "Bearer $key")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                }
            }
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val status = connection.responseCode
            this.status.set(status)
            if (status != expect) null.also { Log.w(TAG, "pears $method ${path.replace(CHECK_ID, "/v1/checks/…")} answered $status") }
            else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            null.also { Log.w(TAG, "pears $method ${path.replace(CHECK_ID, "/v1/checks/…")} failed: ${e.javaClass.simpleName}") }
        }
    }

    /** The last answer's HTTP status on this thread. */
    private val status = ThreadLocal<Int>()

    private companion object {
        const val TAG = "FriendlyPear"
        const val RETRY_MS = 4_000L
        /** A question's id stays out of the log. */
        val CHECK_ID = Regex("/v1/checks/[A-Za-z0-9_-]+")
    }
}
