package app.friendlypear.ui.peri

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.services.PeriView

/**
 * The new Peri, a preview (round 4): Peri by layers ([LayeredPeri]) on her hill, to see before she becomes the Peri
 * of every screen. Each button shows an expression; "Say something" has her say something kind, her mouth moving in
 * any expression and her hands talking too; "Wave hello" says hello. Touch her and she laughs; move a finger anywhere
 * and she follows it with her eyes.
 */
class LayersActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }
    private lateinit var peri: LayeredPeri
    private lateinit var world: PeriWorld
    private lateinit var said: TextView
    /** Her mouth moves while her voice plays, and while her words show up in the bubble even with the voice off. */
    private var voice = false
    private var reading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        world = PeriWorld(this)
        peri = LayeredPeri(this).apply {
            setOnClickListener { say(Lines.next(this@LayersActivity, R.array.peri_taps)) }
        }
        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Palette.GREEN_DARK)
            contentDescription = getString(R.string.back)
            background = pressable(rounded(Palette.WHITE, dp(24).toFloat()), Palette.RIPPLE_DARK)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { finish() }
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
            // On a white pill, so the title reads on any sky.
            addView(heading(getString(R.string.layers_title)).apply {
                background = rounded(Palette.WHITE, dp(24).toFloat())
                setPadding(dp(18), dp(6), dp(18), dp(8))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        }
        said = TextView(this).apply {
            text = getString(R.string.layers_hint)
            sizeSp(19f)
            // Her own voice: round Fredoka, as in her bubble at home.
            typeface = Fonts.display(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.12f)
            maxLines = 4
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            val tail = dp(12).toFloat()
            background = BubbleDrawable(Palette.WHITE, Palette.LINE, dp(1).toFloat(), dp(22).toFloat(), tail, BubbleDrawable.Tail.TOP)
            setPadding(dp(20), dp(12) + tail.toInt(), dp(20), dp(14))
        }

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(12), dp(12), dp(12), dp(16))
        }
        val buttons = LOOKS.map { (label, look) -> getString(label) to { peri.express(look) } } +
            listOf(getString(R.string.layers_blink) to { peri.blink() }, getString(R.string.layers_wave) to { wave() })
        buttons.chunked(3).forEach { row ->
            sheet.addView(LinearLayout(this).apply {
                row.forEach { (label, action) ->
                    // As tall as their words need with big text, never less than a comfortable touch.
                    addView(chipButton(label) { action() }.apply { minimumHeight = dp(46) },
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
                }
            })
        }
        sheet.addView(Pill(this, getString(R.string.layers_talk), R.drawable.ic_volume, primary = true).apply {
            setOnClickListener { say(Lines.next(this@LayersActivity, R.array.peri_kind)) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply { setMargins(dp(4), dp(10), dp(4), 0) })

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(top)
            addView(peri, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(4) })
            addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
            addView(sheet, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        }
        val root = FrameLayout(this).apply {
            addView(world, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val pad = dp(16)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            column.setPadding(pad + bars.left, dp(8) + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        // Peri stands on her hill, wherever the sheet leaves her.
        column.viewTreeObserver.addOnGlobalLayoutListener {
            if (peri.height == 0 || world.height == 0) return@addOnGlobalLayoutListener
            val at = IntArray(2).also { peri.getLocationInWindow(it) }
            val origin = IntArray(2).also { world.getLocationInWindow(it) }
            world.setGround(at[1] - origin[1] + peri.feetY + dp(3))
        }
        setContentView(root)
        sheet.floatIn(120)
    }

    /** A finger anywhere on the screen: she follows it with her eyes. */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> peri.follow(event.rawX, event.rawY)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> peri.letGo()
        }
        return super.dispatchTouchEvent(event)
    }

    private fun wave() {
        peri.wave()
        say(getString(R.string.layers_hello))
    }

    private fun say(line: String) {
        said.text = line
        said.popIn()
        reading = true
        peri.talk(true)
        said.removeCallbacks(doneReading)
        said.postDelayed(doneReading, (line.split(' ').size * WORD_MS).coerceIn(READ_MIN_MS, READ_MAX_MS))
        hub.say(line)
    }

    private val doneReading = Runnable {
        reading = false
        peri.talk(voice)
    }

    private val view = object : PeriView {
        override fun speaking(on: Boolean) {
            voice = on
            peri.talk(on || reading)
        }
    }

    override fun onResume() {
        super.onResume()
        hub.attach(view)
    }

    override fun onPause() {
        hub.detach(view)
        voice = false
        peri.talk(reading)
        super.onPause()
    }

    private companion object {
        val LOOKS = listOf(
            R.string.layers_happy to LayeredPeri.Look.HAPPY,
            R.string.layers_laugh to LayeredPeri.Look.LAUGH,
            R.string.layers_surprised to LayeredPeri.Look.SURPRISED,
            R.string.layers_love to LayeredPeri.Look.LOVE,
            R.string.layers_stars to LayeredPeri.Look.STARS,
            R.string.layers_worried to LayeredPeri.Look.WORRIED,
            R.string.layers_brave to LayeredPeri.Look.BRAVE,
            R.string.layers_sleepy to LayeredPeri.Look.SLEEPY,
            R.string.layers_wink to LayeredPeri.Look.WINK,
            R.string.layers_sad to LayeredPeri.Look.SAD,
        )
        const val WORD_MS = 260L
        const val READ_MIN_MS = 900L
        const val READ_MAX_MS = 4_500L
    }
}
