package app.friendlypear.net

import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Relation
import app.friendlypear.core.shield.Signal
import app.friendlypear.core.shield.Verdict
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** What the investigator found: its vote and short evidence lines for the family, never links or codes. */
data class Investigated(val delta: Double, val verdict: String, val evidence: List<String>)

/**
 * Asks the backend about a doubtful message: Muse Spark Open's second opinion, and, if it is still doubtful,
 * the Muse Spark investigator. Only the text, the channel and what the rules saw travel (plus, for the
 * investigator, which relations the family has); the sender stays on the phone. Any failure returns null and
 * Peri keeps the rules' verdict.
 */
class OpinionClient(private val baseUrl: String, private val deviceToken: () -> String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken().length >= 32

    /** Blocking: call it off the main thread. Returns the vote in log-odds, at most ±2. */
    fun ask(message: IncomingMessage, verdict: Verdict, callActive: Boolean, lang: Lang): Double? =
        post("/v1/opinion", body(message, verdict, callActive, lang), OPINION_TIMEOUT)
            ?.optDouble("delta")?.takeIf { it.isFinite() }

    /** Blocking and slower: the investigator looks things up for a few rounds before answering. */
    fun investigate(message: IncomingMessage, verdict: Verdict, callActive: Boolean, lang: Lang, relations: Set<Relation>): Investigated? {
        val body = body(message, verdict, callActive, lang)
            .put("familyRelations", JSONArray(relations.filter { it != Relation.OTHER }.map { it.name }))
        val answer = post("/v1/investigate", body, INVESTIGATION_TIMEOUT) ?: return null
        val delta = answer.optDouble("delta").takeIf { it.isFinite() } ?: return null
        val lines = answer.optJSONArray("evidence") ?: JSONArray()
        val evidence = (0 until lines.length()).mapNotNull { lines.optString(it).takeIf(String::isNotBlank) }.take(MAX_EVIDENCE)
        return Investigated(delta, answer.optString("verdict", "doubt"), evidence)
    }

    private fun body(message: IncomingMessage, verdict: Verdict, callActive: Boolean, lang: Lang) = JSONObject()
        .put("level", verdict.level.name)
        .put("channel", message.channel.name)
        .put("senderKnown", Signal.UNKNOWN_SENDER !in verdict.signals)
        .put("callActive", callActive)
        .put("signals", JSONArray(verdict.signals.map { it.name }))
        .put("text", message.text.take(MAX_TEXT))
        .put("lang", if (lang == Lang.ES) "es" else "en")

    private fun post(path: String, body: JSONObject, readTimeoutMillis: Int): JSONObject? {
        if (!enabled) return null
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3_000
                readTimeout = readTimeoutMillis
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("authorization", "Bearer ${deviceToken()}")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != 200) return null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val MAX_TEXT = 2_000
        const val MAX_EVIDENCE = 5
        const val OPINION_TIMEOUT = 12_000
        const val INVESTIGATION_TIMEOUT = 30_000
    }
}
