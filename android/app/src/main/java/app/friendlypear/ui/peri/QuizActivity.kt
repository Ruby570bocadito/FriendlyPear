package app.friendlypear.ui.peri

import android.app.Activity
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.config.Diary
import app.friendlypear.config.Shields
import app.friendlypear.core.agent.Mood
import app.friendlypear.services.PeriMoment
import app.friendlypear.services.PeriView
import java.util.Calendar

/**
 * "Scam or not?": Peri shows six messages, one at a time, the way they'd arrive on the phone, and the person says
 * whether each is a trap or fine. Peri inspects each one with her magnifying glass, reacts to the answer (a thumbs
 * up, or a kind "nearly") and says why in a sentence; at the end she celebrates the round. A round played well earns
 * a shield, once a day. The links in the messages are pretend: shown as links, never opened, never read aloud.
 */
class QuizActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }
    private lateinit var face: PeriFace
    private lateinit var bubble: TextView
    private lateinit var progress: TextView
    private lateinit var dots: LinearLayout
    private lateinit var content: LinearLayout
    private var round: List<Int> = emptyList()
    private var at = 0
    private var correct = 0
    private val marks = ArrayList<Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        face = PeriFace(this).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        bubble = body("", 18f).apply {
            setTextColor(Palette.INK)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            val tail = dp(12).toFloat()
            background = BubbleDrawable(Palette.WHITE, Palette.LINE, dp(1).toFloat(), dp(20).toFloat(), tail, BubbleDrawable.Tail.LEFT)
            setPadding(dp(16) + tail.toInt(), dp(12), dp(16), dp(14))
        }
        progress = TextView(this).apply {
            sizeSp(16f)
            typeface = Fonts.display(600)
            setTextColor(Palette.SKY_INK)
            background = rounded(Palette.SKY_TINT, dp(16).toFloat())
            setPadding(dp(12), dp(5), dp(12), dp(6))
        }
        dots = LinearLayout(this).apply { gravity = Gravity.CENTER }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

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
            addView(heading(getString(R.string.quiz_title)).apply { setPadding(dp(12), 0, dp(8), 0) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(progress)
        }
        val peri = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            addView(face, LinearLayout.LayoutParams(dp(118), dp(118)))
            addView(bubble, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(top)
            addView(dots, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(18)).apply { topMargin = dp(12) })
            addView(peri, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
            addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isVerticalScrollBarEnabled = false
            addView(column)
        }
        val pad = dp(18)
        setContentView(FrameLayout(this).apply {
            background = ground()
            addView(scroll)
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                scroll.setPadding(pad + bars.left, dp(10) + bars.top, pad + bars.right, pad + bars.bottom)
                insets
            }
        })
        face.show(Mood.CALM)
        start()
    }

    /** A new round: four traps and two real messages, shuffled. */
    private fun start() {
        val scams = resources.getIntArray(R.array.quiz_scam)
        val traps = scams.indices.filter { scams[it] == 1 }.shuffled().take(TRAPS)
        val fine = scams.indices.filter { scams[it] == 0 }.shuffled().take(ROUND - TRAPS)
        round = (traps + fine).shuffled()
        at = 0
        correct = 0
        marks.clear()
        say(resources.getQuantityString(R.plurals.quiz_intro, round.size, round.size))
        ask()
    }

    private fun ask() {
        content.removeAllViews()
        progress.text = getString(R.string.quiz_progress, at + 1, round.size)
        showDots()
        face.show(Mood.THINKING)
        face.moment(PeriMoment.DETECTIVE)
        val i = round[at]
        val card = message(i)
        content.addView(card)
        val answers = LinearLayout(this).apply {
            addView(Pill(this@QuizActivity, getString(R.string.quiz_scam_button), R.drawable.ic_warning, primary = false, rubber = Rubber.CORAL).apply {
                setOnClickListener { answer(i, scam = true) }
            }, LinearLayout.LayoutParams(0, dp(66), 1f).apply { marginEnd = dp(6) })
            addView(Pill(this@QuizActivity, getString(R.string.quiz_ok_button), R.drawable.ic_check, primary = false, rubber = Rubber.GREEN).apply {
                setOnClickListener { answer(i, scam = false) }
            }, LinearLayout.LayoutParams(0, dp(66), 1f).apply { marginStart = dp(6) })
        }
        content.addView(answers, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })
        if (at == 0) content.addView(body(getString(R.string.quiz_link_note), 15f).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), 0)
        })
        card.floatIn()
        answers.floatIn(90)
    }

    /** The message as it would arrive: who sent it, how, and what it says, with any link drawn as a link. */
    private fun message(i: Int): View {
        val channel = resources.getStringArray(R.array.quiz_channel)[i]
        val (icon, label) = when (channel) {
            "call" -> R.drawable.ic_call to R.string.quiz_channel_call
            "whatsapp" -> R.drawable.ic_message to R.string.quiz_channel_whatsapp
            else -> R.drawable.ic_message to R.string.quiz_channel_sms
        }
        return whiteCard().apply {
            addView(LinearLayout(this@QuizActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(badge(icon, Palette.SKY_TINT, Palette.SKY_INK), LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(LinearLayout(this@QuizActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                    addView(heading(resources.getStringArray(R.array.quiz_sender)[i], 19f))
                    addView(body(getString(label), 15f))
                })
            })
            addView(body(withLinks(resources.getStringArray(R.array.quiz_text)[i]), 19f).apply {
                setTextColor(Palette.INK)
                setPadding(0, dp(12), 0, 0)
            })
        }
    }

    /** "{...}" in a message is a pretend link: drawn blue and underlined, never clickable. */
    private fun withLinks(text: String): CharSequence {
        val out = SpannableStringBuilder()
        var rest = text
        while (true) {
            val open = rest.indexOf('{')
            val close = rest.indexOf('}', open + 1)
            if (open < 0 || close < 0) break
            out.append(rest, 0, open)
            val start = out.length
            out.append(rest, open + 1, close)
            out.setSpan(ForegroundColorSpan(Palette.SKY_INK), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            out.setSpan(UnderlineSpan(), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            rest = rest.substring(close + 1)
        }
        return out.append(rest)
    }

    private fun answer(i: Int, scam: Boolean) {
        val wasScam = resources.getIntArray(R.array.quiz_scam)[i] == 1
        val ok = scam == wasScam
        if (ok) correct++
        marks += ok
        showDots()
        // The buttons go; what it really was, and why, takes their place.
        content.removeViews(1, content.childCount - 1)
        face.show(Mood.CALM)
        face.moment(if (ok) PeriMoment.CLAP else PeriMoment.SORRY)
        if (ok) face.sparkle()
        val clue = resources.getStringArray(R.array.quiz_clue)[i]
        // Her bubble has her reaction; the why is on the card below, and she says both.
        val reaction = Lines.next(this, if (ok) R.array.quiz_right else R.array.quiz_wrong)
        say(reaction, "$reaction $clue")
        val verdict = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            val rubber = if (wasScam) Rubber.CORAL else Rubber.GREEN
            background = tintedCard(rubber.face, rubber.lip)
            setPadding(dp(16), dp(12), dp(16), dp(16))
            addView(badge(if (wasScam) R.drawable.ic_warning else R.drawable.ic_check, Palette.WHITE, rubber.ink), LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(LinearLayout(this@QuizActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(heading(getString(if (wasScam) R.string.quiz_was_scam else R.string.quiz_was_ok), 20f).apply { setTextColor(rubber.ink) })
                addView(body(clue, 17f).apply { setTextColor(Palette.INK) })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        content.addView(verdict, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        val last = at == round.size - 1
        val next = Pill(this, getString(if (last) R.string.quiz_see_result else R.string.quiz_next), R.drawable.ic_chevron, primary = true, trailing = true).apply {
            setOnClickListener {
                if (last) finished() else {
                    at++
                    ask()
                }
            }
        }
        content.addView(next, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(66)).apply { topMargin = dp(16) })
        verdict.popIn()
        next.floatIn(120)
    }

    /** The end of the round: the score, Peri's line for it, and a shield for a round played well (once a day). */
    private fun finished() {
        content.removeAllViews()
        progress.text = getString(R.string.quiz_score, correct, round.size)
        val share = correct.toFloat() / round.size
        val bank = when {
            share >= 0.8f -> R.array.quiz_end_great
            share >= 0.5f -> R.array.quiz_end_good
            else -> R.array.quiz_end_low
        }
        if (share >= 0.8f) {
            face.show(Mood.HAPPY)
            face.postDelayed({ face.moment(PeriMoment.PARTY) }, 700)
        } else {
            face.show(Mood.CALM)
            face.moment(if (share >= 0.5f) PeriMoment.THUMBS_UP else PeriMoment.LOVE)
        }
        val line = Lines.next(this, bank)
        hub.diary.add(Diary.Kind.QUIZ, "$correct/${round.size}")
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (best(this)?.let { correct.toFloat() / it.second > it.first.toFloat() / it.second } != false) {
            prefs.edit().putInt(BEST, correct).putInt(BEST_OF, round.size).apply()
        }
        val today = Calendar.getInstance().let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }
        val shield = share >= 0.66f && prefs.getInt(SHIELD_DAY, -1) != today
        if (shield) {
            prefs.edit().putInt(SHIELD_DAY, today).apply()
            hub.earn(Shields.Kind.LEARNED)
            face.postDelayed({ face.moment(PeriMoment.MEDAL) }, 3_600)
        }
        say(if (shield) line + " " + Lines.next(this, R.array.peri_medal) else line)
        val score = whiteCard().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            // A good round gets Peri with her trophy.
            if (correct * 2 >= round.size) addView(ImageView(this@QuizActivity).apply {
                setImageDrawable(sized(R.drawable.peri_scene_trophy, dp(TROPHY_DP)))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                popIn(200)
            }, LinearLayout.LayoutParams(dp(TROPHY_DP), dp(TROPHY_DP)))
            addView(TextView(this@QuizActivity).apply {
                text = getString(R.string.quiz_score, correct, round.size)
                sizeSp(30f)
                typeface = Fonts.display(700)
                setTextColor(Palette.INK)
                gravity = Gravity.CENTER
            })
            addView(stars(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40)).apply { topMargin = dp(8) })
            if (shield) addView(TextView(this@QuizActivity).apply {
                text = getString(R.string.shields_learned)
                sizeSp(16f)
                typeface = Fonts.display(600)
                setTextColor(Palette.AMBER_INK)
                gravity = Gravity.CENTER
                background = rounded(Palette.SUN_TINT, dp(16).toFloat())
                setPadding(dp(14), dp(6), dp(14), dp(7))
                val icon = getDrawable(R.drawable.ic_shield)!!.mutate().apply {
                    setTint(Palette.AMBER_INK)
                    setBounds(0, 0, dp(20), dp(20))
                }
                setCompoundDrawablesRelative(icon, null, null, null)
                compoundDrawablePadding = dp(6)
                popIn(500)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        }
        content.addView(score)
        content.addView(Pill(this, getString(R.string.quiz_again), R.drawable.ic_learn, primary = true).apply {
            setOnClickListener { start() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(66)).apply { topMargin = dp(18) })
        content.addView(Pill(this, getString(R.string.quiz_back), R.drawable.ic_pear, primary = false).apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(10) })
        score.popIn()
    }

    /** One star per right answer, popping in one after another. */
    private fun stars(): View = LinearLayout(this).apply {
        marks.forEachIndexed { i, ok ->
            addView(ImageView(this@QuizActivity).apply {
                setImageResource(R.drawable.ic_star)
                setColorFilter(if (ok) Palette.SUN else Palette.LINE_STRONG)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                popIn(200L + 110L * i)
            }, LinearLayout.LayoutParams(dp(36), dp(36)))
        }
    }

    /** A dot per message: green for correct, coral for a miss, grey to come; the current one larger. */
    private fun showDots() {
        dots.removeAllViews()
        round.indices.forEach { i ->
            val color = when {
                i < marks.size -> if (marks[i]) Palette.GREEN else Palette.CORAL
                else -> Palette.LINE_STRONG
            }
            val size = if (i == at && i >= marks.size) 14 else 10
            dots.addView(View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(size), dp(size)).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            })
        }
    }

    /** Peri writes [text] in her bubble and says [aloud] (the same, unless the screen already shows the rest). */
    private fun say(text: String, aloud: String = text) {
        bubble.text = text
        hub.say(aloud)
    }

    private val view = object : PeriView {
        override fun speaking(on: Boolean) = face.speaking(on)
    }

    override fun onResume() {
        super.onResume()
        hub.attach(view)
    }

    override fun onPause() {
        hub.detach(view)
        face.speaking(false)
        super.onPause()
    }

    companion object {
        const val TROPHY_DP = 120
        private const val PREFS = "quiz"
        private const val BEST = "best"
        private const val BEST_OF = "best_of"
        private const val SHIELD_DAY = "shield_day"
        private const val ROUND = 6
        private const val TRAPS = 4

        /** The best round so far, as right answers and out of how many; null before the first round. */
        fun best(context: Context): Pair<Int, Int>? {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val of = prefs.getInt(BEST_OF, 0)
            return if (of == 0) null else prefs.getInt(BEST, 0) to of
        }
    }
}
