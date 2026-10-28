package app.friendlypear.net

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The family's pears on the family's own Peri server: this phone's (the name the person chose, what their Peri
 * wears, their role, calm or attention) and everyone else's. Only a paired phone's own key works, never the shared
 * test token. Blocking: call it off the main thread. Any failure returns null or false (and says why in the log,
 * never the key); a pear is sent once more if the first try fails, and that's all.
 */
class PearClient(private val baseUrl: String, private val ownKey: () -> String?) {
    data class Pear(val id: String, val name: String, val outfit: String, val role: String, val state: String, val updatedAt: Long, val self: Boolean)

    val enabled: Boolean get() = baseUrl.isNotBlank() && ownKey() != null

    fun publish(name: String, outfit: String, role: String, state: String): Boolean {
        val body = JSONObject().put("name", name).put("outfit", outfit).put("role", role).put("state", state)
        if (call("PUT", "/v1/pears/self", body) != null) return true
        Thread.sleep(RETRY_MS)
        return call("PUT", "/v1/pears/self", body) != null
    }

    fun family(): List<Pear>? {
        val json = call("GET", "/v1/pears", null) ?: return null
        val list = json.optJSONArray("pears") ?: return null
        return (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            Pear(o.optString("id"), o.optString("name"), o.optString("outfit"), o.optString("role"), o.optString("state"), o.optLong("updatedAt"), o.optBoolean("self"))
        }
    }

    private fun call(method: String, path: String, body: JSONObject?): JSONObject? {
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
            if (status != 200) null.also { Log.w(TAG, "pears $method $path answered $status") }
            else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            null.also { Log.w(TAG, "pears $method $path failed: ${e.javaClass.simpleName}") }
        }
    }

    private companion object {
        const val TAG = "FriendlyPear"
        const val RETRY_MS = 4_000L
    }
}
