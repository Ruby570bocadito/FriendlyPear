package app.friendlypear.net

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.friendlypear.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * This phone's own key for Peri's server, from pairing in person (a QR or a code from `bun pair`). Kept
 * encrypted with a key that never leaves Android's keystore, and never backed up. Until the phone is paired,
 * debug builds use the shared test token, if the server still allows it.
 */
object DeviceKey {
    private const val PREFS = "device"
    private const val SEALED = "sealed_key"
    private const val ALIAS = "friendlypear_device_key"
    private val CODE = Regex("^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$")

    @Volatile private var cached: String? = null

    /** The key every request carries. The shared test token only ever in a debug build. */
    fun current(context: Context): String = own(context) ?: if (BuildConfig.DEBUG) BuildConfig.DEVICE_TOKEN else ""

    /** This phone's own key, if it is paired: never the shared test token. */
    fun mine(context: Context): String? = own(context)

    fun paired(context: Context): Boolean = own(context) != null

    enum class Outcome { PAIRED, BAD_CODE, REFUSED, FAILED }

    fun isCode(code: String) = CODE.matches(code.trim().uppercase())

    /** Only a QR pointing at this app's own server is accepted: a QR can never send Peri somewhere else. */
    fun isOurServer(server: String): Boolean = origin(server) != null && origin(server) == origin(BuildConfig.BACKEND_URL)

    /** Off the main thread: redeems the one-time code and keeps the key it gets back. */
    fun pair(context: Context, code: String): Outcome {
        val clean = code.trim().uppercase()
        if (!isCode(clean)) return Outcome.BAD_CODE
        return runCatching {
            val c = (URL("${BuildConfig.BACKEND_URL.trimEnd('/')}/v1/devices").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("content-type", "application/json")
            }
            try {
                val name = "${Build.MANUFACTURER} ${Build.MODEL}".replace(Regex("[^\\p{L}\\p{N} ._()-]"), "").take(40).ifBlank { "Android" }
                c.outputStream.use { it.write(JSONObject().put("code", clean).put("name", name).toString().toByteArray()) }
                when (c.responseCode) {
                    201 -> {
                        val key = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getString("key")
                        store(context, key)
                        Outcome.PAIRED
                    }
                    400, 403 -> Outcome.REFUSED
                    else -> Outcome.FAILED
                }
            } finally {
                c.disconnect()
            }
        }.getOrDefault(Outcome.FAILED)
    }

    /** Off the main thread: tells the server to forget this phone's key, then forgets it here too. */
    fun unpair(context: Context): Boolean {
        val key = own(context) ?: return true
        val ok = runCatching {
            val c = (URL("${BuildConfig.BACKEND_URL.trimEnd('/')}/v1/devices/self").openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("authorization", "Bearer $key")
            }
            try {
                c.responseCode == 200 || c.responseCode == 401
            } finally {
                c.disconnect()
            }
        }.getOrDefault(false)
        if (ok) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(SEALED).apply()
            cached = null
        }
        return ok
    }

    private fun own(context: Context): String? {
        cached?.let { return it }
        val sealed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SEALED, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(sealed, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(), GCMParameterSpec(128, bytes, 0, 12))
            String(cipher.doFinal(bytes, 12, bytes.size - 12))
        }.getOrNull()?.also { cached = it }
    }

    private fun store(context: Context, key: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val sealed = cipher.iv + cipher.doFinal(key.toByteArray())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(SEALED, Base64.encodeToString(sealed, Base64.NO_WRAP)).apply()
        cached = key
    }

    private fun keystoreKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private fun origin(url: String): String? = runCatching {
        val u = URI(url.trim())
        val scheme = u.scheme?.lowercase() ?: return null
        val host = u.host?.lowercase() ?: return null
        val port = if (u.port == -1) (if (scheme == "https") 443 else 80) else u.port
        "$scheme://$host:$port"
    }.getOrNull()
}
