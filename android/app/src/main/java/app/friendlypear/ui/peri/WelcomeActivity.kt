package app.friendlypear.ui.peri

import android.app.Activity
import android.app.LocaleManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.services.PeriMoment
import app.friendlypear.services.PeriView

/**
 * Peri's welcome, before any permission: who she is, what she does, that the family never spies, and that every
 * permission comes with a reason and can be refused. Four short pages with Peri herself (animated, not a picture).
 * She says only the first one aloud, so the first start isn't a lecture; each page has a button to hear it.
 */
class WelcomeActivity : Activity() {
    private data class Page(val title: Int, val text: Int, val mood: Mood, val moment: PeriMoment? = null)

    private val pages = listOf(
        Page(R.string.welcome_1_title, R.string.welcome_1_text, Mood.CALM),
        Page(R.string.welcome_2_title, R.string.welcome_2_text, Mood.PROTECTIVE),
        Page(R.string.welcome_3_title, R.string.welcome_3_text, Mood.HAPPY),
        Page(R.string.welcome_4_title, R.string.welcome_4_text, Mood.CALM, PeriMoment.THUMBS_UP),
    )
    private var index = 0
    private lateinit var face: PeriFace
    private lateinit var title: TextView
    private lateinit var text: TextView
    private lateinit var dots: LinearLayout
    private lateinit var next: Pill
    private lateinit var skip: TextView
    private val hub by lazy { PeriApp.hub(this) }

    private val view = object : PeriView {
        override fun speaking(on: Boolean) = face.speaking(on)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        index = savedInstanceState?.getInt(PAGE) ?: 0
        face = PeriFace(this).apply {
            isClickable = true
            setOnClickListener { listen() }
        }
        title = heading("", 30f).apply { gravity = Gravity.CENTER }
        text = body("", 20f).apply {
            gravity = Gravity.CENTER
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.2f)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        next = Pill(this, "", R.drawable.ic_chevron, primary = true, size = 22f).apply { setOnClickListener { forward() } }
        val hear = Pill(this, getString(R.string.welcome_listen), R.drawable.ic_mic, primary = false, size = 18f).apply {
            setOnClickListener { listen() }
        }
        skip = TextView(this).apply {
            text = getString(R.string.welcome_skip)
            sizeSp(17f)
            typeface = weight(600)
            setTextColor(Palette.GREEN_DARK)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
            isClickable = true
            setOnClickListener { done() }
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(languages(), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(skip)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.CREAM, Palette.MEADOW))
            clipChildren = false
            addView(top)
            addView(face, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
            addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            addView(text, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
            addView(dots, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(28)).apply { topMargin = dp(18) })
            addView(next, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)).apply { topMargin = dp(14) })
            addView(hear, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(12) })
        }
        val pad = dp(20)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(pad + bars.left, dp(8) + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        setContentView(root)
        show(first = savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()
        hub.attach(view)
    }

    override fun onPause() {
        hub.detach(view)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(PAGE, index)
    }

    /** "Español · English" at the top: the language is chosen before anything is said. Android 13+ only. */
    private fun languages(): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@apply
        val current = resources.configuration.locales[0].language
        listOf("es" to "Español", "en" to "English").forEach { (tag, label) ->
            val on = current == tag
            addView(TextView(this@WelcomeActivity).apply {
                text = label
                sizeSp(16f)
                typeface = weight(if (on) 700 else 500)
                setTextColor(if (on) Palette.WHITE else Palette.GREEN_DARK)
                background = pressable(rounded(if (on) Palette.GREEN_DARK else Palette.GREEN_TINT, dp(18).toFloat()), Palette.RIPPLE_DARK)
                setPadding(dp(14), dp(8), dp(14), dp(8))
                isClickable = true
                textLocale = java.util.Locale(tag)
                setOnClickListener {
                    if (!on) getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(tag)
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) })
        }
    }

    private fun show(first: Boolean = false) {
        val page = pages[index]
        title.text = getString(page.title)
        text.text = getString(page.text)
        face.show(page.mood)
        if (index == 0) face.greet()
        page.moment?.let { m -> face.postDelayed({ face.moment(m) }, 400) }
        next.label = getString(if (index == pages.lastIndex) R.string.welcome_start else R.string.welcome_next)
        skip.visibility = if (index == pages.lastIndex) View.INVISIBLE else View.VISIBLE
        dots.removeAllViews()
        pages.indices.forEach { i ->
            dots.addView(View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (i == index) Palette.GREEN_DARK else Palette.GREEN_TINT)
                }
            }, LinearLayout.LayoutParams(dp(if (i == index) 14 else 10), dp(if (i == index) 14 else 10)).apply { marginStart = dp(5); marginEnd = dp(5) })
        }
        dots.contentDescription = getString(R.string.welcome_page, index + 1, pages.size)
        listOf(title, text).forEachIndexed { i, v -> v.floatIn(40L + 60L * i) }
        // Only the hello is spoken by itself: the rest at the person's pace, or when they ask.
        if (first && index == 0) hub.say(getString(page.title) + ". " + getString(page.text))
    }

    private fun listen() = hub.say(title.text.toString() + ". " + text.text)

    private fun forward() {
        if (index == pages.lastIndex) return done()
        index++
        show()
    }

    private fun done() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(WELCOMED, true).apply()
        setResult(RESULT_OK)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (index > 0) {
            index--
            show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        private const val PAGE = "page"
        /** Same file as the permissions' answers: the welcome belongs to the first start. */
        const val PREFS = "permissions"
        const val WELCOMED = "welcomed"
    }
}
