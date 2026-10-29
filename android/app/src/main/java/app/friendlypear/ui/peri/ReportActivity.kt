package app.friendlypear.ui.peri

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.core.model.Lang
import app.friendlypear.net.ScamReport

/**
 * "Report it": the scam Peri stopped, written up by Muse Spark for the police or a fraud helpline, ready to send,
 * copy or show at the station. Nothing leaves the phone until the person taps "Write it" (the screen says what
 * goes where first). Where to take it is fixed text, so a phone number is never made up.
 */
class ReportActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }
    private lateinit var state: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Out of captures and screen sharing: the scam and what was lost, for the police.
        Guard.secure(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@ReportActivity).apply {
                setImageResource(R.drawable.peri_report)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alive()
            }, LinearLayout.LayoutParams(dp(96), dp(96)))
            addView(heading(getString(R.string.report_title)).apply { setPadding(dp(12), 0, 0, 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
        state = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(state, full(top = 8))

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
        if (hub.lastScam.load() == null) showNothing() else showAsk()
        list.floatIn()
    }

    private fun full(top: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }

    private fun button(height: Int = 60, top: Int = 12) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(height)).apply { topMargin = dp(top) }

    private fun showNothing() {
        state.removeAllViews()
        state.addView(body(getString(R.string.report_none)))
    }

    /** What will be written and where it goes, before anything is sent. */
    private fun showAsk() {
        state.removeAllViews()
        state.addView(body(getString(R.string.report_intro)).apply { setTextColor(Palette.INK) })
        // What goes in it, each with its picture, then where it goes to be written: all before anything is sent.
        state.addView(whiteCard().apply {
            addView(heading(getString(R.string.report_includes), TextSize.BODY))
            listOf(
                R.drawable.ic_message to R.string.report_inc_message,
                R.drawable.ic_call to R.string.report_inc_sender,
                R.drawable.ic_warning to R.string.report_inc_why,
                R.drawable.ic_shield to R.string.report_inc_where,
            ).forEach { (icon, text) ->
                addView(LinearLayout(this@ReportActivity).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(10), 0, 0)
                    addView(badge(icon, Palette.SKY_TINT, Palette.SKY_INK, 36), LinearLayout.LayoutParams(dp(36), dp(36)))
                    addView(body(getString(text)).apply {
                        setTextColor(Palette.INK)
                        setPadding(dp(12), 0, 0, 0)
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                })
            }
        }, full(top = 14))
        state.addView(LinearLayout(this).apply {
            setPadding(dp(4), 0, dp(4), 0)
            addView(ImageView(this@ReportActivity).apply {
                setImageResource(R.drawable.ic_lock_open)
                setColorFilter(Palette.INK_SOFT)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { topMargin = dp(2) })
            addView(body(getString(R.string.report_privacy), TextSize.SMALL).apply { setPadding(dp(10), 0, 0, 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, full(top = 12))
        state.addView(Pill(this, getString(R.string.report_write), R.drawable.ic_message, primary = true).apply {
            setOnClickListener { write() }
        }, button(height = 68, top = 18))
    }

    private fun write() {
        state.removeAllViews()
        state.addView(body(getString(R.string.report_writing)).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE })
        // The report's place, with grey lines where the words will be.
        state.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(18), dp(18), dp(18))
            listOf(0.6f, 1f, 0.95f, 1f, 0.7f, 1f, 0.85f).forEach { share ->
                addView(View(this@ReportActivity).apply { background = rounded(SKELETON, dp(6).toFloat()) },
                    LinearLayout.LayoutParams(0, dp(16)).apply {
                        width = (resources.displayMetrics.widthPixels * 0.75f * share).toInt()
                        bottomMargin = dp(12)
                    })
            }
        }, full(top = 12))
        hub.writeReport { report -> if (!isFinishing) if (report == null) showFailed() else showReport(report) }
    }

    private fun showFailed() {
        state.removeAllViews()
        state.addView(body(getString(R.string.report_failed)).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE })
        state.addView(Pill(this, getString(R.string.report_retry), R.drawable.ic_message, primary = false).apply {
            setOnClickListener { write() }
        }, button())
    }

    private fun showReport(report: ScamReport) {
        state.removeAllViews()
        state.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(report.title, TextSize.BODY))
            addView(body(report.body).apply {
                setTextColor(Palette.INK)
                setTextIsSelectable(true)
                setPadding(0, dp(8), 0, 0)
            })
        }, full(top = 4))
        val text = "${report.title}\n\n${report.body}\n\n${getString(R.string.report_where)}:\n" + report.where.joinToString("\n") { "- $it" }
        state.addView(Pill(this, getString(R.string.report_share), R.drawable.ic_message, primary = true).apply {
            setOnClickListener {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, report.title).putExtra(Intent.EXTRA_TEXT, text)
                startActivity(Intent.createChooser(send, getString(R.string.report_share)))
            }
        }, button(height = 68, top = 16))
        state.addView(Pill(this, getString(R.string.report_copy), R.drawable.ic_message, primary = false).apply {
            setOnClickListener {
                val clip = ClipData.newPlainText(report.title, text)
                // The scam and what was lost: Android 13 and later keep it out of the preview of what was copied.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                }
                getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                Toast.makeText(this@ReportActivity, R.string.report_copied, Toast.LENGTH_LONG).show()
            }
        }, button())
        // In Spain, the 017 helpline is one tap away; the dialler opens with the number, the person calls.
        if (hub.lang == Lang.ES) state.addView(Pill(this, getString(R.string.report_call_017), R.drawable.ic_call, primary = false).apply {
            setOnClickListener { runCatching { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:017"))) } }
        }, button())
        state.addView(heading(getString(R.string.report_where), TextSize.BODY), full(top = 24))
        state.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(6), dp(18), dp(6))
            report.where.forEachIndexed { i, line ->
                if (i > 0) addView(View(this@ReportActivity).apply { setBackgroundColor(RULE) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
                addView(body(line, TextSize.SMALL).apply {
                    setTextColor(Palette.INK)
                    setPadding(0, dp(10), 0, dp(10))
                })
            }
        }, full(top = 10))
        hub.say(getString(R.string.report_ready))
    }

    private companion object {
        val SKELETON = 0xFFEDE4D0.toInt()
        val RULE = 0xFFF3ECDD.toInt()
    }
}
