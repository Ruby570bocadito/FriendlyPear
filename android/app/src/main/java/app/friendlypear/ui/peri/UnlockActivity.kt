package app.friendlypear.ui.peri

import android.app.Activity
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
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
import app.friendlypear.config.Need
import app.friendlypear.services.MessageListener

/**
 * "Unlock Peri": a guide through Android's restricted settings, which lock message alerts and texts for apps
 * installed from a file (Android 13+). Android only offers "Allow restricted settings" after the person has tried
 * to turn the setting on once, so the steps go in that order: try once, allow, turn it on. Each step opens the
 * exact screen and ticks itself off; one unlock covers both alerts and texts.
 */
class UnlockActivity : Activity() {
    private lateinit var steps: List<Step>
    private lateinit var done: View
    private val prefs by lazy { getSharedPreferences("permissions", MODE_PRIVATE) }

    private class Step(val card: LinearLayout, val badge: TextView, val button: Pill, val isDone: () -> Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        list.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@UnlockActivity).apply {
                setImageResource(R.drawable.peri_ui_unlock_guide)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(96), dp(96)))
            addView(heading(getString(R.string.unlock_title)).apply { setPadding(dp(12), 0, 0, 0) })
        })
        list.addView(body(getString(R.string.unlock_why), TextSize.SMALL).apply { setPadding(dp(4), dp(8), dp(4), dp(4)) })

        steps = listOf(
            step(list, 1, R.string.unlock_step1, R.string.unlock_step1_button, { prefs.getBoolean(TRIED, false) || unlocked(this) }) {
                prefs.edit().putBoolean(TRIED, true).apply()
                openListenerSettings()
            },
            // Done once they've been there: apps can't always read the unlock, and step 3 says to come back if needed.
            step(list, 2, R.string.unlock_step2, R.string.unlock_step2_button, { hasNotificationAccess(this) || prefs.getBoolean(VISITED, false) }) {
                prefs.edit().putBoolean(VISITED, true).apply()
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            },
            step(list, 3, R.string.unlock_step3, R.string.unlock_step3_button, { hasNotificationAccess(this) }) {
                openListenerSettings()
            },
        )

        // All set: Peri says so, and offers texts too, now that Android lets her ask.
        done = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(ImageView(this@UnlockActivity).apply {
                setImageResource(R.drawable.peri_calm_thumbsup)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(120), dp(120)))
            addView(heading(getString(R.string.unlock_done), TextSize.BODY).apply { gravity = Gravity.CENTER })
            addView(Pill(this@UnlockActivity, getString(R.string.unlock_done_button), R.drawable.ic_shield, primary = true).apply {
                setOnClickListener { finishWithTexts() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(12) })
        }
        list.addView(done, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        list.addView(body(getString(R.string.unlock_brands), TextSize.SMALL).apply { setPadding(dp(4), dp(14), dp(4), 0) })

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
        listOf(steps[0].card, steps[1].card, steps[2].card).forEachIndexed { i, v -> v.floatIn(60L + 80L * i) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    /** The first step not done is the one to do now: highlighted, with the big button; done ones get a tick. */
    private fun refresh() {
        val current = steps.indexOfFirst { !it.isDone() }
        steps.forEachIndexed { i, s ->
            val finished = s.isDone() && (current == -1 || i < current)
            s.badge.text = if (finished) "✓" else "${i + 1}"
            (s.badge.background as GradientDrawable).setColor(if (finished || i == current) Palette.GREEN_DARK else Palette.GREEN_TINT)
            s.badge.setTextColor(if (finished || i == current) Palette.WHITE else Palette.GREEN_DARK)
            s.button.visibility = if (i == current) View.VISIBLE else View.GONE
            s.card.alpha = if (i == current || finished) 1f else 0.6f
        }
        done.visibility = if (current == -1) View.VISIBLE else View.GONE
    }

    private fun step(list: LinearLayout, n: Int, text: Int, button: Int, isDone: () -> Boolean, open: () -> Unit): Step {
        val badge = TextView(this).apply {
            this.text = "$n"
            sizeSp(TextSize.BODY)
            typeface = weight(800)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Palette.GREEN_TINT)
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val pill = Pill(this, getString(button), R.drawable.ic_chevron, primary = true).apply {
            setOnClickListener { open() }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(16))
            addView(LinearLayout(this@UnlockActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(badge, LinearLayout.LayoutParams(dp(40), dp(40)))
                addView(body(getText(text), TextSize.SMALL).apply {
                    setTextColor(Palette.INK)
                    setPadding(dp(14), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            })
            addView(pill, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(12) })
            // Any step can be done again by tapping its card.
            isClickable = true
            isFocusable = true
            foreground = pressable(rounded(0, dp(24).toFloat()), Palette.RIPPLE_DARK)
            setOnClickListener { open() }
        }
        list.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        return Step(card, badge, pill, isDone)
    }

    /** Straight to FriendlyPear's own switch where Android has that screen, else the list of apps. */
    private fun openListenerSettings() {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(this, MessageListener::class.java).flattenToString())
        runCatching { startActivity(detail) }.onFailure { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    /** Texts were locked by the same rule: now Android shows its normal question. */
    private fun finishWithTexts() {
        val hub = PeriApp.hub(this)
        if (!hub.permissions.granted(Need.READ_SMS)) prefs.edit().putBoolean(ASK_TEXTS, true).apply()
        hub.show(getString(R.string.unlock_peri_says))
        finish()
    }

    companion object {
        private const val TRIED = "unlock_tried"
        private const val VISITED = "unlock_visited_app_info"
        /** Read by the main screen: ask for texts next time it shows. */
        const val ASK_TEXTS = "unlock_ask_texts"

        fun hasNotificationAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
            return ComponentName(context, MessageListener::class.java).flattenToString() in enabled
        }

        /**
         * Whether Android locks restricted settings for this app: Android 13+ and installed from a file (a
         * download or a file manager), not from a store or `adb`.
         */
        fun restricted(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
            val source = runCatching { context.packageManager.getInstallSourceInfo(context.packageName).packageSource }.getOrNull() ?: return false
            val fromFile = source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE || source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
            return fromFile && !unlocked(context)
        }

        /** The person allowed restricted settings (Android keeps it as an app op); unknown counts as not yet. */
        fun unlocked(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = runCatching {
                ops.unsafeCheckOpNoThrow("android:access_restricted_settings", Process.myUid(), context.packageName)
            }.getOrNull() ?: return hasNotificationAccess(context)
            return mode == AppOpsManager.MODE_ALLOWED || hasNotificationAccess(context)
        }
    }
}
