package app.friendlypear.ui.peri

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.net.FamilyRequestClient
import app.friendlypear.net.FamilyRequestClient.Answer
import app.friendlypear.net.FamilyRequestClient.Outcome
import java.util.concurrent.Executors

/**
 * Peri to Peri: a family member asks for the guardian's OK, and the guardian's own Peri shows it here, with the
 * same three answers as the web page. Safest first: "No"; then "Call them now" (Peri tells their Peri to hold on,
 * and the phone dials them); "Yes" last, behind three checks and the phone's own lock.
 */
class GuardianActivity : Activity() {
    private lateinit var url: String
    private lateinit var from: String
    private var fromPhone: String? = null
    private lateinit var toName: String
    private var onCall = false
    private var kind = KIND_SEND_CODE
    private lateinit var peri: ImageView
    private lateinit var answers: LinearLayout
    private lateinit var result: TextView
    private lateinit var expiry: TextView
    private lateinit var why: View
    private lateinit var back: View
    private val io = Executors.newSingleThreadExecutor()
    private var bob: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        from = intent.getStringExtra(EXTRA_FROM) ?: return finish()
        fromPhone = intent.getStringExtra(EXTRA_PHONE)
        toName = intent.getStringExtra(EXTRA_TO) ?: return finish()
        onCall = intent.getBooleanExtra(EXTRA_ON_CALL, false)
        kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_SEND_CODE
        build(intent.getLongExtra(EXTRA_EXPIRES, 0L))
        // Opened later from the notification, it may have been answered elsewhere: check with the server.
        io.execute {
            val fresh = FamilyRequestClient.fetch(url)
            runOnUiThread { if (fresh == null || !fresh.pending) showDone(getString(R.string.guardian_gone), R.drawable.peri_calm_sorry) }
        }
    }

    override fun onDestroy() {
        bob?.cancel()
        io.shutdownNow()
        super.onDestroy()
    }

    private fun build(expiresAt: Long) {
        peri = ImageView(this).apply {
            setImageResource(if (onCall) R.drawable.peri_guardian_alert else R.drawable.peri_guardian)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(peri, LinearLayout.LayoutParams(dp(120), dp(120)))
            addView(heading(getString(R.string.guardian_title, from)).apply { setPadding(dp(10), 0, 0, 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        val ask = card().apply {
            val what = if (kind == KIND_CIRCLE) getString(R.string.guardian_circle, toName) else getString(R.string.guardian_send_code, toName)
            addView(heading(what, TextSize.BODY))
        }
        val alert = LinearLayout(this).apply {
            background = rounded(ALERT_FILL, dp(24).toFloat(), ALERT_LINE, dp(1.5f))
            setPadding(dp(16), dp(14), dp(16), dp(16))
            addView(TextView(this@GuardianActivity).apply {
                text = "!"
                sizeSp(TextSize.BODY)
                typeface = weight(800)
                gravity = Gravity.CENTER
                setTextColor(Palette.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Palette.DANGER)
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(34), dp(34)))
            addView(LinearLayout(this@GuardianActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
                addView(heading(getString(R.string.guardian_alert_title), TextSize.BODY).apply { setTextColor(Palette.AMBER_INK) })
                addView(body(getString(R.string.guardian_alert), TextSize.SMALL).apply { setTextColor(Palette.AMBER_INK) })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            visibility = if (onCall) View.VISIBLE else View.GONE
        }
        val no = Pill(this, getString(R.string.guardian_no), R.drawable.ic_shield, primary = true).apply {
            setOnClickListener { send(Answer.REJECT) }
        }
        val call = Pill(this, getString(R.string.guardian_call, from), R.drawable.ic_call, primary = false).apply {
            setOnClickListener { send(Answer.CALLBACK) }
        }
        val yes = TextView(this).apply {
            text = getString(R.string.guardian_yes)
            sizeSp(TextSize.BODY)
            typeface = weight(600)
            gravity = Gravity.CENTER
            setTextColor(YES_INK)
            background = pressable(rounded(Palette.WHITE, 999f, YES_INK, dp(1.5f)), Palette.RIPPLE_DARK)
            isClickable = true
            isFocusable = true
            setOnClickListener { confirmYes() }
        }
        answers = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(no, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)))
            addView(call, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(12) })
            addView(yes, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(12) })
        }
        result = heading("", TextSize.BODY).apply {
            gravity = Gravity.CENTER
            visibility = View.GONE
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        back = Pill(this, getString(R.string.guardian_back), R.drawable.ic_pear, primary = true).apply {
            visibility = View.GONE
            setOnClickListener {
                startActivity(Intent(this@GuardianActivity, PeriActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            }
        }
        val minutes = if (expiresAt > 0) maxOf(1L, (expiresAt - System.currentTimeMillis() + 59_999) / 60_000) else 15L
        expiry = body(getString(R.string.guardian_expires, minutes), TextSize.SMALL)
        why = card().apply {
            addView(heading(getString(R.string.guardian_why_title), TextSize.BODY).apply { setTextColor(Palette.GREEN_DARK) })
            addView(body(getString(if (kind == KIND_CIRCLE) R.string.guardian_why_circle else R.string.guardian_why), TextSize.SMALL))
            addView(heading(getString(R.string.guardian_meanwhile_title), TextSize.BODY).apply {
                setTextColor(Palette.GREEN_DARK)
                setPadding(0, dp(8), 0, 0)
            })
            addView(body(getString(R.string.guardian_meanwhile), TextSize.SMALL))
        }
        val privacy = body(getString(R.string.guardian_privacy), TextSize.SMALL)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            fun add(v: View, top: Int) = addView(v, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) })
            add(header, 0)
            add(ask, 12)
            add(alert, 12)
            add(answers, 18)
            add(result, 18)
            addView(back, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(16) })
            add(expiry, 10)
            add(why, 14)
            add(privacy, 12)
        }
        listOf(ask, alert, answers, why).forEachIndexed { i, v -> v.floatIn(60L + 70L * i) }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            addView(list)
        }
        val pad = dp(20)
        setContentView(FrameLayout(this).apply {
            background = ground()
            addView(scroll)
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                scroll.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
                insets
            }
        })
        if (Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f) {
            bob = ObjectAnimator.ofFloat(peri, View.TRANSLATION_Y, 0f, -dp(6).toFloat()).apply {
                duration = 1_400
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = cardBackground()
        setPadding(dp(18), dp(16), dp(18), dp(16))
    }

    /** Three checks, the safe answer first, then the phone's own lock: a yes must be on purpose. */
    private fun confirmYes() {
        val checks = listOf(
            getString(R.string.guardian_check_1, from),
            getString(R.string.guardian_check_2, toName),
            getString(R.string.guardian_check_3),
        ).joinToString("\n\n") { "☐  $it" }
        AlertDialog.Builder(this)
            .setTitle(R.string.guardian_confirm_title)
            .setMessage(getString(R.string.guardian_confirm_intro) + "\n\n" + checks)
            .setNegativeButton(R.string.guardian_confirm_no) { _, _ -> send(Answer.REJECT) }
            .setPositiveButton(R.string.guardian_confirm_yes) { _, _ ->
                confirmIdentity(getString(R.string.guardian_identity_title), getString(R.string.guardian_identity_subtitle, from), { send(Answer.APPROVE) })
            }
            .show()
            .friendly(risky = AlertDialog.BUTTON_POSITIVE, riskyColor = YES_INK)
    }

    private fun send(answer: Answer) {
        answers.alpha = 0.5f
        answers.isEnabled = false
        for (i in 0 until answers.childCount) answers.getChildAt(i).isEnabled = false
        io.execute {
            val outcome = FamilyRequestClient.answer(url, answer)
            runOnUiThread { answered(answer, outcome) }
        }
    }

    private fun answered(answer: Answer, outcome: Outcome) {
        val hub = PeriApp.hub(this)
        when (outcome) {
            Outcome.DONE -> {
                val (text, art) = when (answer) {
                    Answer.REJECT -> getString(R.string.guardian_done_no, from) to R.drawable.peri_protective
                    Answer.CALLBACK -> getString(R.string.guardian_done_call, from) to R.drawable.peri_calling
                    Answer.APPROVE -> getString(R.string.guardian_done_yes, from) to R.drawable.peri_calm_thumbsup
                }
                hub.say(text)
                showDone(text, art)
                // Their Peri now offers to call; the guardian's phone dials them too, so the two meet halfway.
                if (answer == Answer.CALLBACK) fromPhone?.let { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) }
            }
            Outcome.GONE -> showDone(getString(R.string.guardian_gone), R.drawable.peri_calm_sorry)
            Outcome.FAILED -> {
                answers.alpha = 1f
                for (i in 0 until answers.childCount) answers.getChildAt(i).isEnabled = true
                hub.show(getString(R.string.guardian_failed))
                result.text = getString(R.string.guardian_failed)
                result.visibility = View.VISIBLE
            }
        }
    }

    private fun showDone(text: String, art: Int) {
        answers.visibility = View.GONE
        expiry.visibility = View.GONE
        why.visibility = View.GONE
        back.visibility = View.VISIBLE
        peri.setImageResource(art)
        result.text = text
        result.visibility = View.VISIBLE
        result.floatIn()
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_FROM = "from"
        private const val EXTRA_PHONE = "phone"
        private const val EXTRA_TO = "to"
        private const val EXTRA_ON_CALL = "on_call"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_EXPIRES = "expires"
        const val KIND_SEND_CODE = "send_code"
        const val KIND_CIRCLE = "circle_change"
        private const val ALERT_FILL = 0xFFFFF1D6.toInt()
        private const val ALERT_LINE = 0xFFF2C46D.toInt()
        /** The "yes" ink: 7.4:1 on white. */
        private const val YES_INK = 0xFF8A2B1F.toInt()

        fun intent(context: Context, url: String, from: String, fromPhone: String?, request: FamilyRequestClient.Request): Intent =
            Intent(context, GuardianActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_FROM, from)
                .putExtra(EXTRA_PHONE, fromPhone)
                .putExtra(EXTRA_TO, request.toName)
                .putExtra(EXTRA_ON_CALL, request.onCall)
                .putExtra(EXTRA_KIND, request.kind)
                .putExtra(EXTRA_EXPIRES, request.expiresAt)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
