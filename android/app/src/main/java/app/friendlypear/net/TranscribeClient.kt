package app.friendlypear.net

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * One voice order to Muse Voice Transcribe through the backend, only when the person turned it on in Settings.
 * The audio is sent once and kept nowhere. Blocking: call it off the main thread. Any failure returns null.
 */
class TranscribeClient(private val baseUrl: String, private val deviceToken: String) {
    val enabled: Boolean get() = baseUrl.isNotBlank() && deviceToken.length >= 32

    fun transcribe(wav: ByteArray): String? {
        if (!enabled) return null
        return try {
            val connection = (URL("${baseUrl.trimEnd('/')}/v1/transcribe").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3_000
                readTimeout = 20_000
                doOutput = true
                setFixedLengthStreamingMode(wav.size)
                setRequestProperty("content-type", "audio/wav")
                setRequestProperty("authorization", "Bearer $deviceToken")
            }
            connection.outputStream.use { it.write(wav) }
            if (connection.responseCode != 200) return null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optString("text").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
