package app.friendlypear.ui.peri

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
import app.friendlypear.config.Need
import app.friendlypear.device.VoiceInput
import app.friendlypear.services.MessageListener
import app.friendlypear.services.PeriHub

/**
 * Peri's one-button screen: what Peri says, in large letters, and a big button to talk.
 * Permissions are asked one at a time, each with Peri's sentence first: the shield's basics at first start,
 * everything else (microphone, calls, texts, location) the first time it's needed.
 */
class PeriActivity : Activity() {
    private lateinit var hub: PeriHub
    private lateinit var voice: VoiceInput
    private lateinit var said: TextView
    private lateinit var talk: Button
    private lateinit var access: Button
    private lateinit var usage: Button
    private lateinit var family: Button
    private var resumed = false
    private var answer: ((Boolean) -> Unit)? = null
    private val asked by lazy { getSharedPreferences("permissions", MODE_PRIVATE) }

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
        family = Button(this).apply {
            text = getString(R.string.family_title)
            textSize = 18f
            setOnClickListener { startActivity(Intent(this@PeriActivity, FamilyActivity::class.java)) }
        }
        // Usage access is a special setting: Peri explains it first, then opens the screen where it's granted.
        usage = Button(this).apply {
            text = getString(R.string.usage_access)
            textSize = 18f
            setOnClickListener {
                hub.say(getString(R.string.why_usage))
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                addView(access)
                addView(usage)
                addView(family)
                addView(talk, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            },
        )

        // This screen shows Android's dialog whenever Peri needs a permission, now or later.
        hub.permissions.presenter = { need, done ->
            answer = done
            requestPermissions(need.permissions.toTypedArray(), REQUEST_NEED)
        }
        firstStart()
    }

    /** The shield's basics, one at a time and only once each: a "no" is never asked again on every start. */
    private fun firstStart() {
        val next = Need.FIRST_START.firstOrNull { !hub.permissions.granted(it) && !asked.getBoolean(it.name, false) }
        if (next != null) {
            asked.edit().putBoolean(next.name, true).apply()
            return hub.permissions.ask(next) {
                hub.refresh()
                firstStart()
            }
        }
        if (askToScreenCalls()) return
        if (!hasNotificationAccess() && !asked.getBoolean(NOTIFICATIONS, false)) {
            asked.edit().putBoolean(NOTIFICATIONS, true).apply()
            hub.say(getString(R.string.why_notifications))
        }
    }

    /** The call-screening role shows Peri the number before pickup. Asked once, with a sentence that says what for. */
    private fun askToScreenCalls(): Boolean {
        val roles = getSystemService(RoleManager::class.java) ?: return false
        if (!roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) || roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) return false
        if (asked.getBoolean(SCREENING, false)) return false
        asked.edit().putBoolean(SCREENING, true).apply()
        hub.say(getString(R.string.calls_why))
        startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQUEST_SCREENING)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENING) firstStart()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        // Peri may have spoken while the screen was hidden, e.g. during a call.
        if (hub.lastSaid.isNotEmpty()) said.text = hub.lastSaid
        hub.onSaid = { text -> said.text = text }
        hub.onSpeechDone = {
            if (resumed && hub.isWaitingForAnswer && voice.available && hub.permissions.granted(Need.MIC)) voice.listen()
        }
        access.visibility = if (hasNotificationAccess()) View.GONE else View.VISIBLE
        usage.visibility = if (hub.bank.allowed()) View.GONE else View.VISIBLE
        hub.refresh()
    }

    override fun onPause() {
        resumed = false
        hub.onSaid = null
        hub.onSpeechDone = null
        super.onPause()
    }

    override fun onDestroy() {
        hub.permissions.presenter = null
        voice.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_NEED) return
        val done = answer
        answer = null
        done?.invoke(grantResults.any { it == PackageManager.PERMISSION_GRANTED })
    }

    /** The microphone is asked the first time the person presses the button. */
    private fun listen() {
        if (!voice.available) {
            said.text = getString(R.string.no_recognizer)
            return
        }
        hub.permissions.ask(Need.MIC) { allowed -> if (allowed) voice.listen() }
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        return ComponentName(this, MessageListener::class.java).flattenToString() in enabled
    }

    private companion object {
        const val REQUEST_NEED = 1
        const val REQUEST_SCREENING = 2
        const val SCREENING = "screening"
        const val NOTIFICATIONS = "notifications"
    }
}
