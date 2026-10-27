package app.friendlypear.ui.peri

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.config.Need
import app.friendlypear.core.agent.Mood
import app.friendlypear.device.VoiceInput
import app.friendlypear.services.MessageListener
import app.friendlypear.services.PeriHub
import app.friendlypear.services.PeriView

/**
 * Peri's screen: Peri herself, what she says in a speech bubble, in large letters, and one big button to talk.
 * Above, a chip that says whether she's fully protecting the person; below, a card with the setup steps still
 * missing, if any. Permissions are asked one at a time, each with Peri's sentence first: the shield's basics at
 * first start, everything else (microphone, calls, texts, location) the first time it's needed.
 * "Mini Peri" switches to the other mode: only the pear, floating over other apps ([MiniPeriService]).
 */
class PeriActivity : Activity() {
    private lateinit var hub: PeriHub
    private lateinit var voice: VoiceInput
    private lateinit var said: TextView
    private lateinit var face: PeriFace
    private var listening = false
    private lateinit var talk: Pill
    private lateinit var status: TextView
    private lateinit var setup: LinearLayout
    private lateinit var access: View
    private lateinit var usage: View
    private lateinit var basics: View
    private lateinit var location: View
    private var pulse: ObjectAnimator? = null
    private var wantsMini = false
    /** Set while the app steps aside for Mini Peri, so the last resume before that doesn't put her away. */
    private var leaving = false
    private var resumed = false
    private var greeted = false
    private var answer: ((Boolean) -> Unit)? = null
    private val asked by lazy { getSharedPreferences("permissions", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hub = PeriApp.hub(this)
        voice = VoiceInput(this, hub.lang, onText = { hub.heard(it) }, onListening = { on ->
            listening = on
            showListening(on)
            showFace(hub.mood)
        }, mayListen = { !hub.inCall }, ears = hub.ears, onMissed = { hub.missedVoice() })
        face = PeriFace(this)

        said = TextView(this).apply {
            text = hub.lastSaid.ifEmpty { getString(R.string.peri_greeting) }
            textSize = 22f
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.12f)
            gravity = Gravity.CENTER
            maxLines = 7
            ellipsize = TextUtils.TruncateAt.END
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            val tail = dp(14).toFloat()
            background = BubbleDrawable(Palette.WHITE, 0, 0f, dp(26).toFloat(), tail, BubbleDrawable.Tail.TOP)
            elevation = dp(4).toFloat()
            setPadding(dp(22), dp(16) + tail.toInt(), dp(22), dp(18))
        }
        talk = Pill(this, getString(R.string.talk), R.drawable.ic_mic, primary = true, size = 24f).apply {
            setOnClickListener { listen() }
        }
        val family = Pill(this, getString(R.string.family_short), R.drawable.ic_family, primary = false, size = 19f).apply {
            setOnClickListener { startActivity(Intent(this@PeriActivity, FamilyActivity::class.java)) }
        }
        val mini = Pill(this, getString(R.string.mini_mode), R.drawable.ic_mini, primary = false, size = 19f).apply {
            setOnClickListener { goMini() }
        }
        val look = Pill(this, getString(R.string.picture_button), R.drawable.ic_image_search, primary = false, size = 19f).apply {
            setOnClickListener { startActivity(Intent(this@PeriActivity, PictureActivity::class.java)) }
        }
        status = TextView(this).apply {
            textSize = 15f
            typeface = weight(600)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(14), dp(6))
            compoundDrawablePadding = dp(8)
        }
        // The shield's basics, if one was turned down: asked again, or Android's app settings if it won't ask.
        basics = setupRow(R.drawable.ic_shield, getString(R.string.setup_basics)) { askBasics() }
        // Optional, asked calmly here rather than during an emergency.
        location = setupRow(R.drawable.ic_family, getString(R.string.setup_location)) {
            asked.edit().putBoolean(LOCATION_ASKED, true).apply()
            hub.permissions.ask(Need.LOCATION) { showSetup() }
        }
        access = setupRow(R.drawable.ic_notifications, getString(R.string.notification_access)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        // Usage access is a special setting: Peri explains it first, then opens the screen where it's granted.
        usage = setupRow(R.drawable.ic_bank, getString(R.string.usage_access)) {
            hub.say(getString(R.string.why_usage))
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        setup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            elevation = dp(2).toFloat()
            setPadding(dp(16), dp(12), dp(12), dp(8))
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.peri_shield)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(56), dp(56)))
                addView(heading(getString(R.string.setup_title), 18f).apply { setPadding(dp(10), 0, 0, 0) })
            })
            addView(basics)
            addView(access)
            addView(usage)
            addView(location)
        }

        val wordmark = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            typeface = weight(800)
            setTextColor(Palette.GREEN_DARK)
        }
        val settings = ImageView(this).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Palette.GREEN_DARK)
            contentDescription = getString(R.string.settings_title)
            background = pressable(GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Palette.GREEN_TINT)
            }, Palette.RIPPLE_DARK)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { startActivity(Intent(this@PeriActivity, SettingsActivity::class.java)) }
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(wordmark, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(status)
            addView(settings, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(10) })
        }
        val secondary = LinearLayout(this).apply {
            addView(family, LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginEnd = dp(6) })
            addView(mini, LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginStart = dp(6) })
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Cream like Peri's world, with a hint of meadow at the bottom; ink text on it is 14:1.
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.CREAM, Palette.MEADOW))
            clipChildren = false
            addView(header)
            addView(face, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
            addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(setup, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
            addView(talk, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(84)).apply { topMargin = dp(18) })
            addView(look, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(12) })
            addView(secondary, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        }
        // Edge to edge: the cream goes under the status and navigation bars, the content stays clear of them.
        val pad = dp(20)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(pad + bars.left, dp(8) + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        setContentView(root)

        // This screen shows Android's dialog whenever Peri needs a permission, now or later.
        hub.permissions.presenter = { need, done ->
            answer = done
            requestPermissions(need.permissions.toTypedArray(), REQUEST_NEED)
        }
        firstStart()
    }

    /** One missing setup step: an icon, what it's for in plain words, and a chevron. */
    private fun setupRow(icon: Int, label: String, onClick: () -> Unit): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(60)
        setPadding(dp(4), dp(6), dp(4), dp(6))
        background = pressable(rounded(Palette.WHITE, dp(16).toFloat()), Palette.RIPPLE_DARK)
        isClickable = true
        isFocusable = true
        contentDescription = label
        setOnClickListener { onClick() }
        addView(ImageView(this@PeriActivity).apply {
            setImageResource(icon)
            setColorFilter(Palette.GREEN_DARK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Palette.GREEN_TINT)
            }
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(body(label, 17f).apply {
            setTextColor(Palette.INK)
            setPadding(dp(14), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(ImageView(this@PeriActivity).apply {
            setImageResource(R.drawable.ic_chevron)
            setColorFilter(Palette.INK_SOFT)
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
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
        // Two modes, never both: opening the app puts Mini Peri away, unless she's about to come out or is
        // beside a call (the call screen brings the app to the front for a moment as it opens).
        if (wantsMini && Settings.canDrawOverlays(this)) return goMini()
        wantsMini = false
        if (MiniPeriService.running && !leaving && !MiniPeriService.forCall) MiniPeriService.stop(this)
        hub.attach(view)
        hub.appVisible = true
        showFace(hub.mood)
        face.speaking(hub.speaking)
        if (!greeted) {
            greeted = true
            face.greet()
        }
        showSetup()
        hub.refresh()
    }

    /** The chip and the setup card: what's still missing for Peri to protect the person fully. */
    private fun showSetup() {
        val noAccess = !hasNotificationAccess()
        val noUsage = !hub.bank.allowed()
        val noBasics = Need.FIRST_START.any { !hub.permissions.granted(it) }
        basics.visibility = if (noBasics) View.VISIBLE else View.GONE
        access.visibility = if (noAccess) View.VISIBLE else View.GONE
        usage.visibility = if (noUsage) View.VISIBLE else View.GONE
        val noLocation = !hub.permissions.granted(Need.LOCATION) && !asked.getBoolean(LOCATION_ASKED, false)
        location.visibility = if (noLocation) View.VISIBLE else View.GONE
        setup.visibility = if (noBasics || noAccess || noUsage || noLocation) View.VISIBLE else View.GONE
        val protecting = !noBasics && !noAccess
        status.text = getString(if (protecting) R.string.status_protecting else R.string.status_setup)
        status.setTextColor(if (protecting) Palette.GREEN_DARK else Palette.AMBER_INK)
        status.background = rounded(if (protecting) Palette.GREEN_TINT else Palette.AMBER_TINT, dp(20).toFloat())
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (protecting) Palette.GREEN else Palette.AMBER_INK)
            setSize(dp(10), dp(10))
        }
        status.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
    }

    private fun askBasics() {
        val need = Need.FIRST_START.firstOrNull { !hub.permissions.granted(it) } ?: return showSetup()
        val canAsk = need.permissions.any { shouldShowRequestPermissionRationale(it) } || !asked.getBoolean(need.name, false)
        if (!canAsk) {
            hub.say(getString(need.why))
            return startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        asked.edit().putBoolean(need.name, true).apply()
        hub.permissions.ask(need) {
            hub.refresh()
            showSetup()
        }
    }

    override fun onPause() {
        resumed = false
        hub.detach(view)
        hub.appVisible = false
        face.speaking(false)
        super.onPause()
    }

    override fun onStop() {
        leaving = false
        super.onStop()
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

    private val view = object : PeriView {
        override fun said(text: String) {
            said.text = text
        }

        override fun mood(mood: Mood) = showFace(mood)

        override fun speaking(on: Boolean) {
            face.speaking(on)
            if (!on && resumed && hub.isWaitingForAnswer && voice.available && hub.permissions.granted(Need.MIC)) voice.listen()
        }
    }

    /** While the microphone is open the button says so and breathes gently, so it's clear Peri is listening. */
    private fun showListening(on: Boolean) {
        talk.label = getString(if (on) R.string.listening else R.string.talk)
        pulse?.cancel()
        talk.scaleX = 1f
        talk.scaleY = 1f
        if (!on || Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return
        pulse = ObjectAnimator.ofPropertyValuesHolder(
            talk,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.03f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f),
        ).apply {
            duration = 650
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    /**
     * Mini Peri floats over other apps, which Android lets only after the person allows it in settings:
     * Peri explains why first. Then the microphone, so she can listen when tapped, and the app steps aside.
     */
    private fun goMini() {
        if (!Settings.canDrawOverlays(this)) {
            wantsMini = true
            hub.say(getString(R.string.why_overlay))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        wantsMini = false
        hub.permissions.ask(Need.MIC) {
            leaving = true
            MiniPeriService.start(this)
            moveTaskToBack(true)
        }
    }

    /** The agent's mood, plus what only the screen knows: the microphone is open, or it's night and all is calm. */
    private fun showFace(mood: Mood) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        face.show(
            when {
                listening -> Mood.LISTENING
                mood == Mood.CALM && (hour >= 22 || hour < 8) -> Mood.SLEEPING
                else -> mood
            },
        )
    }

    /** The microphone is asked the first time the person presses the button. */
    private fun listen() {
        if (!voice.available) {
            said.text = getString(R.string.no_recognizer)
            return
        }
        hub.permissions.ask(Need.MIC) { allowed -> if (allowed && !voice.listen()) hub.say(getString(R.string.no_mic_in_call)) }
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
        const val LOCATION_ASKED = "location_setup"
    }
}
