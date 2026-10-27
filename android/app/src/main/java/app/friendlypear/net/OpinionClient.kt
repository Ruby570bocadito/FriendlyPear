package app.friendlypear.net

import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.shield.Signal
import app.friendlypear.core.shield.Verdict
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Asks the backend for Muse Spark Open's second opinion on a doubtful message. Only the text, the channel
 * and what the rules saw travel; the sender stays on the phone. Any failure returns null and Peri keeps the
 * rules' verdict.
 */
class OpinionClient(private val baseUrl: String, private val deviceToken: String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken.length >= 32

    /** Blocking: call it off the main thread. Returns the vote in log-odds, at most ±2. */
    fun ask(message: IncomingMessage, verdict: Verdict, callActive: Boolean, lang: Lang): Double? {
        if (!enabled) return null
        val body = JSONObject()
            .put("level", verdict.level.name)
            .put("channel", message.channel.name)
            .put("senderKnown", Signal.UNKNOWN_SENDER !in verdict.signals)
            .put("callActive", callActive)
            .put("signals", JSONArray(verdict.signals.map { it.name }))
            .put("text", message.text.take(MAX_TEXT))
            .put("lang", if (lang == Lang.ES) "es" else "en")
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}/v1/opinion").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3_000
                readTimeout = 12_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("authorization", "Bearer $deviceToken")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != 200) return null
            val answer = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            answer.optDouble("delta").takeIf { it.isFinite() }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val MAX_TEXT = 2_000
    }
}
