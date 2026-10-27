package app.friendlypear.ui.peri

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.device.VoiceInput
import app.friendlypear.services.MessageListener
import app.friendlypear.services.PeriHub

/**
 * Peri's one-button screen: what Peri says, in large letters, and a big button to talk.
 * Permissions are asked for with a sentence that says what for; progressive permissions refine this later.
 */
class PeriActivity : Activity() {
    private lateinit var hub: PeriHub
    private lateinit var voice: VoiceInput
    private lateinit var said: TextView
    private lateinit var talk: Button
    private lateinit var access: Button
    private var resumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hub = PeriApp.hub(this)
        voice = VoiceInput(this, hub.lang, onText = { hub.heard(it) }, onListening = { listening ->
            talk.text = getString(if (listening) R.string.listening else R.string.talk)
        })

        said = TextView(this).apply {
            text = hub.lastSaid.ifEmpty { getString(R.string.peri_greeting) }
            textSize = 28f
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        talk = Button(this).apply {
            text = getString(R.string.talk)
            textSize = 26f
            minHeight = (140 * resources.displayMetrics.density).toInt()
            setOnClickListener { listen() }
        }
        access = Button(this).apply {
            text = getString(R.string.notification_access)
            textSize = 18f
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(access)
                addView(talk, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            },
        )

        val missing = PERMISSIONS.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) {
            said.text = getString(R.string.permissions_why)
            requestPermissions(missing.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            askToScreenCalls()
        }
    }

    /** The call-screening role shows Peri the number before pickup. Asked once, with a sentence that says what for. */
    private fun askToScreenCalls() {
        val roles = getSystemService(RoleManager::class.java) ?: return
        if (!roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) || roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) return
        said.text = getString(R.string.calls_why)
        startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQUEST_SCREENING)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENING) said.text = hub.lastSaid.ifEmpty { getString(R.string.peri_greeting) }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        // Peri may have spoken while the screen was hidden, e.g. during a call.
        if (hub.lastSaid.isNotEmpty()) said.text = hub.lastSaid
        hub.onSaid = { text -> said.text = text }
        hub.onSpeechDone = { if (resumed && hub.isWaitingForAnswer && voice.available) voice.listen() }
        access.visibility = if (hasNotificationAccess()) View.GONE else View.VISIBLE
        hub.refresh()
    }

    override fun onPause() {
        resumed = false
        hub.onSaid = null
        hub.onSpeechDone = null
        super.onPause()
    }

    override fun onDestroy() {
        voice.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        hub.refresh()
        said.text = hub.lastSaid.ifEmpty { getString(R.string.peri_greeting) }
        askToScreenCalls()
    }

    private fun listen() {
        if (!voice.available) {
            said.text = getString(R.string.no_recognizer)
            return
        }
        voice.listen()
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        return ComponentName(this, MessageListener::class.java).flattenToString() in enabled
    }

    private companion object {
        const val REQUEST_PERMISSIONS = 1
        const val REQUEST_SCREENING = 2
        val PERMISSIONS = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}
