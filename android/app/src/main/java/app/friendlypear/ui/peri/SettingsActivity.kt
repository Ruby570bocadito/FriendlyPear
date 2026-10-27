package app.friendlypear.ui.peri

import android.app.Activity
import android.app.LocaleManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import app.friendlypear.R

/**
 * Settings: for now, the language of the screens and of Peri's voice. "Same as the phone" follows Android;
 * Spanish or English stay put whatever the phone says (families mix them). Peri still understands both.
 */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        list.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@SettingsActivity).apply {
                setImageResource(R.drawable.peri_calm_wave1)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(96), dp(96)))
            addView(heading(getString(R.string.settings_title), 28f).apply { setPadding(dp(12), 0, 0, 0) })
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            elevation = dp(2).toFloat()
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        card.addView(heading(getString(R.string.settings_language), 21f))
        card.addView(body(getString(R.string.settings_language_note), 16f).apply { setPadding(0, dp(4), 0, dp(6)) })
        val options = listOf("" to getString(R.string.language_phone), "es" to "Español", "en" to "English")
        val group = RadioGroup(this)
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val current = if (supported) chosenLanguage() else ""
        options.forEach { (tag, label) ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = label
                textSize = 20f
                typeface = weight(600)
                setTextColor(Palette.INK)
                minHeight = dp(56)
                isEnabled = supported
                isChecked = tag == current
                setOnClickListener { if (supported) choose(tag) }
                if (tag.isNotEmpty()) textLocale = java.util.Locale(tag)
            })
        }
        card.addView(group)
        if (!supported) card.addView(body(getString(R.string.settings_language_old_android), 16f).apply { setPadding(0, dp(6), 0, 0) })
        list.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })

        val pad = dp(20)
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            addView(list)
        }
        setContentView(FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.CREAM, Palette.MEADOW))
            addView(scroll)
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                scroll.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
                insets
            }
        })
    }

    /** "" when the app follows the phone, else "es" or "en". */
    private fun chosenLanguage(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return ""
        val locales = getSystemService(LocaleManager::class.java)?.applicationLocales ?: return ""
        return if (locales.isEmpty) "" else locales[0].language
    }

    /** Android saves the choice and redraws every screen in the new language; Peri switches her voice too. */
    private fun choose(tag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || tag == chosenLanguage()) return
        getSystemService(LocaleManager::class.java)?.applicationLocales =
            if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
    }
}
