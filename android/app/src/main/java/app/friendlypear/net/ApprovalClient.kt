package app.friendlypear.net

import app.friendlypear.core.agent.ApprovalKind
import app.friendlypear.core.agent.ApprovalResult
import app.friendlypear.core.model.Lang
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Family MFA against the backend: creates a one-time approval link per guardian and waits for their answers (two
 * yeses for the riskiest requests, when there are two guardians to give them). Only the kind of action and first
 * names travel. Blocking: call it off the main thread.
 */
class ApprovalClient(private val baseUrl: String, private val deviceToken: () -> String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken().length >= 32

    /** One guardian's link, by the first name it was made for. */
    data class Link(val name: String, val url: String)

    /** [links]: one per guardian, in the order they were given; [needed]: how many of them must say yes. */
    data class Created(val id: String, val url: String, val links: List<Link> = emptyList(), val needed: Int = 1)

    /** How it ended: [yes] is who said yes, [by] who settled it (a no, "call me first", or the last yes). */
    data class Answer(val result: ApprovalResult, val yes: List<String> = emptyList(), val by: String? = null)

    fun create(kind: ApprovalKind, toName: String, lang: Lang, onCall: Boolean = false, guardians: List<String> = emptyList()): Created? {
        if (!enabled) return null
        val body = JSONObject()
            .put("kind", kind.name.lowercase())
            .put("toName", toName)
            .put("lang", if (lang == Lang.ES) "es" else "en")
            .put("onCall", onCall)
            .put("guardians", JSONArray(guardians))
        val answer = call("POST", "/v1/approvals", body.toString(), expect = 201) ?: return null
        val links = answer.optJSONArray("links")?.let { list ->
            (0 until list.length()).mapNotNull { i -> list.optJSONObject(i)?.let { Link(it.optString("name"), it.optString("url")) } }
        }.orEmpty()
        return Created(answer.getString("id"), answer.getString("url"), links, answer.optInt("needed", 1))
    }

    /**
     * Polls until the guardians decide or the links expire (15 minutes, plus a margin). [onYes] hears who said yes
     * each time a yes comes in while another is still needed.
     */
    fun await(id: String, onYes: (List<String>) -> Unit = {}, everyMillis: Long = 3_000, giveUpMillis: Long = 16 * 60_000): Answer {
        val until = System.currentTimeMillis() + giveUpMillis
        var heard = 0
        while (System.currentTimeMillis() < until) {
            val json = call("GET", "/v1/approvals/$id", null, expect = 200)
            val yes = json?.optJSONArray("yes")?.let { list -> (0 until list.length()).map { list.optString(it) } }.orEmpty()
            val by = json?.takeUnless { it.isNull("by") }?.optString("by")?.ifEmpty { null }
            when (json?.optString("status")) {
                "approved" -> return Answer(ApprovalResult.APPROVED, yes, by)
                "rejected" -> return Answer(ApprovalResult.REJECTED, yes, by)
                "callback" -> return Answer(ApprovalResult.CALL_BACK, yes, by)
                "expired" -> return Answer(ApprovalResult.EXPIRED, yes, by)
                "pending" -> if (yes.size > heard) {
                    heard = yes.size
                    onYes(yes)
                }
            }
            Thread.sleep(everyMillis)
        }
        return Answer(ApprovalResult.EXPIRED)
    }

    private fun call(method: String, path: String, body: String?, expect: Int): JSONObject? = try {
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
        if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
        if (connection.responseCode != expect) null
        else JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
    } catch (e: Exception) {
        null
    }
}
