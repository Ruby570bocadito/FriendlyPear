package app.friendlypear.ui.peri

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.friendlypear.BuildConfig
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.net.DeviceKey
import java.util.concurrent.Executors

/**
 * Opened by the phone's camera from the QR that `bun pair` shows (friendlypear://pair?code=…&server=…), or from
 * Settings with a typed code. It says which server and asks before connecting; a QR for any other server is
 * refused, so a stranger's QR can't send Peri's messages somewhere else.
 */
class PairActivity : Activity() {
    /** Its decisive buttons ignore touches through another app's window drawn over them; Peri says why. */
    private val guard = Guard(this)
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var connect: Pill
    private lateinit var later: Pill

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Out of captures and screen sharing: the pairing code.
        Guard.secure(this)
        val uri: Uri? = intent.data
        val code = (uri?.getQueryParameter("code") ?: intent.getStringExtra(EXTRA_CODE)).orEmpty().trim().uppercase()
        val server = uri?.getQueryParameter("server") ?: BuildConfig.BACKEND_URL
        val ours = DeviceKey.isOurServer(server)
        val valid = DeviceKey.isCode(code)

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        list.addView(ImageView(this).apply {
            setImageResource(if (ours && valid) R.drawable.peri_shield else R.drawable.peri_worried)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(170), dp(170)))
        list.addView(heading(getString(R.string.pair_title)).apply { gravity = Gravity.CENTER })
        list.addView(body(getString(R.string.pair_text)).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(4))
        })
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(14), dp(18), dp(14))
            addView(body(getString(R.string.pair_server, Uri.parse(server).host ?: server), TextSize.SMALL).apply { setTextColor(Palette.INK) })
            addView(heading(code.ifEmpty { "—" }).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.08f
                setTextColor(Palette.GREEN_DARK)
                setPadding(0, dp(8), 0, 0)
            })
        }
        list.addView(details, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        status = body("", TextSize.SMALL).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        list.addView(status)
        connect = guard.button(Pill(this, getString(R.string.pair_connect), R.drawable.ic_shield, primary = true)).apply {
            setOnClickListener { pair(code) }
        }
        list.addView(connect, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(70)).apply { topMargin = dp(14) })
        later = Pill(this, getString(R.string.pair_not_now), R.drawable.ic_close, primary = false).apply {
            setOnClickListener { finish() }
        }
        list.addView(later, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(12) })

        when {
            !ours -> refuse(getString(R.string.pair_other_server))
            !valid -> refuse(getString(R.string.pair_bad_code))
        }

        val pad = dp(22)
        val scroll = ScrollView(this).apply { addView(list) }
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

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }

    private fun refuse(why: String) {
        status.text = why
        status.setTextColor(Palette.AMBER_INK)
        connect.visibility = View.GONE
    }

    private fun pair(code: String) {
        connect.isEnabled = false
        connect.alpha = 0.5f
        status.text = getString(R.string.pair_working)
        io.execute {
            val outcome = DeviceKey.pair(this, code)
            runOnUiThread {
                val hub = PeriApp.hub(this)
                when (outcome) {
                    DeviceKey.Outcome.PAIRED -> {
                        status.text = getString(R.string.pair_done)
                        status.setTextColor(Palette.GREEN_DARK)
                        connect.visibility = View.GONE
                        later.label = getString(R.string.guardian_back)
                        hub.say(getString(R.string.pair_done))
                    }
                    DeviceKey.Outcome.REFUSED, DeviceKey.Outcome.BAD_CODE -> refuse(getString(R.string.pair_refused))
                    DeviceKey.Outcome.FAILED -> {
                        status.text = getString(R.string.pair_failed)
                        connect.isEnabled = true
                        connect.alpha = 1f
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_CODE = "code"
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        guard.check(ev)
        return super.dispatchTouchEvent(ev)
    }
}
