package app.friendlypear.ui.peri

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.rive.runtime.kotlin.core.Fit

/**
 * Rive's gallery (debug builds only): every .riv the app uses, with a button for each value of its input, to see
 * each state and the blend between them without going through the screen that uses it. Open it with
 * `adb shell am start -n app.friendlypear/.ui.peri.RiveDemoActivity`.
 */
class RiveDemoActivity : Activity() {
    /** One animation, and the values worth trying for its input (just "Again" for one that only plays). */
    private class Entry(val title: String, val art: RiveArt, val values: List<Pair<String, Float>>)

    private val entries = listOf(
        Entry("Shield (the pause, a trap explained)", RiveArt.SHIELD, listOf("Watching" to 0f, "Danger" to 1f, "Safe" to 2f)),
        Entry("Stars (the quiz's score)", RiveArt.STARS, listOf("0" to 0f, "3" to 3f, "5" to 5f, "6" to 6f)),
        Entry("Magnifying glass (a picture)", RiveArt.SCAN, listOf("Look" to 1f, "Fine" to 2f, "Doubt" to 3f, "Trap" to 4f)),
        Entry("Ring (the checkup)", RiveArt.RING, listOf("30%" to 30f, "70%" to 70f, "100%" to 100f)),
        Entry("Pulse (the \"Protecting you\" chip)", RiveArt.PULSE, listOf("Protecting" to 1f, "Set up" to 0f)),
        Entry("Burst (a badge, a great quiz)", RiveArt.BURST, listOf("Again" to 0f)),
        Entry("Pencil (the report being written)", RiveArt.WRITE, listOf("Again" to 0f)),
        Entry("Tab: Peri", RiveArt.TAB_PERI, listOf("Pick" to 1f, "Leave" to 0f)),
        Entry("Tab: Diary", RiveArt.TAB_DIARY, listOf("Pick" to 1f, "Leave" to 0f)),
        Entry("Tab: Learn", RiveArt.TAB_LEARN, listOf("Pick" to 1f, "Leave" to 0f)),
        Entry("Tab: Family", RiveArt.TAB_FAMILY, listOf("Pick" to 1f, "Leave" to 0f)),
        Entry("Microphone (\"Talk to Peri\", white)", RiveArt.VOICE, listOf("Listen" to 1f, "Stop" to 0f)),
        Entry("Shield glint (the shields chip)", RiveArt.GLINT, listOf("Again" to 0f)),
        Entry("Link (pairing with the family)", RiveArt.LINK, listOf("Wait" to 0f, "Connect" to 1f, "Done" to 2f, "Fail" to 3f)),
        Entry("Send arrow (the chat, white)", RiveArt.SEND, listOf("Send" to 1f, "Again" to 2f)),
        Entry("Next arrow (the welcome, white)", RiveArt.NEXT, listOf("Again" to 0f)),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            addView(heading("Rive gallery"))
            addView(body("Each file is made by assets/rive (vector, a state machine with one number input)."))
            entries.forEach { addView(card(it)) }
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Palette.CREAM)
            addView(column)
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        })
    }

    private fun card(entry: Entry) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = cardBackground()
        setPadding(dp(16), dp(14), dp(16), dp(18))
        addView(body(entry.title))
        // The app's own way to make one (RiveArt.kt): it loads Rive the first time, and gives the memory back on closing.
        val view = rive(entry.art) ?: return@apply addView(body("Off: animations are off on this phone, or Rive can't load here."))
        addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(220)).apply { topMargin = dp(8) })
        addView(LinearLayout(this@RiveDemoActivity).apply {
            gravity = Gravity.CENTER
            entry.values.forEach { (label, value) ->
                addView(TextView(this@RiveDemoActivity).apply {
                    text = label
                    sizeSp(16f)
                    setTextColor(Palette.WHITE)
                    gravity = Gravity.CENTER
                    background = pressable(rounded(Palette.GREEN_DARK, dp(22).toFloat()), Palette.RIPPLE_DARK)
                    setPadding(dp(14), 0, dp(14), 0)
                    setOnClickListener {
                        // No input: it plays as it appears, so it's loaded again.
                        if (entry.art.input.isEmpty()) view.setRiveResource(entry.art.raw, stateMachineName = entry.art.machine, fit = Fit.CONTAIN)
                        else view.show(entry.art, value)
                    }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)).apply { marginStart = dp(4); marginEnd = dp(4) })
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(16)
        }
    }
}
