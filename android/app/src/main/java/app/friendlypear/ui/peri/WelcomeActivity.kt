package app.friendlypear.ui.peri

import android.app.Activity
import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
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
 * (animated, not a picture) standing on her hill in her world. Each page's card has what it's about, a title, a
 * sentence and three concrete points with their icons, over a progress bar, so it reads like the product it is.
 * She says only the first page aloud, so the first start isn't a lecture; the round button reads any page. On the
 * wardrobe page she shows off a few outfits by herself.
 */
class WelcomeActivity : Activity() {
    private data class Page(val about: Int, val title: Int, val text: Int, val mood: Mood, val points: List<Pair<Int, Int>>, val moment: PeriMoment? = null)

    private val pages = listOf(
        Page(R.string.welcome_about_1, R.string.welcome_1_title, R.string.welcome_1_text, Mood.CALM, listOf(
            R.drawable.ic_mic to R.string.welcome_point_1a, R.drawable.ic_message to R.string.welcome_point_1b, R.drawable.ic_call to R.string.welcome_point_1c,
        )),
        Page(R.string.welcome_about_2, R.string.welcome_2_title, R.string.welcome_2_text, Mood.PROTECTIVE, listOf(
            R.drawable.ic_shield to R.string.welcome_point_2a, R.drawable.ic_call to R.string.welcome_point_2b, R.drawable.ic_warning to R.string.welcome_point_2c,
        )),
        Page(R.string.welcome_about_3, R.string.welcome_3_title, R.string.welcome_3_text, Mood.HAPPY, listOf(
            R.drawable.ic_family to R.string.welcome_point_3a, R.drawable.ic_check to R.string.welcome_point_3b, R.drawable.ic_notifications to R.string.welcome_point_3c,
        )),
        Page(R.string.welcome_about_dress, R.string.welcome_dress_title, R.string.welcome_dress_text, Mood.CALM, listOf(
            R.drawable.ic_wardrobe to R.string.welcome_point_4a, R.drawable.ic_family to R.string.welcome_point_4b,
        )),
        Page(R.string.welcome_about_4, R.string.welcome_4_title, R.string.welcome_4_text, Mood.CALM, listOf(
            R.drawable.ic_check to R.string.welcome_point_5a, R.drawable.ic_close to R.string.welcome_point_5b, R.drawable.ic_settings to R.string.welcome_point_5c,
        ), PeriMoment.THUMBS_UP),
    )
    private val dressPage = 3
    private lateinit var world: PeriWorld
    private lateinit var dressNow: View
    private var index = 0
    private lateinit var face: PeriFace
    private lateinit var about: TextView
    private lateinit var title: TextView
    private lateinit var text: TextView
    private lateinit var points: LinearLayout
    private lateinit var progress: LinearLayout
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
        about = TextView(this).apply {
            sizeSp(TextSize.SMALL)
            typeface = weight(800)
            letterSpacing = 0.08f
            isAllCaps = true
            setTextColor(Palette.GREEN_DARK)
        }
        title = heading("").apply { isAccessibilityHeading = true }
        text = body("").apply {
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.2f)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        points = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        progress = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        next = Pill(this, "", R.drawable.ic_chevron, primary = true, trailing = true).apply { setOnClickListener { forward() } }
        // Hearing the page: a round button beside "Next", so the card keeps the room.
        val hear = ImageView(this).apply {
            setImageResource(R.drawable.ic_volume)
            setColorFilter(Palette.GREEN_DARK)
            contentDescription = getString(R.string.welcome_listen)
            background = pressable(cardBackground(), Palette.RIPPLE_DARK)
            setPadding(dp(20), dp(18), dp(20), dp(22))
            stateListAnimator = pressScale()
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
        // The words on a card: what it's about, the title, a sentence, three points and the progress so far.
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(20), dp(18), dp(20), dp(16))
            addView(about)
            addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
            addView(text, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
            addView(points, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
            addView(dressNow, LinearLayout.LayoutParams(dp(200), dp(46)).apply { topMargin = dp(10) })
            addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)).apply { topMargin = dp(16) })
        }
        val buttons = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(hear, LinearLayout.LayoutParams(dp(68), dp(68)).apply { marginEnd = dp(10) })
            addView(next, LinearLayout.LayoutParams(0, dp(68), 1f))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(top)
            addView(face, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
            addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            addView(buttons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
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

    /** One of a page's points: its icon in a soft green circle, and a short line. */
    private fun point(icon: Int, line: Int): View = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(this@WelcomeActivity).apply {
            setImageResource(icon)
            setColorFilter(Palette.GREEN_DARK)
            background = rounded(Palette.GREEN_TINT, dp(18).toFloat())
            setPadding(dp(8), dp(8), dp(8), dp(8))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        addView(body(getString(line), TextSize.SMALL).apply {
            setTextColor(Palette.INK)
            typeface = weight(600)
            setPadding(dp(12), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun show(first: Boolean = false) {
        val page = pages[index]
        about.text = getString(page.about)
        title.text = getString(page.title)
        text.text = getString(page.text)
        points.removeAllViews()
        page.points.forEachIndexed { i, (icon, line) ->
            points.addView(point(icon, line), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                if (i > 0) topMargin = dp(8)
            })
        }
        face.show(page.mood)
        face.removeCallbacks(fashion)
        face.outfit = Outfit.load(this)
        dressNow.visibility = if (index == dressPage) View.VISIBLE else View.GONE
        if (index == dressPage) face.post(fashion)
        if (index == 0) face.greet()
        page.moment?.let { m -> face.postDelayed({ face.moment(m) }, 400) }
        next.label = getString(if (index == pages.lastIndex) R.string.welcome_start else R.string.welcome_next)
        skip.visibility = if (index == pages.lastIndex) View.INVISIBLE else View.VISIBLE
        // The progress so far: one segment per page, filled up to this one.
        progress.removeAllViews()
        pages.indices.forEach { i ->
            progress.addView(View(this).apply {
                background = rounded(if (i <= index) Palette.GREEN_DARK else Palette.GREEN_TINT, dp(4).toFloat())
            }, LinearLayout.LayoutParams(0, dp(6), 1f).apply {
                marginStart = if (i == 0) 0 else dp(3)
                marginEnd = if (i == pages.lastIndex) 0 else dp(3)
            })
        }
        progress.contentDescription = getString(R.string.welcome_page, index + 1, pages.size)
        listOf(about, title, text, points).forEachIndexed { i, v -> v.floatIn(30L + 50L * i) }
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
