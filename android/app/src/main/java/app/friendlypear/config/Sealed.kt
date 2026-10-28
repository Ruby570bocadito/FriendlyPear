package app.friendlypear.config

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * What Peri keeps on the phone about scams and people (the last scam's message, the numbers blocked, who Peri called
 * or texted, the diary, the family setup) is sealed with AES-GCM under a key that never leaves Android's keystore:
 * copied off the phone, the files can't be read, and a changed byte makes them fail to open rather than lie.
 * Values saved before this was in place are read as they are, and sealed the next time they're written.
 */
object Sealed {
    private const val ALIAS = "friendlypear_local"
    private const val MARK = "sealed:"

    @Volatile
    private var key: SecretKey? = null

    /** [text] sealed; [text] itself if the keystore can't be used, so nothing Peri needs is ever lost. */
    fun seal(text: String): String = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        MARK + Base64.encodeToString(cipher.iv + cipher.doFinal(text.toByteArray()), Base64.NO_WRAP)
    }.getOrDefault(text)

    /** The text inside, or [stored] itself if it was never sealed; null if it can't be opened. */
    fun open(stored: String?): String? {
        if (stored == null || !isSealed(stored)) return stored
        return runCatching {
            val bytes = Base64.decode(stored.removePrefix(MARK), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
            String(cipher.doFinal(bytes, 12, bytes.size - 12))
        }.getOrNull()
    }

    fun isSealed(stored: String): Boolean = stored.startsWith(MARK)

    @Synchronized
    private fun key(): SecretKey {
        key?.let { return it }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val found = store.getKey(ALIAS, null) as? SecretKey ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
        return found.also { key = it }
    }
}

fun SharedPreferences.getSealed(name: String): String? = Sealed.open(getString(name, null))

fun SharedPreferences.Editor.putSealed(name: String, value: String): SharedPreferences.Editor = putString(name, Sealed.seal(value))

/** Seals [names] in place if they were saved before sealing was in place. */
fun SharedPreferences.sealOld(vararg names: String) {
    val old = names.mapNotNull { name -> getString(name, null)?.takeUnless(Sealed::isSealed)?.let { name to it } }
    if (old.isEmpty()) return
    edit().apply { old.forEach { (name, value) -> putSealed(name, value) } }.apply()
}
