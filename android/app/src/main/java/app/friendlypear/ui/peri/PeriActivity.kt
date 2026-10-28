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
 * Peri's screen, in her world (a sky that follows the hour and her mood, and a place that changes every day), with
 * four sections along the bottom: Peri, her diary, learning with her and the family. Peri's own section: Peri
 * standing on her hill, what she says in a speech bubble, in large letters, and one big button to talk.
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
    private lateinit var shieldsChip: TextView
    private lateinit var report: View
    private lateinit var web: View
    private lateinit var webText: TextView
    private lateinit var setup: LinearLayout
    private lateinit var access: View
    private lateinit var usage: View
    private lateinit var basics: View
    private lateinit var location: View
    private lateinit var sms: View
    private lateinit var world: PeriWorld
    private lateinit var tabs: TabBar
    private lateinit var home: LinearLayout
    private lateinit var pages: android.widget.FrameLayout
    private lateinit var wordmark: TextView
    private var diary: DiaryPage? = null
    private var learn: LearnPage? = null
    private var family: FamilyPage? = null
    private var tab = TAB_PERI
    private var bars = android.graphics.Insets.NONE
    private lateinit var familyAlert: View
    private lateinit var familyAlertTitle: TextView
    private lateinit var familyAlertText: TextView
    private lateinit var question: View
    private lateinit var questionTitle: TextView
    private lateinit var questionText: TextView
    /** The "was it you?" on show (its id and who asks), or null. */
    private var asking: Pair<String, String>? = null
    /** The family alert on show (whose pear, and since when), or null. */
    private var familyAlertKey: String? = null
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
            if (on && !listening) hub.haptics.listening()
            listening = on
            showListening(on)
            showFace(hub.mood)
        }, mayListen = { !hub.inCall }, ears = hub.ears, onMissed = { hub.missedVoice() })
        face = PeriFace(this).apply {
            isClickable = true
            setOnClickListener { tapped() }
            setOnTouchListener(touch)
        }

        said = TextView(this).apply {
            text = hub.lastSaid.ifEmpty { getString(R.string.peri_greeting) }
            sizeSp(SAID_SP)
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.15f)
            maxLines = 7
            ellipsize = TextUtils.TruncateAt.END
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            val tail = dp(14).toFloat()
            background = BubbleDrawable(Palette.WHITE, Palette.LINE, dp(1).toFloat(), dp(24).toFloat(), tail, BubbleDrawable.Tail.TOP)
            setPadding(dp(22), dp(16) + tail.toInt(), dp(22), dp(18))
        }
        talk = Pill(this, getString(R.string.talk), R.drawable.ic_mic, primary = true, size = TextSize.TITLE).apply {
            setOnClickListener { listen() }
        }
        val mini = Pill(this, getString(R.string.mini_mode), R.drawable.ic_mini, primary = false).apply {
            setOnClickListener { goMini() }
        }
        val look = Pill(this, getString(R.string.picture_button), R.drawable.ic_image_search, primary = false).apply {
            setOnClickListener { startActivity(Intent(this@PeriActivity, PictureActivity::class.java)) }
        }
        status = TextView(this).apply {
            sizeSp(TextSize.SMALL)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            typeface = weight(600)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(6), dp(14), dp(6))
            compoundDrawablePadding = dp(8)
            // Tapping the chip shows what Peri can do and what she did.
            isClickable = true
            isFocusable = true
            setOnClickListener { startActivity(Intent(this@PeriActivity, AccessActivity::class.java)) }
        }
        // Her shields, beside her feet: how many the person earned. Tapping shows what for.
        shieldsChip = TextView(this).apply {
            sizeSp(TextSize.SMALL)
            typeface = weight(700)
            setTextColor(Palette.GREEN_DARK)
            gravity = Gravity.CENTER_VERTICAL
            background = pressable(rounded(Palette.WHITE, 999f, Palette.LINE_STRONG, dp(1.5f)), Palette.RIPPLE_DARK)
            setPadding(dp(10), dp(6), dp(14), dp(6))
            compoundDrawablePadding = dp(6)
            val icon = getDrawable(R.drawable.ic_shield)!!.mutate().apply {
                setTint(Palette.GREEN_DARK)
                setBounds(0, 0, dp(20), dp(20))
            }
            setCompoundDrawablesRelative(icon, null, null, null)
            isClickable = true
            isFocusable = true
            visibility = View.GONE
            setOnClickListener { showShields() }
        }
        // The shield's basics, if one was turned down: asked again, or Android's app settings if it won't ask.
        basics = setupRow(R.drawable.ic_shield, getString(R.string.setup_basics)) { askBasics() }
        // Texts help with codes, but Android may lock them for apps installed from a file: optional, can be hidden.
        sms = setupRow(R.drawable.ic_notifications, getString(R.string.setup_sms), dismissKey = DISMISS_SMS) { ask(Need.READ_SMS) }
        // Optional, asked calmly here rather than during an emergency.
        location = setupRow(R.drawable.ic_family, getString(R.string.setup_location)) {
            asked.edit().putBoolean(LOCATION_ASKED, true).apply()
            hub.permissions.ask(Need.LOCATION) { showSetup() }
        }
        val label = if (UnlockActivity.restricted(this)) R.string.setup_unlock else R.string.notification_access
        access = setupRow(R.drawable.ic_notifications, getString(label)) {
            // Installed from a file, Android locks this: the guide walks through unlocking it, step by step.
            if (UnlockActivity.restricted(this)) return@setupRow startActivity(Intent(this, UnlockActivity::class.java))
            hub.show(getString(R.string.why_notifications))
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        // Usage access is a special setting: Peri explains it first, then opens the screen where it's granted.
        usage = setupRow(R.drawable.ic_bank, getString(R.string.usage_access), dismissKey = DISMISS_USAGE) {
            hub.show(getString(R.string.why_usage))
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        setup = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(12), dp(12), dp(8))
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.peri_shield)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(heading(getString(R.string.setup_title), TextSize.SMALL).apply { setPadding(dp(8), 0, 0, 0) })
            })
            addView(basics)
            addView(sms)
            addView(access)
            addView(usage)
            addView(location)
        }

        // After a scam: "Want to report it?", with a button to have it written up, and a cross to hide it.
        report = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(12), dp(8), dp(14))
            visibility = View.GONE
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.peri_report)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
                addView(LinearLayout(this@PeriActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), 0, dp(4), 0)
                    addView(heading(getString(R.string.report_card_title), TextSize.BODY))
                    addView(body(getString(R.string.report_card_text), TextSize.SMALL))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Palette.INK_SOFT)
                    contentDescription = getString(R.string.report_hide)
                    background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    setOnClickListener {
                        hub.lastScam.hide()
                        showSetup()
                    }
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
            })
            // How the trick worked (three panels), or have it written up for the police.
            addView(LinearLayout(this@PeriActivity).apply {
                addView(Pill(this@PeriActivity, getString(R.string.trap_card_how), R.drawable.ic_image_search, primary = false).apply {
                    setOnClickListener { startActivity(Intent(this@PeriActivity, TrapActivity::class.java)) }
                }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(5) })
                addView(Pill(this@PeriActivity, getString(R.string.report_card_button), R.drawable.ic_message, primary = false).apply {
                    setOnClickListener { startActivity(Intent(this@PeriActivity, ReportActivity::class.java)) }
                }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginStart = dp(5) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
                marginEnd = dp(8)
            })
        }

        // A family member's Peri is on alert: their pear needs attention. It comes before every other card.
        familyAlertTitle = heading("", TextSize.BODY).apply { setTextColor(Palette.CORAL_INK) }
        familyAlertText = body(getString(R.string.family_alert_text), TextSize.SMALL).apply { setTextColor(Palette.INK) }
        familyAlert = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = tintedCard(Palette.CORAL_TINT, 0xFFEFB4A8.toInt())
            setPadding(dp(16), dp(12), dp(8), dp(14))
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(badge(R.drawable.ic_family, Palette.WHITE, Palette.CORAL_INK, 44), LinearLayout.LayoutParams(dp(44), dp(44)))
                addView(LinearLayout(this@PeriActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), 0, dp(4), 0)
                    addView(familyAlertTitle)
                    addView(familyAlertText)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Palette.INK_SOFT)
                    contentDescription = getString(R.string.report_hide)
                    background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    setOnClickListener {
                        asked.edit().putString(FAMILY_DISMISSED, familyAlertKey).apply()
                        familyAlertKey = null
                        showSetup()
                    }
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
            })
            addView(Pill(this@PeriActivity, getString(R.string.family_alert_button), R.drawable.ic_family, primary = false, rubber = Rubber.CORAL).apply {
                setOnClickListener { showTab(TAB_FAMILY) }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply {
                topMargin = dp(10)
                marginEnd = dp(8)
            })
        }

        // "Was it you?": another family phone got a message from a new number that says it's family.
        questionTitle = heading("", TextSize.BODY).apply { setTextColor(Palette.LILAC_INK) }
        questionText = body("", TextSize.SMALL).apply { setTextColor(Palette.INK) }
        question = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = tintedCard(Palette.LILAC_TINT, 0xFFCDBEF5.toInt())
            setPadding(dp(16), dp(12), dp(16), dp(14))
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(badge(R.drawable.ic_family, Palette.WHITE, Palette.LILAC_INK, 44), LinearLayout.LayoutParams(dp(44), dp(44)))
                addView(LinearLayout(this@PeriActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), 0, 0, 0)
                    addView(questionTitle)
                    addView(questionText)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            })
            addView(LinearLayout(this@PeriActivity).apply {
                addView(Pill(this@PeriActivity, getString(R.string.check_me), R.drawable.ic_check, primary = false, size = 17f).apply {
                    setOnClickListener { answerQuestion(me = true) }
                }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginEnd = dp(5) })
                addView(Pill(this@PeriActivity, getString(R.string.check_not_me), R.drawable.ic_close, primary = false, size = 17f).apply {
                    setOnClickListener { answerQuestion(me = false) }
                }, LinearLayout.LayoutParams(0, dp(56), 1f).apply { marginStart = dp(5) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        }

        // A website's junk notice: it isn't the phone talking, and its notices can be switched off in one tap.
        webText = body("", TextSize.SMALL)
        web = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(12), dp(8), dp(14))
            visibility = View.GONE
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.peri_shield)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
                addView(LinearLayout(this@PeriActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), 0, dp(4), 0)
                    addView(heading(getString(R.string.web_card_title), TextSize.BODY))
                    addView(webText)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.ic_close)
                    setColorFilter(Palette.INK_SOFT)
                    contentDescription = getString(R.string.report_hide)
                    background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    setOnClickListener {
                        hub.webNotices.hide()
                        showSetup()
                    }
                }, LinearLayout.LayoutParams(dp(44), dp(44)))
            })
            addView(Pill(this@PeriActivity, getString(R.string.web_card_button), R.drawable.ic_notifications, primary = false).apply {
                setOnClickListener { silenceWebsite() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply {
                topMargin = dp(10)
                marginEnd = dp(8)
            })
        }

        wordmark = TextView(this).apply {
            text = getString(R.string.app_name)
            sizeSp(TextSize.TITLE)
            maxLines = 1
            typeface = Fonts.display(700)
            setTextColor(Palette.GREEN_DARK)
        }
        val spacer = View(this)
        val logo = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = getString(R.string.app_name)
            visibility = View.GONE
        }
        val settings = ImageView(this).apply {
            setImageResource(R.drawable.ic_settings)
            setColorFilter(Palette.GREEN_DARK)
            contentDescription = getString(R.string.settings_title)
            // A round white button: it reads on any sky, by day and by night.
            background = pressable(rounded(Palette.WHITE, dp(24).toFloat()), Palette.RIPPLE_DARK)
            setPadding(dp(11), dp(11), dp(11), dp(11))
            setOnClickListener { startActivity(Intent(this@PeriActivity, SettingsActivity::class.java)) }
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(logo, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(8) })
            addView(wordmark, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            // The name shrinks to fit beside the chip; if it still can't, Peri's round face stands in for it.
            wordmark.post {
                wordmark.fitWidth(TextSize.TITLE, 17f)
                if (wordmark.paint.measureText(wordmark.text.toString()) > wordmark.width) {
                    wordmark.visibility = View.GONE
                    logo.visibility = View.VISIBLE
                    spacer.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
            }
            addView(status)
            addView(spacer, LinearLayout.LayoutParams(0, 0, 0f))
            addView(settings, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(10) })
        }
        val secondary = LinearLayout(this).apply {
            addView(look, LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginEnd = dp(6) })
            addView(mini, LinearLayout.LayoutParams(0, dp(60), 1f).apply { marginStart = dp(6) })
        }
        home = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            addView(header)
            addView(android.widget.FrameLayout(this@PeriActivity).apply {
                clipChildren = false
                addView(face, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
                addView(shieldsChip, android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.START,
                ).apply { bottomMargin = dp(10) })
                // Her wardrobe, beside her other foot: a hat, glasses, a bow tie...
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.ic_wardrobe)
                    setColorFilter(Palette.GREEN_DARK)
                    contentDescription = getString(R.string.wardrobe_open)
                    background = pressable(rounded(Palette.WHITE, dp(26).toFloat()), Palette.RIPPLE_DARK)
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    stateListAnimator = pressScale()
                    setOnClickListener { startActivity(Intent(this@PeriActivity, WardrobeActivity::class.java)) }
                }, android.widget.FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM or Gravity.END).apply { bottomMargin = dp(8) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
            addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(question, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            addView(familyAlert, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            addView(web, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            addView(report, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            addView(setup, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            addView(talk, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)).apply { topMargin = dp(14) })
            addView(secondary, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        }
        world = PeriWorld(this).apply { onTone = { applyTone(it) } }
        pages = android.widget.FrameLayout(this).apply {
            clipChildren = false
            addView(home, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        }
        tabs = TabBar(
            this,
            listOf(
                TabBar.Tab(R.drawable.ic_pear, getString(R.string.tab_peri), Palette.GREEN_TINT, Palette.GREEN_DARK),
                TabBar.Tab(R.drawable.ic_diary, getString(R.string.tab_diary), Palette.SUN_TINT, Palette.AMBER_INK),
                TabBar.Tab(R.drawable.ic_learn, getString(R.string.tab_learn), Palette.SKY_TINT, Palette.SKY_INK),
                TabBar.Tab(R.drawable.ic_family, getString(R.string.family_short), Palette.LILAC_TINT, Palette.LILAC_INK),
            ),
        ) { showTab(it) }
        tabs.select(TAB_PERI, animate = false)
        val root = android.widget.FrameLayout(this).apply {
            addView(world, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
            addView(pages, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
            addView(tabs, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, dp(TAB_BAR_DP), Gravity.BOTTOM).apply {
                leftMargin = dp(12)
                rightMargin = dp(12)
            })
        }
        // Edge to edge: the world goes under the status and navigation bars, the sections stay clear of them.
        root.setOnApplyWindowInsetsListener { _, insets ->
            bars = insets.getInsets(WindowInsets.Type.systemBars())
            padPages()
            tabs.layoutParams = (tabs.layoutParams as android.widget.FrameLayout.LayoutParams).apply { bottomMargin = bars.bottom + dp(8) }
            insets
        }
        // Wherever Peri ends up (a longer sentence makes her smaller), her hill rises to her feet.
        home.viewTreeObserver.addOnGlobalLayoutListener { placeGround() }
        setContentView(root)
        applyTone(world.dark)
        // The screen says hello: Peri first, then her bubble and the buttons, one by one.
        listOf(said, setup, talk, secondary).forEachIndexed { i, v -> v.floatIn(80L + 70L * i) }
        tabs.floatIn(360)
        if (savedInstanceState == null) face.post { face.enter() }

        // This screen shows Android's dialog whenever Peri needs a permission, now or later.
        hub.permissions.presenter = { need, done ->
            answer = done
            requestPermissions(need.permissions.toTypedArray(), REQUEST_NEED)
        }
        // The very first time, Peri introduces herself before asking for anything. Someone updating the app has
        // already answered her questions: no welcome for them.
        if (!asked.getBoolean(WelcomeActivity.WELCOMED, false) && asked.all.isNotEmpty()) {
            asked.edit().putBoolean(WelcomeActivity.WELCOMED, true).apply()
        }
        if (!asked.getBoolean(WelcomeActivity.WELCOMED, false)) {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(this, WelcomeActivity::class.java), REQUEST_WELCOME)
        } else {
            firstStart()
        }
    }

    /** The sections' scrolls clear the status bar at the top and the tab bar at the bottom. */
    private fun padPages() {
        val side = dp(20)
        val bottom = dp(TAB_BAR_DP) + bars.bottom + dp(8) + dp(14)
        home.setPadding(side + bars.left, dp(8) + bars.top, side + bars.right, bottom)
        listOfNotNull(diary, learn, family).forEach { it.setPadding(dp(16) + bars.left, dp(12) + bars.top, dp(16) + bars.right, bottom) }
    }

    /**
     * Another section: the one on show fades away and the new one rises into place, built again from what's on the
     * phone now. Peri's hills sink to make room, and come back to her feet on her section.
     */
    private fun showTab(next: Int) {
        if (next == tab) return
        val from = page(tab)
        val to = page(next)
        tab = next
        tabs.select(next)
        if (to is Page) {
            to.refresh()
            to.scrollTo(0, 0)
        }
        to.animate().cancel()
        from.animate().cancel()
        to.visibility = View.VISIBLE
        if (animationsOn()) {
            from.animate().alpha(0f).setDuration(110).withEndAction {
                from.visibility = View.GONE
                from.alpha = 1f
            }.start()
            to.alpha = 0f
            to.translationY = dp(18).toFloat()
            to.animate().alpha(1f).translationY(0f).setDuration(240).setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start()
        } else {
            from.visibility = View.GONE
            to.alpha = 1f
        }
        placeGround()
        if (next == TAB_PERI) showSetup()
    }

    private fun page(index: Int): View = when (index) {
        TAB_DIARY -> diary ?: DiaryPage(this, hub).also {
            diary = it
            addPage(it)
        }
        TAB_LEARN -> learn ?: LearnPage(this, hub) { startActivity(it) }.also {
            learn = it
            addPage(it)
        }
        TAB_FAMILY -> family ?: FamilyPage(this, hub) { startActivity(it) }.also {
            family = it
            addPage(it)
        }
        else -> home
    }

    private fun addPage(page: Page) {
        page.visibility = View.GONE
        pages.addView(page, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        padPages()
    }

    /** On Peri's section her hill reaches her feet; on the others it stays low, behind the tab bar. */
    private fun placeGround() {
        if (!::world.isInitialized || world.height == 0) return
        if (tab != TAB_PERI || face.height == 0) return world.setGround(world.height - dp(GROUND_LOW_DP).toFloat())
        val at = IntArray(2).also { face.getLocationInWindow(it) }
        val origin = IntArray(2).also { world.getLocationInWindow(it) }
        world.setGround(at[1] - origin[1] + face.feetY + dp(3))
    }

    /** By night the sky is dark: the name turns light, and so do the system bars' icons. */
    private fun applyTone(dark: Boolean) {
        wordmark.setTextColor(if (dark) Palette.CREAM else Palette.GREEN_DARK)
        val light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (dark) 0 else light, light)
    }

    /** Back from another section goes to Peri's; from hers, it leaves as usual. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (tab != TAB_PERI) return showTab(TAB_PERI)
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    /** What Peri says, at most about a quarter of the screen tall, so she always has room. */
    private fun showSaid(text: CharSequence) {
        said.removeCallbacks(typing)
        said.text = text
        said.post {
            val root = said.rootView.height.takeIf { it > 0 } ?: return@post
            said.fitInto((root * BUBBLE_SHARE).toInt(), SAID_SP, TextSize.SMALL)
            // New words appear bit by bit, as if she were saying them; the same sentence again just shows.
            if (text.toString() != typed && animationsOn()) type(text)
            typed = text.toString()
        }
    }

    private var typed = ""
    private var typing = Runnable { }

    /**
     * Reveals [text] from the start, hiding the rest with a transparent colour so the bubble keeps its size and
     * nothing jumps. TalkBack hears the whole sentence once, at the end.
     */
    private fun type(text: CharSequence) {
        val total = text.length
        if (total < 2) return
        val step = maxOf(1, total / TYPE_STEPS)
        var shown = 0
        said.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_NONE
        typing = object : Runnable {
            override fun run() {
                shown = minOf(total, shown + step)
                if (shown >= total) {
                    said.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                    said.text = text
                    return
                }
                said.text = android.text.SpannableString(text).apply {
                    setSpan(android.text.style.ForegroundColorSpan(android.graphics.Color.TRANSPARENT), shown, total, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                said.postDelayed(this, TYPE_MS)
            }
        }
        said.post(typing)
    }

    /** She glances towards a touch on the screen beside her. */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (ev.actionMasked == android.view.MotionEvent.ACTION_DOWN && ::face.isInitialized && face.width > 0) {
            val at = IntArray(2).also { face.getLocationOnScreen(it) }
            val onFace = ev.rawY >= at[1] && ev.rawY <= at[1] + face.height && kotlin.math.abs(ev.rawX - (at[0] + face.width / 2f)) < face.width * 0.25f
            val dx = ev.rawX - (at[0] + face.width / 2f)
            if (!onFace && kotlin.math.abs(dx) > dp(48)) face.lookToward(dx)
        }
        return super.dispatchTouchEvent(ev)
    }

    /** One missing setup step: an icon, what it's for in plain words, and a chevron. */
    private fun setupRow(icon: Int, label: String, dismissKey: String? = null, onClick: () -> Unit): View = LinearLayout(this).apply {
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
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginStart = dp(6) })
        addView(body(label).apply {
            setTextColor(Palette.INK)
            setPadding(dp(14), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(ImageView(this@PeriActivity).apply {
            setImageResource(R.drawable.ic_chevron)
            setColorFilter(Palette.INK_SOFT)
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
        // An optional step can be put away for good: Peri still protects without it.
        if (dismissKey != null) addView(ImageView(this@PeriActivity).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Palette.INK_SOFT)
            contentDescription = getString(R.string.setup_hide)
            background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener {
                asked.edit().putBoolean(dismissKey, true).apply()
                showSetup()
            }
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(4) })
    }

    /** The shield's basics, one at a time and only once each: a "no" is never asked again on every start. */
    private fun firstStart() {
        // Installed from a file, Android answers a request for texts with a frightening "denied" notice: skip it
        // here, and let the unlock guide (from the setup card) open texts and alerts together.
        val locked = UnlockActivity.restricted(this)
        val next = Need.FIRST_START.firstOrNull { !hub.permissions.granted(it) && !asked.getBoolean(it.name, false) && !(locked && it == Need.READ_SMS) }
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
            hub.show(getString(if (locked) R.string.why_notifications_locked else R.string.why_notifications))
        }
    }

    /** The call-screening role shows Peri the number before pickup. Asked once, with a sentence that says what for. */
    private fun askToScreenCalls(): Boolean {
        val roles = getSystemService(RoleManager::class.java) ?: return false
        if (!roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) || roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) return false
        if (asked.getBoolean(SCREENING, false)) return false
        asked.edit().putBoolean(SCREENING, true).apply()
        hub.show(getString(R.string.calls_why))
        startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQUEST_SCREENING)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENING) firstStart()
        if (requestCode == REQUEST_WELCOME) {
            // She already said hello in the welcome: no second greeting today.
            asked.edit().putInt(GREETED_DAY, Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).apply()
            firstStart()
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        // Peri may have spoken while the screen was hidden, e.g. during a call.
        if (hub.lastSaid.isNotEmpty()) showSaid(hub.lastSaid)
        // Two modes, never both: opening the app puts Mini Peri away, unless she's about to come out or is
        // beside a call (the call screen brings the app to the front for a moment as it opens).
        if (wantsMini && Settings.canDrawOverlays(this)) return goMini()
        wantsMini = false
        if (MiniPeriService.running && !leaving && !MiniPeriService.forCall) MiniPeriService.stop(this)
        hub.attach(view)
        hub.appVisible = true
        showFace(hub.mood)
        face.speaking(hub.speaking)
        val now = System.currentTimeMillis()
        val away = now - asked.getLong(LAST_SEEN, now)
        if ((!greeted || away > PEEKABOO_MS) && asked.getBoolean(WelcomeActivity.WELCOMED, false)) {
            val first = !greeted
            greeted = true
            face.greet()
            greet(away, first)
        }
        world.refreshPlace()
        face.outfit = Outfit.load(this)
        hub.publishPear()
        familyAlert.post(pollFamily)
        if (tab != TAB_PERI) (page(tab) as? Page)?.refresh()
        sensors?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)?.let { sensors?.registerListener(shake, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
        showShieldCount(hub.shields.total, pop = false)
        congratulate()
        showSetup()
        hub.refresh()
        // Back from the unlock guide: texts can be asked for normally now.
        if (asked.getBoolean(UnlockActivity.ASK_TEXTS, false)) {
            asked.edit().putBoolean(UnlockActivity.ASK_TEXTS, false).apply()
            if (!hub.permissions.granted(Need.READ_SMS)) {
                asked.edit().putBoolean(Need.READ_SMS.name, false).apply()
                ask(Need.READ_SMS)
            }
        }
    }

    /**
     * Peri's hello, by the time of day and a little different each time: a big hello after days apart, a peekaboo
     * after hours, her scarf on winter mornings, and her cape once all is calm after she stopped a scam. It's written
     * every time the app opens, and said aloud only the first time each day: a friend, not a chatterbox.
     */
    private fun greet(away: Long, first: Boolean) {
        if (hub.isWaitingForAnswer || hub.mood != Mood.CALM) return
        // A fresh start keeps what she last said (a warning still matters); after hours away she says hello anew.
        if (hub.lastSaid.isNotEmpty() && away <= PEEKABOO_MS && first) return
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val winter = cal.get(Calendar.MONTH) in setOf(Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY)
        val scam = hub.lastScam.load()
        val hero = scam != null && asked.getLong(HERO_FOR, 0L) != scam.at && System.currentTimeMillis() - scam.at > HERO_AFTER_MS
        val list = when {
            hero -> R.array.peri_hero
            away > RETURN_MS -> R.array.greet_return
            away > PEEKABOO_MS -> R.array.peri_peekaboo
            winter && hour in 7..12 -> R.array.greet_winter
            hour in 7..12 -> R.array.greet_morning
            hour in 13..19 -> R.array.greet_afternoon
            hour in 20..21 -> R.array.greet_evening
            else -> R.array.greet_night
        }
        if (hero) asked.edit().putLong(HERO_FOR, scam!!.at).apply()
        val text = Lines.next(this, list) + (shieldsLine()?.let { " $it" } ?: "")
        // At night she says hello with a yawn instead of a wave; back after a while, with her heart.
        when (list) {
            R.array.greet_night -> face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.YAWN) }, YAWN_DELAY_MS)
            R.array.peri_peekaboo, R.array.greet_return -> face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.PEEKABOO) }, YAWN_DELAY_MS)
            R.array.peri_hero -> face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.HERO) }, YAWN_DELAY_MS)
            R.array.greet_winter -> face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.SCARF) }, YAWN_DELAY_MS)
        }
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        if (asked.getInt(GREETED_DAY, -1) != today) {
            asked.edit().putInt(GREETED_DAY, today).apply()
            hub.say(text)
        } else {
            hub.show(text)
        }
    }

    /**
     * The browser's own switch for that one website's notices (browsers give each site a channel), or, if this
     * phone can't open it, the browser's notification settings. Peri says what to do there first.
     */
    private fun silenceWebsite() {
        val notice = hub.webNotices.offer() ?: return showSetup()
        hub.show(getString(R.string.web_how_off, notice.site ?: getString(R.string.web_unknown_site)))
        val channel = notice.channel?.let {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, notice.browser)
                .putExtra(Settings.EXTRA_CHANNEL_ID, it)
        }
        val browser = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, notice.browser)
        val opened = listOfNotNull(channel, browser).any { runCatching { startActivity(it) }.isSuccess }
        if (opened) {
            hub.webNotices.hide()
            hub.earn(app.friendlypear.config.Shields.Kind.STOPPED)
        }
    }

    /** "By the way, you've earned a new shield": only when all is calm, never right after a warning. */
    private fun shieldsLine(): String? {
        val n = hub.shields.unseen
        if (n == 0 || hub.mood != Mood.CALM || hub.isWaitingForAnswer) return null
        hub.shields.seen()
        face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.THUMBS_UP) }, CONGRATULATE_DELAY_MS)
        return resources.getQuantityString(R.plurals.shields_new, n, n)
    }

    private fun congratulate() {
        if (hub.speaking) return
        shieldsLine()?.let { hub.say(it) }
    }

    private fun showShieldCount(total: Int, pop: Boolean) {
        shieldsChip.visibility = if (total > 0) View.VISIBLE else View.GONE
        shieldsChip.text = java.text.NumberFormat.getIntegerInstance().format(total)
        shieldsChip.contentDescription = resources.getQuantityString(R.plurals.shields_chip, total, total)
        if (!pop || !animationsOn()) return
        shieldsChip.scaleX = 0.5f
        shieldsChip.scaleY = 0.5f
        shieldsChip.animate().scaleX(1f).scaleY(1f).setDuration(420).setInterpolator(android.view.animation.OvershootInterpolator(3f)).start()
        face.moment(app.friendlypear.services.PeriMoment.MEDAL)
    }

    /** What each shield was for, in a small card over the screen. */
    private fun showShields() {
        val s = hub.shields
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(4))
            addView(LinearLayout(this@PeriActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(this@PeriActivity).apply {
                    setImageResource(R.drawable.peri_shield)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(dp(72), dp(72)))
                addView(heading(getString(R.string.shields_title)).apply { setPadding(dp(10), 0, 0, 0) })
            })
            addView(body(getString(R.string.shields_intro), TextSize.SMALL).apply { setPadding(0, dp(8), 0, dp(6)) })
            listOf(
                app.friendlypear.config.Shields.Kind.STOPPED to R.string.shields_stopped,
                app.friendlypear.config.Shields.Kind.ASKED to R.string.shields_asked,
                app.friendlypear.config.Shields.Kind.FAMILY to R.string.shields_family,
                app.friendlypear.config.Shields.Kind.LEARNED to R.string.shields_learned,
            ).forEach { (kind, label) ->
                addView(LinearLayout(this@PeriActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(8), 0, dp(8))
                    addView(body(getString(label)).apply { setTextColor(Palette.INK) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    addView(heading(s.count(kind).toString(), TextSize.BODY).apply {
                        setTextColor(Palette.GREEN_DARK)
                        fontFeatureSettings = "tnum"
                    })
                })
            }
        }
        android.app.AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton(R.string.shields_close, null)
            .show()
            .friendly()
    }

    private val taps = ArrayDeque<Long>()
    private val single = Runnable { tappedOnce() }
    private var round = 0

    /** Each kind of answer goes round its own shuffled list, so nothing repeats until all were heard. */
    private fun next(array: Int): String = Lines.next(this, array)

    /**
     * Tapping Peri. One tap (she waits a moment, in case more follow): where you touched her (the leaf, her
     * tummy, her feet) and, on her tummy, a tip, a joke, a fact or a kind word in turn, each with its own move.
     * Three quick taps make her dizzy and spin; five or more and she gives up laughing and dances.
     */
    private fun tapped() {
        if (hub.isWaitingForAnswer) return
        val now = android.os.SystemClock.uptimeMillis()
        taps.addLast(now)
        while (taps.isNotEmpty() && now - taps.first() > TAP_STREAK_MS) taps.removeFirst()
        face.removeCallbacks(single)
        // While she's talking, taps only get a hop: she won't talk over herself.
        if (hub.speaking) return face.play(PeriFace.Trick.HOP)
        when {
            taps.size >= 5 -> {
                taps.clear()
                face.play(PeriFace.Trick.DANCE)
                face.sparkle()
                hub.say(next(R.array.peri_giveup))
            }
            taps.size == 3 -> {
                face.play(PeriFace.Trick.SPIN)
                // Once the spin is over, her eyes are still going round.
                face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.DIZZY) }, SPIN_SETTLE_MS)
                hub.say(next(R.array.peri_dizzy))
            }
            taps.size == 2 -> face.play(PeriFace.Trick.HOP)
            else -> face.postDelayed(single, TAP_WAIT_MS)
        }
    }

    private fun tappedOnce() {
        if (hub.isWaitingForAnswer || hub.speaking) return
        val at = face.lastTouch
        when {
            at < 0.3f -> {
                face.play(PeriFace.Trick.WINK)
                hub.say(next(R.array.peri_tap_head))
            }
            at > 0.8f -> {
                face.play(PeriFace.Trick.DANCE)
                hub.say(next(R.array.peri_tap_feet))
            }
            else -> when (round++ % 4) {
                0 -> {
                    face.moment(app.friendlypear.services.PeriMoment.IDEA)
                    hub.say(next(R.array.peri_taps) + " " + next(R.array.peri_tips))
                }
                1 -> {
                    face.moment(app.friendlypear.services.PeriMoment.LAUGH)
                    hub.say(next(R.array.peri_jokes))
                }
                2 -> {
                    face.moment(app.friendlypear.services.PeriMoment.THUMBS_UP)
                    hub.say(next(R.array.peri_facts))
                }
                else -> {
                    face.moment(app.friendlypear.services.PeriMoment.LOVE)
                    hub.say(next(R.array.peri_kind))
                }
            }
        }
    }

    /**
     * Playing with Peri, beyond taps: hold her and she gets the giggles, drag her and she comes back with a bounce,
     * rub her leaf and she melts, shake the phone and she gets dizzy. Never while she's warning or waiting for an
     * answer: the danger comes first.
     */
    private val touch = object : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var lastX = 0f
        private var direction = 0f
        private var turns = 0
        private var onLeaf = false
        private var dragging = false
        private var tickling = false
        private var rubbed = false
        private val slop by lazy { android.view.ViewConfiguration.get(this@PeriActivity).scaledTouchSlop }
        private val tickle = Runnable {
            if (!playful()) return@Runnable
            tickling = true
            hub.haptics.listening()
            face.moment(app.friendlypear.services.PeriMoment.LAUGH)
            face.play(PeriFace.Trick.DANCE)
            if (!hub.speaking) hub.say(next(R.array.peri_tickle))
        }

        override fun onTouch(v: View, e: android.view.MotionEvent): Boolean {
            when (e.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    lastX = e.x
                    direction = 0f
                    turns = 0
                    dragging = false
                    tickling = false
                    rubbed = false
                    onLeaf = face.lastTouch < LEAF_TOP
                    face.postDelayed(tickle, TICKLE_MS)
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    // Rubbing her leaf: the finger goes back and forth on top of her head.
                    if (onLeaf && !dragging && !rubbed) {
                        val step = e.x - lastX
                        if (kotlin.math.abs(step) > slop / 3f) {
                            val now = Math.signum(step)
                            if (direction != 0f && now != direction) turns++
                            direction = now
                            lastX = e.x
                        }
                        if (turns >= LEAF_RUBS && playful()) {
                            rubbed = true
                            face.removeCallbacks(tickle)
                            face.moment(app.friendlypear.services.PeriMoment.LOVE)
                            if (!hub.speaking) hub.say(next(R.array.peri_leaf))
                        }
                    }
                    val far = kotlin.math.hypot(dx, dy) > slop * (if (onLeaf) 4 else 2)
                    if (far) face.removeCallbacks(tickle)
                    if (!dragging && !tickling && !rubbed && far && playful() && animationsOn()) {
                        dragging = true
                        face.moment(app.friendlypear.services.PeriMoment.SURPRISED)
                    }
                    if (dragging) face.drag(dx, dy)
                }
                android.view.MotionEvent.ACTION_UP -> {
                    face.removeCallbacks(tickle)
                    val moved = kotlin.math.hypot(e.rawX - downX, e.rawY - downY)
                    when {
                        dragging -> {
                            face.release()
                            face.postDelayed({ face.play(PeriFace.Trick.HOP) }, HOME_MS)
                            if (!hub.speaking) hub.say(next(R.array.peri_drag))
                        }
                        tickling || rubbed -> Unit
                        moved < slop * 2 -> v.performClick()
                    }
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    face.removeCallbacks(tickle)
                    if (dragging) face.release()
                }
            }
            return true
        }
    }

    /** Peri plays only when all is calm (or she's asleep), never in the middle of a warning or a question. */
    private fun playful(): Boolean = tab == TAB_PERI && !hub.isWaitingForAnswer && hub.mood in PLAYFUL

    /** Shaking the phone (three strong jolts within a second) makes her spin and get dizzy. */
    private val shake = object : android.hardware.SensorEventListener {
        private val jolts = ArrayDeque<Long>()
        private var lastJolt = 0L
        private var lastShake = 0L

        override fun onSensorChanged(event: android.hardware.SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val g = kotlin.math.sqrt(x * x + y * y + z * z) / android.hardware.SensorManager.GRAVITY_EARTH
            if (g < SHAKE_G) return
            val now = android.os.SystemClock.uptimeMillis()
            if (now - lastJolt < JOLT_GAP_MS) return
            lastJolt = now
            jolts.addLast(now)
            while (jolts.isNotEmpty() && now - jolts.first() > 1_000) jolts.removeFirst()
            if (jolts.size < 3 || now - lastShake < 4_000 || !playful()) return
            lastShake = now
            jolts.clear()
            face.play(PeriFace.Trick.SPIN)
            face.postDelayed({ face.moment(app.friendlypear.services.PeriMoment.DIZZY) }, SPIN_SETTLE_MS)
            if (!hub.speaking) hub.say(next(R.array.peri_shake))
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit
    }

    private val sensors by lazy { getSystemService(android.hardware.SensorManager::class.java) }

    /** While Peri's screen is open, a look at the family's pears every half minute. */
    private val pollFamily = object : Runnable {
        override fun run() {
            checkFamily()
            familyAlert.postDelayed(this, FAMILY_POLL_MS)
        }
    }

    /**
     * A family member's pear needs attention (their Peri is warning them, or waiting for the family): a card here,
     * and Peri says so once per alert. Hiding the card hides that alert only.
     */
    private fun checkFamily() {
        if (!hub.seesFamilyPears) return
        hub.openChecks { checks ->
            if (checks == null || !resumed) return@openChecks
            val next = checks.firstOrNull { it.kind == "new_number" }
            asking = next?.let { it.id to it.from.ifEmpty { getString(R.string.check_someone) } }
            val (id, from) = asking ?: return@openChecks showSetup()
            questionTitle.text = getString(R.string.check_question_title, from)
            questionText.text = getString(R.string.check_question_text, from)
            showSetup()
            if (id != asked.getString(CHECK_SAID, null)) {
                asked.edit().putString(CHECK_SAID, id).apply()
                hub.haptics.mood(Mood.WORRIED)
                hub.say(getString(R.string.check_question_says, from))
            }
        }
        hub.familyPears { family ->
            if (family == null || !resumed) return@familyPears
            // The same trap reaching several of the family comes first: everyone should know.
            val campaign = family.campaign
            val alert = family.pears.firstOrNull { !it.self && it.state == "attention" }
            // One alert per family member in trouble (or per campaign), said once: their phone sending its pear again
            // doesn't repeat it. Once all is calm, the next one is new.
            val key = when {
                campaign != null -> "campaign:$campaign"
                alert != null -> "pear:${alert.id}"
                else -> null
            }
            if (key == null) asked.edit().remove(FAMILY_SAID).remove(FAMILY_DISMISSED).apply()
            if (key == null || key == asked.getString(FAMILY_DISMISSED, null)) {
                familyAlertKey = null
                return@familyPears showSetup()
            }
            familyAlertKey = key
            if (campaign != null) {
                familyAlertTitle.text = getString(R.string.campaign_title)
                familyAlertText.text = getString(R.string.campaign_text, getString(TRAP_NAMES[campaign] ?: R.string.trap_name_link).lowercase())
            } else if (alert != null) {
                familyAlertTitle.text = getString(R.string.family_alert_title, alert.name)
                familyAlertText.text = getString(R.string.family_alert_text)
            }
            showSetup()
            if (key != asked.getString(FAMILY_SAID, null)) {
                asked.edit().putString(FAMILY_SAID, key).apply()
                hub.haptics.mood(Mood.WORRIED)
                hub.say(if (campaign != null) getString(R.string.campaign_says) else getString(R.string.family_alert_says, alert!!.name))
            }
        }
    }

    /** The answer to "was it you?" goes back to the one who asked; Peri says thanks. */
    private fun answerQuestion(me: Boolean) {
        val (id, from) = asking ?: return
        asking = null
        showSetup()
        hub.answerCheck(id, me) { sent ->
            if (sent) hub.say(getString(R.string.check_thanks, from))
        }
    }

    /** The chip and the setup card: what's still missing for Peri to protect the person fully. */
    private fun showSetup() {
        val noAccess = !hasNotificationAccess()
        val noUsage = !hub.bank.allowed() && !asked.getBoolean(DISMISS_USAGE, false)
        val noBasics = BASICS.any { !hub.permissions.granted(it) }
        val noSms = !hub.permissions.granted(Need.READ_SMS) && !asked.getBoolean(DISMISS_SMS, false)
        val noLocation = !hub.permissions.granted(Need.LOCATION) && !asked.getBoolean(LOCATION_ASKED, false)
        // One step at a time, the most important first: a card with every step would squeeze Peri off the screen.
        val steps = listOf(noBasics to basics, noAccess to access, noSms to sms, noUsage to usage, noLocation to location)
        val first = steps.firstOrNull { it.first }?.second
        steps.forEach { (_, row) -> row.visibility = if (row == first) View.VISIBLE else View.GONE }
        // A family question first, then family in trouble, then a website to silence, then a scam to report: one card
        // at a time, so Peri keeps her room.
        val asked = asking != null
        question.visibility = if (asked) View.VISIBLE else View.GONE
        val family = !asked && familyAlertKey != null
        familyAlert.visibility = if (family) View.VISIBLE else View.GONE
        val site = hub.webNotices.offer()?.takeIf { !family && !asked }
        web.visibility = if (site != null) View.VISIBLE else View.GONE
        if (site != null) webText.text = getString(R.string.web_card_text, site.site ?: getString(R.string.web_unknown_site))
        val reporting = !family && !asked && site == null && hub.lastScam.offer() != null
        report.visibility = if (reporting) View.VISIBLE else View.GONE
        setup.visibility = if (first != null && !reporting && site == null && !family && !asked) View.VISIBLE else View.GONE
        // Contacts and calls, plus a way to see messages (texts or alerts): that's enough to protect.
        val seesMessages = hub.permissions.granted(Need.READ_SMS) || !noAccess
        val protecting = !noBasics && seesMessages
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
        val need = BASICS.firstOrNull { !hub.permissions.granted(it) } ?: return showSetup()
        ask(need)
    }

    /** Asks for [need] again, or, if Android won't show its dialog any more, explains and opens the app's settings. */
    private fun ask(need: Need) {
        // Installed from outside a store, Android 13+ locks texts and notification access ("restricted setting").
        if (need == Need.READ_SMS && UnlockActivity.restricted(this)) return startActivity(Intent(this, UnlockActivity::class.java))
        val canAsk = need.permissions.any { shouldShowRequestPermissionRationale(it) } || !asked.getBoolean(need.name, false)
        if (!canAsk) {
            hub.show(getString(need.why) + " " + getString(R.string.setup_restricted))
            return startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        asked.edit().putBoolean(need.name, true).apply()
        hub.permissions.ask(need) {
            hub.refresh()
            showSetup()
        }
    }

    override fun onPause() {
        sensors?.unregisterListener(shake)
        familyAlert.removeCallbacks(pollFamily)
        resumed = false
        asked.edit().putLong(LAST_SEEN, System.currentTimeMillis()).apply()
        // Off screen (the call screen, another app), Peri doesn't keep listening.
        voice.stop()
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
        override fun said(text: String) = showSaid(text)

        override fun mood(mood: Mood) = showFace(mood)

        override fun moment(moment: app.friendlypear.services.PeriMoment) = face.moment(moment)

        override fun callStarted() = voice.stop()

        override fun shieldEarned(total: Int) = showShieldCount(total, pop = true)

        override fun scamToReport() = showSetup()

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
            hub.show(getString(R.string.why_overlay))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        wantsMini = false
        hub.permissions.ask(Need.MIC) {
            leaving = true
            face.moment(app.friendlypear.services.PeriMoment.BYE)
            hub.say(next(R.array.peri_bye))
            MiniPeriService.start(this)
            moveTaskToBack(true)
        }
    }

    /**
     * The agent's mood, plus what only the screen knows: the microphone is open, or it's night and all is calm.
     * Her world shows it too: a coral veil while she warns, confetti when she's happy, night while she sleeps.
     */
    private fun showFace(mood: Mood) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val shown = when {
            listening -> Mood.LISTENING
            mood == Mood.CALM && (hour >= 22 || hour < 7) -> Mood.SLEEPING
            else -> mood
        }
        face.show(shown)
        if (::world.isInitialized) world.mood(shown)
    }

    /** The microphone is asked the first time the person presses the button. */
    private fun listen() {
        if (!voice.available) {
            showSaid(getString(R.string.no_recognizer))
            return
        }
        hub.permissions.ask(Need.MIC) { allowed -> if (allowed && !voice.listen()) hub.say(getString(R.string.no_mic_in_call)) }
    }

    private fun hasNotificationAccess(): Boolean = UnlockActivity.hasNotificationAccess(this)

    private companion object {
        const val REQUEST_NEED = 1
        const val SAID_SP = TextSize.BODY
        const val TAP_STREAK_MS = 1_600L
        /** A single tap waits this long for a second one before Peri answers it. */
        const val TAP_WAIT_MS = 380L
        /** About when her spin ends and she can stand there, dizzy. */
        const val SPIN_SETTLE_MS = 950L
        const val YAWN_DELAY_MS = 700L
        const val CONGRATULATE_DELAY_MS = 1_800L
        const val TYPE_MS = 22L
        /** However long the sentence, it's all there in about this many steps (~2 s). */
        const val TYPE_STEPS = 90
        const val BUBBLE_SHARE = 0.24f
        const val REQUEST_SCREENING = 2
        const val REQUEST_WELCOME = 3
        const val SCREENING = "screening"
        const val NOTIFICATIONS = "notifications"
        const val LOCATION_ASKED = "location_setup"
        const val GREETED_DAY = "greeted_day"
        const val LAST_SEEN = "last_seen"
        const val HERO_FOR = "hero_for"
        /** Away this long, she says "peekaboo"; this long, she missed the person. */
        const val PEEKABOO_MS = 3 * 60 * 60_000L
        const val RETURN_MS = 2 * 24 * 60 * 60_000L
        /** Her cape comes out once things have calmed down after a scam, not in the middle of it. */
        const val HERO_AFTER_MS = 30 * 60_000L
        /** Touches above this share of her picture are on her leaf. */
        const val LEAF_TOP = 0.3f
        const val LEAF_RUBS = 3
        const val TICKLE_MS = 550L
        /** About when she's back in her place after being dragged. */
        const val HOME_MS = 420L
        const val SHAKE_G = 2.3f
        const val JOLT_GAP_MS = 120L
        val PLAYFUL = setOf(Mood.CALM, Mood.HAPPY, Mood.SLEEPING)
        const val FAMILY_POLL_MS = 30_000L
        const val CHECK_SAID = "check_said"
        val TRAP_NAMES = mapOf(
            "parcel" to R.string.trap_name_parcel,
            "link" to R.string.trap_name_link,
            "code" to R.string.trap_name_code,
            "family" to R.string.trap_name_family,
            "remote" to R.string.trap_name_remote,
            "prize" to R.string.trap_name_prize,
        )
        const val FAMILY_SAID = "family_said"
        const val FAMILY_DISMISSED = "family_dismissed"
        const val TAB_PERI = 0
        const val TAB_DIARY = 1
        const val TAB_LEARN = 2
        const val TAB_FAMILY = 3
        const val TAB_BAR_DP = 76
        /** Off Peri's section, how far above the bottom her hills stay. */
        const val GROUND_LOW_DP = 92
        const val DISMISS_SMS = "dismiss_sms"
        const val DISMISS_USAGE = "dismiss_usage"
        /** What Peri can't protect without: who is family, and whether a call is going on. */
        val BASICS = listOf(Need.CONTACTS, Need.PHONE_STATE)
    }
}
