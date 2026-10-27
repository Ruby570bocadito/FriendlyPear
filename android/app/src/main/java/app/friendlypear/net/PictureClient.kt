package app.friendlypear.net

import app.friendlypear.core.model.Lang
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A risky thing in the picture, and where it is when SAM 3.1 could find it (fractions of the picture, 0..1). */
data class PictureMark(val kind: String, val x1: Float, val y1: Float, val x2: Float, val y2: Float)

data class PictureVerdict(val verdict: String, val kinds: List<String>, val marks: List<PictureMark>)

/**
 * "Look at this picture": sends one picture the person chose to the backend (Muse Spark reads it, SAM 3.1 marks
 * what's risky). The picture is never stored, on the phone or the backend. Blocking: call it off the main thread.
 */
class PictureClient(private val baseUrl: String, private val deviceToken: String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken.length >= 32

    fun look(jpegBase64: String, lang: Lang): PictureVerdict? {
        if (!enabled) return null
        val body = JSONObject().put("image", jpegBase64).put("mime", "image/jpeg").put("lang", if (lang == Lang.ES) "es" else "en")
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}/v1/picture").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
                setRequestProperty("authorization", "Bearer $deviceToken")
            }
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            if (connection.responseCode != 200) return null
            val answer = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val kinds = answer.optJSONArray("kinds") ?: JSONArray()
            val marks = answer.optJSONArray("marks") ?: JSONArray()
            PictureVerdict(
                verdict = answer.optString("verdict", "doubt"),
                kinds = (0 until kinds.length()).map { kinds.optString(it) },
                marks = (0 until marks.length()).mapNotNull { i ->
                    val m = marks.optJSONObject(i) ?: return@mapNotNull null
                    val b = m.optJSONObject("box") ?: return@mapNotNull null
                    PictureMark(m.optString("kind"), b.optDouble("x1").toFloat(), b.optDouble("y1").toFloat(), b.optDouble("x2").toFloat(), b.optDouble("y2").toFloat())
                },
            )
        } catch (e: Exception) {
            null
        }
    }
}
