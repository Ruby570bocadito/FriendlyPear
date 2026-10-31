package app.friendlypear.ui.peri

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.core.agent.PauseAnswer

/**
 * The safety pause: a full screen over the money or remote-control app the person just opened, a while after
 * something worrying. Peri, protective, says why; the big button calls the family to check, "Leave this app"
 * goes to the home screen, and "Continue anyway" unlocks after a few seconds. The person always decides.
 * It needs "appear on top" (the same as Mini Peri); without it Peri only speaks.
 */
class PauseOverlay(context: Context, private val onAnswer: (PauseAnswer) -> Unit) {
    private val context = context.applicationContext
    private val windows = this.context.getSystemService(WindowManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var shown: View? = null
    /** Shown about the call on the line (its "hang up" button): it goes when that call ends. */
    private var forCall = false

    fun show(text: String, callName: String?, hangUp: Boolean = false, keep: Boolean = false) {
        showing(text, callName, hangUp, keep)
        forCall = hangUp
    }

    /** The call ended (hung up by Peri, by the person or by the caller): a pause about it has nothing left to do. */
    fun callEnded() {
        if (forCall) hide()
        forCall = false
    }

    private fun showing(text: String, callName: String?, hangUp: Boolean, keep: Boolean) {
        if (!Settings.canDrawOverlays(context)) return
        hide()
        val c = context
        val face = PeriFace(c).apply { show(Mood.PROTECTIVE) }
        val said = TextView(c).apply {
            this.text = text
            sizeSp(TextSize.BODY)
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.12f)
            gravity = Gravity.CENTER
            val tail = c.dp(14).toFloat()
            background = BubbleDrawable(Palette.WHITE, Palette.LINE, c.dp(1).toFloat(), c.dp(24).toFloat(), tail, BubbleDrawable.Tail.TOP)
            setPadding(c.dp(22), c.dp(16) + tail.toInt(), c.dp(22), c.dp(18))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE
        }
        val title = c.heading(c.getString(R.string.pause_title)).apply { gravity = Gravity.CENTER }
        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = ground()
            setPadding(c.dp(20), c.dp(48), c.dp(20), c.dp(32))
            clipChildren = false
            // The red shield beside the title, shaking a little: something here needs a second look.
            val shield = c.rive(RiveArt.SHIELD, 1f)
            if (shield == null) addView(title) else addView(LinearLayout(c).apply {
                gravity = Gravity.CENTER
                addView(shield, LinearLayout.LayoutParams(c.dp(64), c.dp(64)))
                addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = c.dp(8)
                })
            })
            addView(face, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        // A stranger on the line: hanging up comes first, the biggest button.
        if (hangUp) {
            root.addView(
                Pill(c, c.getString(R.string.pause_hang_up), R.drawable.ic_call, primary = true, rubber = Rubber.CORAL).apply {
                    setOnClickListener { answer(PauseAnswer.HANG_UP) }
                },
                buttonParams(80, top = 18),
            )
        }
        // The family asked Peri to hang up: "keep talking" stays the person's choice, right under it.
        if (keep) {
            root.addView(
                Pill(c, c.getString(R.string.pause_keep), R.drawable.ic_call, primary = false).apply {
                    setOnClickListener { answer(PauseAnswer.KEEP_CALL) }
                },
                buttonParams(64, top = 12),
            )
            windows.addView(root, overlayParams())
            shown = root
            return
        }
        if (callName != null) {
            root.addView(
                Pill(c, c.getString(R.string.pause_call, callName), R.drawable.ic_family, primary = true).apply {
                    setOnClickListener { answer(PauseAnswer.CALL_FAMILY) }
                },
                buttonParams(80, top = 18),
            )
        }
        root.addView(
            Pill(c, c.getString(R.string.pause_leave), R.drawable.ic_shield, primary = callName == null && !hangUp).apply {
                setOnClickListener {
                    context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    answer(PauseAnswer.LEAVE)
                }
            },
            buttonParams(if (callName == null && !hangUp) 80 else 64, top = 12),
        )
        // A speed bump, not a wall: "continue" unlocks after a few seconds.
        val carryOn = TextView(c).apply {
            sizeSp(TextSize.BODY)
            typeface = weight(600)
            setTextColor(Palette.INK_SOFT)
            gravity = Gravity.CENTER
            isEnabled = false
            alpha = 0.75f
            setPadding(0, c.dp(14), 0, c.dp(6))
            setOnClickListener { answer(PauseAnswer.CONTINUE) }
        }
        root.addView(carryOn, buttonParams(56, top = 4))
        countdown(carryOn, WAIT_SECONDS)

        windows.addView(root, overlayParams())
        shown = root
    }

    private fun overlayParams() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )

    fun hide() {
        main.removeCallbacksAndMessages(null)
        shown?.let { runCatching { windows.removeView(it) } }
        shown = null
    }

    private fun answer(choice: PauseAnswer) {
        hide()
        onAnswer(choice)
    }

    private fun countdown(view: TextView, left: Int) {
        if (left <= 0) {
            view.text = context.getString(R.string.pause_continue)
            view.isEnabled = true
            view.alpha = 1f
            return
        }
        view.text = context.getString(R.string.pause_continue_in, left)
        main.postDelayed({ countdown(view, left - 1) }, 1_000)
    }

    private fun buttonParams(height: Int, top: Int) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dp(height)).apply { topMargin = context.dp(top) }

    private companion object {
        const val WAIT_SECONDS = 5
    }
}
