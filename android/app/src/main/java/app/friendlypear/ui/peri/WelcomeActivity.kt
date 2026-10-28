package app.friendlypear.ui.peri

import android.app.Activity
import android.app.LocaleManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.services.PeriMoment
import app.friendlypear.services.PeriView
import kotlin.math.abs

/**
 * Peri's welcome, before any permission: who she is, what she does, that the family never spies, that she can be
 * dressed up, and that every permission comes with a reason and can be refused. Five short pages with Peri herself
 * (animated, not a picture) standing on her hill in her world, the words on a card. She says only the first one
 * aloud, so the first start isn't a lecture; each page has a button to hear it. On the wardrobe page she shows off
 * a few outfits by herself.
 */
class WelcomeActivity : Activity() {
    private data class Page(val title: Int, val text: Int, val mood: Mood, val moment: PeriMoment? = null)

    private val pages = listOf(
        Page(R.string.welcome_1_title, R.string.welcome_1_text, Mood.CALM),
        Page(R.string.welcome_2_title, R.string.welcome_2_text, Mood.PROTECTIVE),
        Page(R.string.welcome_3_title, R.string.welcome_3_text, Mood.HAPPY),
        Page(R.string.welcome_dress_title, R.string.welcome_dress_text, Mood.CALM),
        Page(R.string.welcome_4_title, R.string.welcome_4_text, Mood.CALM, PeriMoment.THUMBS_UP),
    )
    private val dressPage = 3
    private lateinit var world: PeriWorld
    private lateinit var dressNow: View
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
        title = heading("").apply { gravity = Gravity.CENTER }
        text = body("").apply {
            gravity = Gravity.CENTER
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.2f)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        next = Pill(this, "", R.drawable.ic_chevron, primary = true, trailing = true).apply { setOnClickListener { forward() } }
        val hear = Pill(this, getString(R.string.welcome_listen), R.drawable.ic_mic, primary = false).apply {
            setOnClickListener { listen() }
        }
        skip = TextView(this).apply {
            text = getString(R.string.welcome_skip)
            sizeSp(TextSize.SMALL)
            typeface = weight(600)
            setTextColor(Palette.GREEN_DARK)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            // On a white pill, so it reads on any sky.
            background = pressable(rounded(Palette.WHITE, dp(20).toFloat()), Palette.RIPPLE_DARK)
            isClickable = true
            setOnClickListener { done() }
        }
        dressNow = chipButton(getString(R.string.welcome_dress_now)) { startActivity(android.content.Intent(this, WardrobeActivity::class.java)) }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(languages(), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(skip)
        }
        // The words on a card: they read the same on any sky and any hill.
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(12))
            addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(text, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            addView(dressNow, LinearLayout.LayoutParams(dp(200), dp(46)).apply { topMargin = dp(12) })
            addView(dots, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(28)).apply { topMargin = dp(10) })
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(top)
            addView(face, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
            addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            addView(next, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)).apply { topMargin = dp(14) })
            addView(hear, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(10) })
        }
        world = PeriWorld(this)
        val root = android.widget.FrameLayout(this).apply {
            addView(world, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
            addView(column, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val pad = dp(20)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            column.setPadding(pad + bars.left, dp(8) + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        // Peri stands on her hill, wherever the card leaves her.
        column.viewTreeObserver.addOnGlobalLayoutListener {
            if (face.height == 0 || world.height == 0) return@addOnGlobalLayoutListener
            val at = IntArray(2).also { face.getLocationInWindow(it) }
            val origin = IntArray(2).also { world.getLocationInWindow(it) }
            world.setGround(at[1] - origin[1] + face.feetY + dp(3))
        }
        setContentView(root)
        show(first = savedInstanceState == null)
    }

    override fun onResume() {
        super.onResume()
        hub.attach(view)
        // Back from the wardrobe: she wears what was chosen there.
        if (index != dressPage) face.outfit = Outfit.load(this)
    }

    override fun onPause() {
        hub.detach(view)
        face.removeCallbacks(fashion)
        super.onPause()
    }

    /** On the wardrobe page she tries one outfit after another, with a hop, until the page turns. */
    private val fashion = object : Runnable {
        override fun run() {
            if (index != dressPage) return
            face.outfit = Outfit.random()
            face.play(PeriFace.Trick.HOP)
            face.postDelayed(this, FASHION_MS)
        }
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
                sizeSp(TextSize.SMALL)
                typeface = weight(if (on) 700 else 500)
                setTextColor(if (on) Palette.WHITE else Palette.GREEN_DARK)
                background = pressable(
                    if (on) rounded(Palette.GREEN_DARK, dp(20).toFloat()) else rounded(Palette.WHITE, dp(20).toFloat(), Palette.LINE_STRONG, dp(1.5f)),
                    Palette.RIPPLE_DARK,
                )
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
        face.removeCallbacks(fashion)
        face.outfit = Outfit.load(this)
        dressNow.visibility = if (index == dressPage) View.VISIBLE else View.GONE
        if (index == dressPage) face.post(fashion)
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

    /** A swipe turns the page, like a book: left for the next one, right to go back. Never past the last. */
    private val swipe by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val dx = e2.x - (e1 ?: return false).x
                if (abs(dx) < dp(72) || abs(velocityX) < abs(velocityY)) return false
                if (dx < 0 && index < pages.lastIndex) forward() else if (dx > 0 && index > 0) back()
                return true
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        swipe.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun back() {
        index--
        show()
    }

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
            back()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        private const val PAGE = "page"
        private const val FASHION_MS = 1_500L
        /** Same file as the permissions' answers: the welcome belongs to the first start. */
        const val PREFS = "permissions"
        const val WELCOMED = "welcomed"
    }
}
