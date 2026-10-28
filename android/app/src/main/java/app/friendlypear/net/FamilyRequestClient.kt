package app.friendlypear.net

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The guardian's side of a family request, when the guardian also has Peri. It needs no device token: the
 * one-time link is the key, exactly as in the browser. What the request says comes from the server, never from
 * the SMS that carried the link.
 */
object FamilyRequestClient {
    /** The asking phone's pear when it asked: its name and what it wears ("STRAW,,BOWTIE"). */
    data class Pear(val name: String, val outfit: String)

    /** [needed]: how many guardians must say yes; [yes]: who already did, by first name. */
    data class Request(
        val kind: String, val toName: String, val onCall: Boolean, val pending: Boolean, val expiresAt: Long, val pear: Pear? = null,
        val needed: Int = 1, val yes: List<String> = emptyList(),
    )

    enum class Answer(val form: String) { REJECT("decision=reject"), CALLBACK("decision=callback"), APPROVE("decision=approve&confirm=yes") }

    /**
     * [ON_CALL]: the server held a yes back, this phone being on a call with a stranger; [IN_DANGER]: held back, this phone
     * being in danger itself (a SIM swap's signs, a trap minutes ago); [HALF]: a first yes of two.
     */
    enum class Outcome { DONE, HALF, GONE, ON_CALL, IN_DANGER, FAILED }

    fun fetch(url: String): Request? = runCatching {
        val c = open(url, "GET").apply { setRequestProperty("Accept", "application/json") }
        try {
            if (c.responseCode != 200) return null
            val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val kind = j.getString("kind")
            if (kind != "send_code" && kind != "circle_change") return null
            val pear = j.optJSONObject("pear")?.let { Pear(it.optString("name").take(40), it.optString("outfit").take(40)) }
            val yes = j.optJSONArray("yes")?.let { list -> (0 until list.length()).map { list.optString(it).take(40) }.filter { it.isNotBlank() } }.orEmpty()
            Request(
                kind, j.getString("toName"), j.optBoolean("onCall"), j.getString("status") == "pending", j.optLong("expiresAt"), pear?.takeIf { it.name.isNotBlank() },
                j.optInt("needed", 1).coerceIn(1, 2), yes,
            )
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    /**
     * The same form the guardian's page sends; a yes always carries the confirmation the page asks for. With this
     * phone's own [key] the server knows who decides, and holds a yes back while this phone is on a stranger's call.
     */
    fun answer(url: String, answer: Answer, key: String? = null): Outcome = runCatching {
        val c = open(url, "POST").apply {
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            if (key != null) setRequestProperty("Authorization", "Bearer $key")
        }
        try {
            c.outputStream.use { it.write(answer.form.toByteArray()) }
            when (c.responseCode) {
                200 -> Outcome.DONE
                202 -> Outcome.HALF
                404, 410 -> Outcome.GONE
                409 -> Outcome.ON_CALL
                423 -> Outcome.IN_DANGER
                else -> Outcome.FAILED
            }
        } finally {
            c.disconnect()
        }
    }.getOrDefault(Outcome.FAILED)

    private fun open(url: String, method: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 8_000
        readTimeout = 8_000
        instanceFollowRedirects = false
    }
}
