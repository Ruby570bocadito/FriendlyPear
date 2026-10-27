package app.friendlypear.net

import app.friendlypear.core.agent.ApprovalKind
import app.friendlypear.core.agent.ApprovalResult
import app.friendlypear.core.model.Lang
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Family MFA against the backend: creates a one-time approval link and waits for the guardian's answer.
 * Only the kind of action and a first name travel. Blocking: call it off the main thread.
 */
class ApprovalClient(private val baseUrl: String, private val deviceToken: String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken.length >= 32

    data class Created(val id: String, val url: String)

    fun create(kind: ApprovalKind, toName: String, lang: Lang, onCall: Boolean = false): Created? {
        if (!enabled) return null
        val body = JSONObject()
            .put("kind", kind.name.lowercase())
            .put("toName", toName)
            .put("lang", if (lang == Lang.ES) "es" else "en")
            .put("onCall", onCall)
        val answer = call("POST", "/v1/approvals", body.toString(), expect = 201) ?: return null
        return Created(answer.getString("id"), answer.getString("url"))
    }

    /** Polls until the guardian decides or the link expires (15 minutes, plus a margin). */
    fun await(id: String, everyMillis: Long = 3_000, giveUpMillis: Long = 16 * 60_000): ApprovalResult {
        val until = System.currentTimeMillis() + giveUpMillis
        while (System.currentTimeMillis() < until) {
            when (call("GET", "/v1/approvals/$id", null, expect = 200)?.optString("status")) {
                "approved" -> return ApprovalResult.APPROVED
                "rejected" -> return ApprovalResult.REJECTED
                "callback" -> return ApprovalResult.CALL_BACK
                "expired" -> return ApprovalResult.EXPIRED
            }
            Thread.sleep(everyMillis)
        }
        return ApprovalResult.EXPIRED
    }

    private fun call(method: String, path: String, body: String?, expect: Int): JSONObject? = try {
        val connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 3_000
            readTimeout = 8_000
            setRequestProperty("authorization", "Bearer $deviceToken")
            if (body != null) {
                doOutput = true
                setRequestProperty("content-type", "application/json")
            }
        }
        if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
        if (connection.responseCode != expect) null
        else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        null
    }
}
