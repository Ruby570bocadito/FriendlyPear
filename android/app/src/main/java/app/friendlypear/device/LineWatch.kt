package app.friendlypear.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import app.friendlypear.BuildConfig
import app.friendlypear.config.getSealed
import app.friendlypear.config.putSealed
import java.security.MessageDigest

/**
 * What a SIM swap looks like from the phone: a different SIM in it, or the mobile line going dead for a while as the
 * internet keeps working (the scammer's new SIM has taken the number, and with it the bank's codes). A dead line with
 * no internet either is just a place without coverage, and airplane mode is the person's choice: neither counts.
 * Only a fingerprint of the SIMs in use is kept, sealed, never their numbers. [onEvent] runs on the main thread.
 */
class LineWatch(context: Context, private val onEvent: (Event) -> Unit) {
    enum class Event { NEW_SIM, LINE_LOST, LINE_BACK }

    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val telephony = this.context.getSystemService(TelephonyManager::class.java)
    private var dead = false
    private var warned = false

    /** Still dead once the wait is over: with the internet working, it's said; without, it looks again later. */
    private val lost = object : Runnable {
        override fun run() {
            if (!dead || warned) return
            if (!internet()) {
                main.postDelayed(this, RECHECK_MS)
                return
            }
            warned = true
            Log.i(TAG, "line lost while the internet works")
            onEvent(Event.LINE_LOST)
        }
    }

    private val callback = object : TelephonyCallback(), TelephonyCallback.ServiceStateListener {
        override fun onServiceStateChanged(state: ServiceState) = serviceState(state.state)
    }

    private val subscriptions = object : SubscriptionManager.OnSubscriptionsChangedListener() {
        override fun onSubscriptionsChanged() = checkSim()
    }

    fun start() {
        checkSim()
        runCatching { telephony?.registerTelephonyCallback(context.mainExecutor, callback) }
            .onFailure { Log.w(TAG, "line watch off: ${it.javaClass.simpleName}") }
        runCatching { context.getSystemService(SubscriptionManager::class.java)?.addOnSubscriptionsChangedListener(context.mainExecutor, subscriptions) }
    }

    private fun serviceState(state: Int) {
        val nowDead = (state == ServiceState.STATE_OUT_OF_SERVICE || state == ServiceState.STATE_EMERGENCY_ONLY) && simInside()
        if (nowDead == dead) return
        dead = nowDead
        if (dead) {
            Log.i(TAG, "line dead, waiting")
            main.postDelayed(lost, LOST_MS)
            return
        }
        main.removeCallbacks(lost)
        if (warned && state == ServiceState.STATE_IN_SERVICE) {
            warned = false
            onEvent(Event.LINE_BACK)
        }
    }

    private fun simInside(): Boolean {
        val sim = telephony?.simState ?: return false
        return sim != TelephonyManager.SIM_STATE_ABSENT && sim != TelephonyManager.SIM_STATE_UNKNOWN
    }

    /** Internet other than the mobile line itself (wifi, usually): the phone is fine, only its line isn't. */
    private fun internet(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && !caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * The SIMs in use, as a fingerprint: a different one from last time is said once. No SIM at all isn't compared
     * (a SIM taken out and put back is the same one); the first look only remembers.
     */
    @SuppressLint("MissingPermission")
    private fun checkSim() {
        if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return
        val active = runCatching { context.getSystemService(SubscriptionManager::class.java)?.activeSubscriptionInfoList }.getOrNull().orEmpty()
        if (active.isEmpty()) return
        val now = fingerprint(active.map { "${it.subscriptionId}:${it.carrierId}:${it.mccString}${it.mncString}" }.sorted())
        val before = prefs.getSealed(KEY)
        if (before == now) return
        prefs.edit().putSealed(KEY, now).apply()
        if (before != null) {
            Log.i(TAG, "a different SIM")
            onEvent(Event.NEW_SIM)
        }
    }

    private fun fingerprint(parts: List<String>): String =
        MessageDigest.getInstance("SHA-256").digest(parts.joinToString("|").toByteArray()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "FriendlyPear"
        const val PREFS = "line"
        const val KEY = "sims"
        /** How long the line stays dead before it's said: long enough for a tunnel or a lift, a minute when testing. */
        val LOST_MS = if (BuildConfig.DEBUG) 60_000L else 15 * 60_000L
        const val RECHECK_MS = 60_000L
    }
}
