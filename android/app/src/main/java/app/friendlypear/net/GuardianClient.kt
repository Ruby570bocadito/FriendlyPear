package app.friendlypear.net

import app.friendlypear.core.model.Lang
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Peri's news for the guardians, only while the person shares it: a personal link per guardian, and events
 * that say what happened, never what it was about (a warning's level and channel, a pause and its answer).
 * Blocking: call it off the main thread. Any failure returns null or false; nothing is retried in a loop.
 */
class GuardianClient(private val baseUrl: String, private val deviceToken: () -> String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken().length >= 32

    fun createLink(name: String, lang: Lang): String? {
        val body = JSONObject().put("name", name).put("lang", if (lang == Lang.ES) "es" else "en")
        return call("POST", "/v1/guardians/links", body, expect = 201)?.optString("url")?.takeIf { it.isNotBlank() }
    }

    /** Turning sharing off: every guardian link stops working. */
    fun revokeAll(): Boolean = call("DELETE", "/v1/guardians/links", null, expect = 200) != null

    fun warning(level: String, channel: String): Boolean =
        call("POST", "/v1/events", JSONObject().put("kind", "warning").put("level", level).put("channel", channel), expect = 201) != null

    fun pause(answer: String): Boolean =
        call("POST", "/v1/events", JSONObject().put("kind", "pause").put("answer", answer), expect = 201) != null

    private fun call(method: String, path: String, body: JSONObject?, expect: Int): JSONObject? {
        if (!enabled) return null
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 3_000
                readTimeout = 8_000
                setRequestProperty("authorization", "Bearer ${deviceToken()}")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("content-type", "application/json")
                }
            }
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != expect) null else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            null
        }
    }
}
