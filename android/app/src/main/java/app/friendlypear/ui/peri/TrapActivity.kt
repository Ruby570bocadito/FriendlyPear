package app.friendlypear.ui.peri

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

/**
 * "How the trap worked": after Peri stops a scam, three Muse Image panels with a sentence under each, so the
 * person sees how that kind of trick works and spots the next one. "Learn" opens it for any kind of trap. The panels are made once per kind of scam
 * (`assets/peri/generate.ts traps`), so nothing is sent anywhere to show them.
 */
class TrapActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // From "Learn", the kind asked for; after a scam, the kind of the scam Peri stopped.
        val chosen = intent.getStringExtra(EXTRA_KIND)
        val kind = chosen ?: hub.lastScam.load()?.kind ?: LINK
        val panels = PANELS[kind] ?: PANELS.getValue(LINK)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@TrapActivity).apply {
                setImageResource(R.drawable.ic_back)
                setColorFilter(Palette.GREEN_DARK)
                contentDescription = getString(R.string.back)
                background = pressable(rounded(Palette.WHITE, dp(24).toFloat()), Palette.RIPPLE_DARK)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(heading(if (chosen != null) getString(NAMES[kind] ?: R.string.trap_title) else getString(R.string.trap_title)).apply {
                setPadding(dp(12), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            // The shield turning green with its tick: this trap is known now.
            rive(RiveArt.SHIELD, 2f)?.let { addView(it, LinearLayout.LayoutParams(dp(64), dp(64))) }
        })
        panels.forEachIndexed { i, (image, caption) ->
            list.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = cardBackground()
                setPadding(dp(14), dp(14), dp(14), dp(18))
                addView(FrameLayout(this@TrapActivity).apply {
                    addView(ImageView(this@TrapActivity).apply {
                        setImageResource(image)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        background = rounded(Palette.CREAM, dp(16).toFloat())
                        clipToOutline = true
                    }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
                    // Its number, in the sun's colour, over the panel's corner.
                    addView(TextView(this@TrapActivity).apply {
                        text = java.text.NumberFormat.getIntegerInstance().format(i + 1)
                        sizeSp(20f)
                        typeface = Fonts.display(700)
                        setTextColor(Palette.INK)
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Palette.SUN)
                            setStroke(dp(3), Palette.WHITE)
                        }
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.TOP or Gravity.START).apply {
                        topMargin = dp(10)
                        marginStart = dp(10)
                    })
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                addView(body(getString(caption)).apply {
                    setTextColor(Palette.INK)
                    setPadding(dp(4), dp(12), dp(4), 0)
                })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        }
        list.addView(Pill(this, getString(R.string.trap_done), R.drawable.ic_shield, primary = true).apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(68)).apply { topMargin = dp(20) })
        list.addView(LinearLayout(this).apply {
            addView(Pill(this@TrapActivity, getString(R.string.welcome_listen), R.drawable.ic_mic, primary = false).apply {
                setOnClickListener { hub.say(panels.joinToString(" ") { getString(it.second) }) }
            }, LinearLayout.LayoutParams(0, dp(58), 1f).apply { marginEnd = dp(6) })
            if (chosen == null && hub.lastScam.load() != null) addView(Pill(this@TrapActivity, getString(R.string.report_card_button), R.drawable.ic_message, primary = false).apply {
                setOnClickListener { startActivity(Intent(this@TrapActivity, ReportActivity::class.java)) }
            }, LinearLayout.LayoutParams(0, dp(58), 1f).apply { marginStart = dp(6) })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })

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

    companion object {
        const val EXTRA_KIND = "kind"
        const val LINK = "link"
        private val COURIERS = setOf("Correos", "SEUR", "DHL", "Royal Mail", "Evri", "UPS", "FedEx", "USPS")
        private val OFFICIALS = setOf("la DGT", "la Agencia Tributaria", "la Seguridad Social", "HMRC", "the DVLA", "the IRS", "the NHS")
        private val UTILITIES = setOf("Iberdrola", "Endesa", "Naturgy")

        /**
         * The kind of trap, from what the shield found: a code, a fake payment, a fake relative or a fake love, remote
         * control, a prize, a fake public office or bill, a number to call, a parcel or a fake link.
         */
        fun kindOf(signals: Collection<String>, brand: String?): String = when {
            "CODE_DURING_CALL" in signals -> "code"
            // A buyer's "safe payment" asks for the card too: its own panels tell that trick better than the code's.
            signals.any { it in setOf("MISTAKEN_PAYMENT", "SAFE_PAYMENT_BAIT") } -> "payment"
            signals.any { it in setOf("CODE_FORWARD_REQUEST", "ONE_TIME_CODE", "CREDENTIAL_REQUEST") } -> "code"
            "NEW_NUMBER_CLAIM" in signals -> "family"
            "ROMANCE_BAIT" in signals -> "romance"
            "SECRECY" in signals -> "family"
            signals.any { it in setOf("REMOTE_ACCESS_APP", "DEVICE_SCARE") } -> "remote"
            "PRIZE_OR_PROFIT" in signals -> "prize"
            brand in OFFICIALS -> "official"
            brand in UTILITIES -> "bill"
            "LINK" !in signals && signals.any { it in setOf("CALLBACK_REQUEST", "COMPANY_FROM_MOBILE") } -> "callback"
            brand in COURIERS -> "parcel"
            else -> LINK
        }

        private val NAMES = mapOf(
            "parcel" to R.string.trap_name_parcel,
            "link" to R.string.trap_name_link,
            "code" to R.string.trap_name_code,
            "family" to R.string.trap_name_family,
            "remote" to R.string.trap_name_remote,
            "prize" to R.string.trap_name_prize,
            "callback" to R.string.trap_name_callback,
            "payment" to R.string.trap_name_payment,
            "official" to R.string.trap_name_official,
            "bill" to R.string.trap_name_bill,
            "romance" to R.string.trap_name_romance,
        )

        private val PANELS: Map<String, List<Pair<Int, Int>>> = mapOf(
            "parcel" to listOf(
                R.drawable.peri_trap_parcel_1 to R.string.trap_parcel_1,
                R.drawable.peri_trap_parcel_2 to R.string.trap_parcel_2,
                R.drawable.peri_trap_parcel_3 to R.string.trap_parcel_3,
            ),
            "link" to listOf(
                R.drawable.peri_trap_link_1 to R.string.trap_link_1,
                R.drawable.peri_trap_link_2 to R.string.trap_link_2,
                R.drawable.peri_trap_link_3 to R.string.trap_link_3,
            ),
            "code" to listOf(
                R.drawable.peri_trap_code_1 to R.string.trap_code_1,
                R.drawable.peri_trap_code_2 to R.string.trap_code_2,
                R.drawable.peri_trap_code_3 to R.string.trap_code_3,
            ),
            "family" to listOf(
                R.drawable.peri_trap_family_1 to R.string.trap_family_1,
                R.drawable.peri_trap_family_2 to R.string.trap_family_2,
                R.drawable.peri_trap_family_3 to R.string.trap_family_3,
            ),
            "remote" to listOf(
                R.drawable.peri_trap_remote_1 to R.string.trap_remote_1,
                R.drawable.peri_trap_remote_2 to R.string.trap_remote_2,
                R.drawable.peri_trap_remote_3 to R.string.trap_remote_3,
            ),
            "prize" to listOf(
                R.drawable.peri_trap_prize_1 to R.string.trap_prize_1,
                R.drawable.peri_trap_prize_2 to R.string.trap_prize_2,
                R.drawable.peri_trap_prize_3 to R.string.trap_prize_3,
            ),
            "callback" to listOf(
                R.drawable.peri_trap_callback_1 to R.string.trap_callback_1,
                R.drawable.peri_trap_callback_2 to R.string.trap_callback_2,
                R.drawable.peri_trap_callback_3 to R.string.trap_callback_3,
            ),
            "payment" to listOf(
                R.drawable.peri_trap_payment_1 to R.string.trap_payment_1,
                R.drawable.peri_trap_payment_2 to R.string.trap_payment_2,
                R.drawable.peri_trap_payment_3 to R.string.trap_payment_3,
            ),
            "official" to listOf(
                R.drawable.peri_trap_official_1 to R.string.trap_official_1,
                R.drawable.peri_trap_official_2 to R.string.trap_official_2,
                R.drawable.peri_trap_official_3 to R.string.trap_official_3,
            ),
            "bill" to listOf(
                R.drawable.peri_trap_bill_1 to R.string.trap_bill_1,
                R.drawable.peri_trap_bill_2 to R.string.trap_bill_2,
                R.drawable.peri_trap_bill_3 to R.string.trap_bill_3,
            ),
            "romance" to listOf(
                R.drawable.peri_trap_romance_1 to R.string.trap_romance_1,
                R.drawable.peri_trap_romance_2 to R.string.trap_romance_2,
                R.drawable.peri_trap_romance_3 to R.string.trap_romance_3,
            ),
        )
    }
}
