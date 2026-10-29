package app.friendlypear.ui.peri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.core.agent.Topic
import kotlin.math.PI
import kotlin.math.sin

/**
 * "Ask Peri": a written chat about staying safe, for anyone who'd rather type, or can't talk to her (no microphone).
 * Only two voices are in it: what the person writes or picks, and what Peri answers to it right then
 * ([app.friendlypear.services.PeriHub.ask]). The messages and notifications that reach the phone never show up here,
 * so a scammer's words can't pass for Peri's, and nothing written here leaves the phone. Her answers are written and
 * reviewed in advance, never a model's words; the orders she understands aloud ("call Laura") work here too.
 */
class ChatActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }
    private lateinit var list: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var chips: LinearLayout
    /** Peri by layers in the header: her mouth moves as she answers, and a risky topic gets her brave face. */
    private lateinit var peri: LayeredPeri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        peri = LayeredPeri(this).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(peri, LinearLayout.LayoutParams(dp(76), dp(76)))
            addView(LinearLayout(this@ChatActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, dp(6), 0)
                addView(heading(getString(R.string.chat_title)))
                addView(body(getString(R.string.chat_note), TextSize.SMALL).apply { setTextColor(Palette.INK_SOFT) })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(this@ChatActivity).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(Palette.GREEN_DARK)
                contentDescription = getString(R.string.chat_close)
                background = pressable(rounded(Palette.WHITE, dp(24).toFloat()), Palette.RIPPLE_DARK)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
        }
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(10))
        }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(list)
        }
        chips = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val chipRow = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(chips)
        }
        input = EditText(this).apply {
            hint = getString(R.string.chat_hint)
            sizeSp(TextSize.BODY)
            typeface = weight(400)
            setTextColor(Palette.INK)
            setHintTextColor(Palette.INK_SOFT)
            background = rounded(Palette.WHITE, dp(28).toFloat(), Palette.LINE_STRONG, dp(1.5f))
            setPadding(dp(18), dp(12), dp(18), dp(12))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            maxLines = 4
            setOnEditorActionListener { _, action, event ->
                val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
                if (action == EditorInfo.IME_ACTION_SEND || enter) send(text.toString())
                true
            }
        }
        val sendButton = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron)
            setColorFilter(Palette.WHITE)
            contentDescription = getString(R.string.chat_send)
            background = pressable(rounded(Palette.GREEN_DARK, dp(28).toFloat()), Palette.RIPPLE_LIGHT)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            stateListAnimator = pressScale()
            setOnClickListener { send(input.text.toString()) }
        }
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) })
            addView(sendButton, LinearLayout.LayoutParams(dp(56), dp(56)))
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(header)
            addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })
            addView(chipRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(bar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        }
        setContentView(FrameLayout(this).apply {
            background = ground()
            addView(column)
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                column.setPadding(dp(16) + bars.left, dp(8) + bars.top, dp(16) + bars.right, dp(12) + bars.bottom)
                insets
            }
        })
        val hello = if (intent.getBooleanExtra(EXTRA_NO_MIC, false)) getString(R.string.chat_no_mic) + " " + getString(R.string.chat_hello) else getString(R.string.chat_hello)
        addPeri(hello)
        suggest(START)
        column.floatIn()
    }

    private fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        input.text.clear()
        addMine(text)
        hub.ask(text) { lines, topic ->
            if (isFinishing) return@ask
            answer(lines) {
                if (topic != null && topic !in CALM) peri.express(LayeredPeri.Look.BRAVE)
                suggest(FOLLOW_UPS[topic] ?: START)
            }
        }
    }

    /** Her answer after a moment of "writing" (her three dots bounce), one line after another, her mouth moving. */
    private fun answer(lines: List<String>, then: () -> Unit) {
        if (lines.isEmpty()) return then()
        val dots = typing()
        add(dots, mine = false)
        val wait = if (animationsOn()) (450L + lines.first().length * 10L).coerceAtMost(1_300L) else 0L
        list.postDelayed({
            if (isFinishing) return@postDelayed
            list.removeView(dots)
            addPeri(lines.first())
            talkFor(lines.first())
            answer(lines.drop(1), then)
        }, wait)
    }

    /** She says the line in the header: her mouth moves for about as long as it takes to read it. */
    private fun talkFor(line: String) {
        peri.removeCallbacks(quiet)
        peri.talk(true)
        peri.postDelayed(quiet, (line.split(' ').size * WORD_MS).coerceIn(TALK_MIN_MS, TALK_MAX_MS))
    }

    private val quiet = Runnable { peri.talk(false) }


    /** Peri "writing": a white bubble with her three dots bouncing one after another. */
    private fun typing(): View = LinearLayout(this).apply {
        background = rounded(Palette.WHITE, dp(22).toFloat(), Palette.LINE, dp(1))
        setPadding(dp(14), dp(14), dp(14), dp(14))
        contentDescription = getString(R.string.chat_typing)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        addView(TypingDots(this@ChatActivity), LinearLayout.LayoutParams(dp(54), dp(20)))
    }

    private fun addMine(text: String) = add(bubble(text, mine = true), mine = true)

    private fun addPeri(text: String) = add(bubble(text, mine = false), mine = false)

    private fun add(view: View, mine: Boolean) {
        list.addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = if (mine) Gravity.END else Gravity.START
            topMargin = dp(10)
            if (mine) marginStart = dp(48) else marginEnd = dp(36)
        })
        view.popIn()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    /** The person's words on the right, in green; Peri's on the left, white, with a way to hear them. */
    private fun bubble(text: String, mine: Boolean): View {
        val words = body(text).apply {
            setTextColor(if (mine) Palette.WHITE else Palette.INK)
            setLineSpacing(0f, 1.15f)
        }
        return LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = if (mine) rounded(Palette.GREEN_DARK, dp(22).toFloat()) else rounded(Palette.WHITE, dp(22).toFloat(), Palette.LINE, dp(1))
            setPadding(dp(16), dp(12), dp(if (mine) 16 else 8), dp(12))
            addView(words, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (!mine) addView(ImageView(this@ChatActivity).apply {
                setImageResource(R.drawable.ic_volume)
                setColorFilter(Palette.GREEN_DARK)
                contentDescription = getString(R.string.chat_hear)
                background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setOnClickListener { hub.say(text) }
            }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginStart = dp(4) })
        }
    }

    /** Questions to tap, said as the person would: each one goes in as if written. */
    private fun suggest(ids: List<Int>) {
        chips.removeAllViews()
        ids.forEach { id ->
            val label = getString(id)
            chips.addView(chipButton(label) { send(label) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(46)).apply { marginEnd = dp(8) })
        }
    }

    companion object {
        private const val EXTRA_NO_MIC = "no_mic"
        private const val WORD_MS = 230L
        private const val TALK_MIN_MS = 700L
        private const val TALK_MAX_MS = 3_500L

        fun intent(context: Context, noMic: Boolean = false): Intent = Intent(context, ChatActivity::class.java).putExtra(EXTRA_NO_MIC, noMic)

        private val CALM = setOf(Topic.WHATSAPP, Topic.PASSWORDS, Topic.WHAT_IS_A_SCAM, Topic.SHOPPING, Topic.TROUBLE)

        private val START = listOf(
            R.string.chat_s_bank, R.string.chat_s_joke, R.string.chat_s_code, R.string.chat_s_link, R.string.chat_s_who, R.string.chat_s_family,
            R.string.chat_s_app, R.string.chat_s_prize, R.string.chat_s_whatsapp, R.string.chat_s_stolen, R.string.chat_s_scam, R.string.chat_s_gave,
        )

        /** After an answer, what usually comes next. */
        private val FOLLOW_UPS: Map<Topic, List<Int>> = mapOf(
            Topic.BANK_CALL to listOf(R.string.chat_s_gave, R.string.chat_s_code, R.string.chat_s_app),
            Topic.CODE to listOf(R.string.chat_s_gave, R.string.chat_s_whatsapp, R.string.chat_s_sim),
            Topic.LINK to listOf(R.string.chat_s_gave, R.string.chat_s_shop, R.string.chat_s_scam),
            Topic.FAMILY_MONEY to listOf(R.string.chat_s_code, R.string.chat_s_gave, R.string.chat_s_scam),
            Topic.REMOTE_APP to listOf(R.string.chat_s_installed, R.string.chat_s_bank),
            Topic.STOLEN_PHONE to listOf(R.string.chat_s_sim, R.string.chat_s_whatsapp),
            Topic.SIM_SWAP to listOf(R.string.chat_s_bank, R.string.chat_s_stolen),
            Topic.WHAT_IS_A_SCAM to listOf(R.string.chat_s_bank, R.string.chat_s_link, R.string.chat_s_police),
            Topic.PRIZE to listOf(R.string.chat_s_link, R.string.chat_s_gave),
            // "Tengo un problema": what happened, as one tap each; an emergency is one of them.
            Topic.TROUBLE to listOf(R.string.chat_s_called, R.string.chat_s_message, R.string.chat_s_gave, R.string.chat_s_emergency),
            Topic.CHECK_MESSAGE to listOf(R.string.chat_s_gave, R.string.chat_s_scam),
        )
    }
}

/** Three dots that rise one after another, as while someone writes; still with "remove animations". */
private class TypingDots(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Palette.GREEN_DARK }
    private val moves = context.animationsOn()

    override fun onDraw(canvas: Canvas) {
        val r = height / 5f
        val gap = (width - 6 * r) / 2f
        val t = SystemClock.uptimeMillis()
        for (i in 0 until 3) {
            val phase = ((t + i * 160) % 900) / 900.0
            val lift = if (moves) (sin(phase * 2 * PI).coerceAtLeast(0.0) * r * 1.6).toFloat() else 0f
            paint.alpha = if (moves) (150 + 105 * (lift / (r * 1.6f))).toInt().coerceIn(0, 255) else 200
            canvas.drawCircle(r + i * (2 * r + gap), height - r - lift, r, paint)
        }
        if (moves) postInvalidateOnAnimation()
    }
}
