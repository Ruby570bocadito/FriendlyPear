package app.friendlypear.net

import app.friendlypear.config.LastScam
import app.friendlypear.core.model.Lang
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** The report Muse Spark wrote, and where to take it (fixed text from the backend, never the model's). */
data class ScamReport(val title: String, val body: String, val where: List<String>)

/**
 * "Report it": sends the scam Peri stopped (the message, its sender, when, why) to the backend, which has Muse
 * Spark write it up for the police or a helpline. Only when the person asks; nothing is kept. Blocking: call it
 * off the main thread.
 */
class ReportClient(private val baseUrl: String, private val deviceToken: () -> String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken().length >= 32

    fun write(scam: LastScam.Scam, lang: Lang): ScamReport? {
        if (!enabled) return null
        val body = JSONObject()
            .put("lang", if (lang == Lang.ES) "es" else "en")
            .put("channel", scam.channel)
            .put("sender", scam.sender.take(120))
            .put("text", scam.text.take(2_000))
            .put("at", scam.at)
            // The time as this phone shows it, in its own time zone and language.
            .put("when", java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.SHORT, if (lang == Lang.ES) java.util.Locale("es", "ES") else java.util.Locale.UK).format(java.util.Date(scam.at)))
            .put("reason", scam.reason)
            .put("signals", JSONArray(scam.signals))
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}/v1/report").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("authorization", "Bearer ${deviceToken()}")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != 200) return null
            val answer = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val where = answer.optJSONArray("where") ?: JSONArray()
            ScamReport(answer.optString("title"), answer.optString("body"), (0 until where.length()).map { where.optString(it) })
                .takeIf { it.body.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
