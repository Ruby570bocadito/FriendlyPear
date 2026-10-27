package app.friendlypear.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

/** Tells the agent when a call starts or ends; it never touches the call's audio. */
class CallStateWatcher(context: Context, private val onChange: (active: Boolean) -> Unit) {
    private val context = context.applicationContext
    private var started = false

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) = onChange(state != TelephonyManager.CALL_STATE_IDLE)
    }

    /** Returns false while the phone-state permission is missing; call again once it is granted. */
    fun start(): Boolean {
        if (started) return true
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return false
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return false
        telephony.registerTelephonyCallback(context.mainExecutor, callback)
        started = true
        return true
    }
}
