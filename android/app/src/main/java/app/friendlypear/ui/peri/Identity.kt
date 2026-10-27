package app.friendlypear.ui.peri

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal

/**
 * The phone's own lock (fingerprint, face or PIN) before something that matters: approving a family request,
 * changing who protects you. A phone without a lock screen goes straight on: there is nothing to ask for.
 */
internal fun Activity.confirmIdentity(title: String, subtitle: String, onOk: () -> Unit, onCancel: () -> Unit = {}) {
    val keyguard = getSystemService(KeyguardManager::class.java)
    if (keyguard == null || !keyguard.isDeviceSecure) return onOk()
    val prompt = BiometricPrompt.Builder(this)
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL)
        .build()
    prompt.authenticate(CancellationSignal(), mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onCancel()
    })
}
