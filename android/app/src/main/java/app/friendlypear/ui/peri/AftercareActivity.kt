package app.friendlypear.ui.peri

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import app.friendlypear.core.agent.Breach

/**
 * First aid after a scam: something was already given away (a code, the card, money, an app that controls the phone,
 * an ID, a password, or data leaked by a company). The steps in order, the most urgent first, each with a button
 * that does it where the phone can (the dialer, the internet switch, the report, the family); each one ticked off
 * stays ticked. Never a reproach. Without a kind, it first asks what happened.
 */
class AftercareActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }
    private val done by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }

    private enum class Act { DIAL, INTERNET, APPS, REPORT, FAMILY, WHATSAPP }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { k -> Breach.entries.firstOrNull { it.name == k } }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (kind == null || kind == Breach.SOMETHING) choose(list) else steps(list, kind)
        val pad = dp(20)
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            addView(list)
        }
        setContentView(FrameLayout(this).apply {
            background = ground()
            addView(scroll)
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                scroll.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
                insets
            }
        })
        list.floatIn()
    }

    /** "What happened?": one big button per kind, in the person's words. */
    private fun choose(list: LinearLayout) {
        list.addView(header(R.string.after_choose_title, R.string.after_choose_text))
        CHOICES.forEach { (breach, label) ->
            list.addView(Pill(this, getString(label), R.drawable.ic_shield, primary = false).apply {
                setOnClickListener {
                    startActivity(intent(this@AftercareActivity, breach))
                    finish()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(62)).apply { topMargin = dp(10) })
        }
    }

    private fun steps(list: LinearLayout, kind: Breach) {
        list.addView(header(R.string.after_title, SUBTITLES.getValue(kind)))
        STEPS.getValue(kind).forEachIndexed { i, (text, act) -> list.addView(step(kind, i, text, act)) }
        list.addView(Pill(this, getString(R.string.after_listen), R.drawable.ic_mic, primary = false).apply {
            setOnClickListener { hub.say(STEPS.getValue(kind).joinToString(" ") { getString(it.first) }) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(16) })
        list.addView(Pill(this, getString(R.string.trap_done), R.drawable.ic_shield, primary = true).apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(10) })
        // Tomorrow, a reminder to look at the bank's movements: these things sometimes show up a day later.
        if (kind == Breach.CARD || kind == Breach.BANK || kind == Breach.MONEY || kind == Breach.REMOTE) AftercareReminder.set(this, kind)
    }

    private fun header(title: Int, text: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(ImageView(this@AftercareActivity).apply {
            setImageResource(R.drawable.peri_protective)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(130), dp(130)))
        addView(heading(getString(title)).apply { gravity = Gravity.CENTER })
        addView(body(getString(text)).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(4))
        })
    }

    /** One step: its number, what to do, a button that does it if the phone can, and a tick once done. */
    private fun step(kind: Breach, index: Int, text: Int, act: Act?): View {
        val key = "${kind.name}:$index"
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val tick = TextView(this).apply {
            sizeSp(TextSize.BODY)
            typeface = weight(800)
            gravity = Gravity.CENTER
            setTextColor(Palette.WHITE)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        fun paint(ticked: Boolean) {
            tick.text = if (ticked) "✓" else "${index + 1}"
            tick.background = rounded(if (ticked) Palette.GREEN_DARK else Palette.CORAL_INK, dp(17).toFloat())
            card.alpha = if (ticked) 0.75f else 1f
        }
        paint(done.getBoolean(key, false))
        card.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(tick, LinearLayout.LayoutParams(dp(34), dp(34)))
            addView(body(getString(text)).apply {
                setTextColor(Palette.INK)
                setPadding(dp(12), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
        card.addView(LinearLayout(this).apply {
            if (act != null) addView(chipButton(getString(LABELS.getValue(act))) { run(kind, act) }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginEnd = dp(6) })
            addView(chipButton(getString(R.string.after_done)) {
                val now = !done.getBoolean(key, false)
                done.edit().putBoolean(key, now).apply()
                paint(now)
            }, LinearLayout.LayoutParams(if (act != null) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT, dp(48)))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        return card.also {
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        }
    }

    private fun run(kind: Breach, act: Act) {
        val go = when (act) {
            // The dialer, empty: the bank's number is the one on the card, never one from a message.
            Act.DIAL -> Intent(Intent.ACTION_DIAL)
            Act.INTERNET -> Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            Act.APPS -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            Act.REPORT -> Intent(this, ReportActivity::class.java)
            Act.WHATSAPP -> packageManager.getLaunchIntentForPackage("com.whatsapp")
            Act.FAMILY -> {
                hub.needsFamilyHelp()
                hub.guardianPhone()?.let { Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", it, null)) } ?: run {
                    // No guardian's number here: the family's phones already show it, if Peri shares its news;
                    // if not, the dialer, to call someone trusted.
                    if (hub.familySeesPear) {
                        hub.say(getString(R.string.after_family_told))
                        return
                    }
                    hub.say(getString(R.string.after_call_someone))
                    Intent(Intent.ACTION_DIAL)
                }
            }
        } ?: run {
            hub.show(getString(R.string.after_no_app))
            return
        }
        runCatching { startActivity(go) }.onFailure { hub.show(getString(R.string.after_no_app)) }
    }

    companion object {
        private const val EXTRA_KIND = "kind"
        private const val PREFS = "aftercare"

        fun intent(context: Context, kind: Breach?): Intent =
            Intent(context, AftercareActivity::class.java).putExtra(EXTRA_KIND, kind?.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        private val CHOICES = listOf(
            Breach.CODE to R.string.after_pick_code,
            Breach.CARD to R.string.after_pick_card,
            Breach.MONEY to R.string.after_pick_money,
            Breach.BANK to R.string.after_pick_bank,
            Breach.REMOTE to R.string.after_pick_remote,
            Breach.ID to R.string.after_pick_id,
            Breach.PASSWORD to R.string.after_pick_password,
        )

        private val LABELS = mapOf(
            Act.DIAL to R.string.after_act_dial,
            Act.INTERNET to R.string.after_act_internet,
            Act.APPS to R.string.after_act_apps,
            Act.REPORT to R.string.after_act_report,
            Act.FAMILY to R.string.after_act_family,
            Act.WHATSAPP to R.string.after_act_whatsapp,
        )

        private val SUBTITLES = mapOf(
            Breach.CARD to R.string.after_card,
            Breach.BANK to R.string.after_bank,
            Breach.MONEY to R.string.after_money,
            Breach.REMOTE to R.string.after_remote,
            Breach.CODE to R.string.after_code,
            Breach.ID to R.string.after_id,
            Breach.PASSWORD to R.string.after_password,
        )

        /** The steps, the most urgent first. */
        private val STEPS: Map<Breach, List<Pair<Int, Act?>>> = mapOf(
            Breach.CARD to listOf(
                R.string.after_card_1 to Act.DIAL, R.string.after_card_2 to null, R.string.after_family to Act.FAMILY,
                R.string.after_report to Act.REPORT, R.string.after_bank_calls to null,
            ),
            Breach.BANK to listOf(
                R.string.after_bank_1 to Act.DIAL, R.string.after_bank_2 to null, R.string.after_bank_3 to null,
                R.string.after_family to Act.FAMILY, R.string.after_report to Act.REPORT,
            ),
            Breach.MONEY to listOf(
                R.string.after_money_1 to Act.DIAL, R.string.after_money_2 to Act.REPORT, R.string.after_family to Act.FAMILY,
                R.string.after_money_3 to null,
            ),
            Breach.REMOTE to listOf(
                R.string.after_remote_1 to Act.INTERNET, R.string.after_remote_2 to Act.APPS, R.string.after_remote_3 to Act.DIAL,
                R.string.after_remote_4 to null, R.string.after_family to Act.FAMILY,
            ),
            Breach.CODE to listOf(
                R.string.after_code_1 to Act.WHATSAPP, R.string.after_code_2 to null, R.string.after_code_3 to Act.DIAL,
                R.string.after_code_4 to Act.FAMILY,
            ),
            Breach.ID to listOf(
                R.string.after_report to Act.REPORT, R.string.after_id_2 to null, R.string.after_family to Act.FAMILY,
            ),
            Breach.PASSWORD to listOf(
                R.string.after_password_1 to null, R.string.after_password_2 to null, R.string.after_password_3 to null,
                R.string.after_password_4 to null,
            ),
        )
    }
}
