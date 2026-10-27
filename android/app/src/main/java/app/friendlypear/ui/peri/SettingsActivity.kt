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
                sizeSp(20f)
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

        // Peri's ears: Muse Voice Transcribe understands older voices better, but the order leaves the phone.
        val hub = app.friendlypear.PeriApp.hub(this)
        val ears = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            elevation = dp(2).toFloat()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_ears), 21f))
            addView(android.widget.CheckBox(this@SettingsActivity).apply {
                text = getString(R.string.settings_ears_muse)
                sizeSp(19f)
                typeface = weight(600)
                setTextColor(Palette.INK)
                isChecked = hub.museVoice
                isEnabled = hub.museVoiceAvailable
                setOnCheckedChangeListener { _, on -> hub.museVoice = on }
            })
            addView(body(getString(if (hub.museVoiceAvailable) R.string.settings_ears_note else R.string.settings_ears_unavailable), 16f).apply {
                setPadding(dp(6), dp(4), 0, 0)
            })
        }
        list.addView(ears, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })

        // Peri's voice: every voice this phone has for her language, to hear and pick the nicest.
        val voiceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            elevation = dp(2).toFloat()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_voice), 21f))
            addView(body(getString(if (hub.googleVoice) R.string.settings_voice_note else R.string.settings_voice_no_google), 16f).apply { setPadding(0, dp(4), 0, dp(6)) })
            val voices = hub.voices()
            val picker = RadioGroup(this@SettingsActivity)
            voices.forEachIndexed { i, v ->
                picker.addView(RadioButton(this@SettingsActivity).apply {
                    id = View.generateViewId()
                    // Only what tells them apart: the accent if it isn't the usual one, and whether it needs internet.
                    val notes = listOfNotNull(
                        getString(R.string.voice_best).takeIf { i == 0 },
                        v.locale.displayCountry.takeIf { it.isNotEmpty() && v.locale.country != voices[0].locale.country },
                        getString(R.string.voice_online).takeIf { v.isNetworkConnectionRequired },
                    )
                    text = if (notes.isEmpty()) getString(R.string.voice_label, i + 1)
                    else getString(R.string.voice_label_notes, i + 1, notes.joinToString(" · "))
                    sizeSp(18f)
                    setTextColor(Palette.INK)
                    minHeight = dp(52)
                    isChecked = v.name == hub.voiceName
                    setOnClickListener { hub.chooseVoice(v.name) }
                })
            }
            if (voices.isEmpty()) addView(body(getString(R.string.settings_voice_none), 16f))
            addView(picker)
            addView(Pill(this@SettingsActivity, getString(R.string.settings_voice_more), R.drawable.ic_mic, primary = false, size = 17f).apply {
                setOnClickListener { moreVoices() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(10) })
        }
        list.addView(voiceCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
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

    /**
     * More and better voices: Android's speech settings (to pick Google's engine and download voices), or, if the
     * phone hasn't got Google's engine, its page in the Play Store.
     */
    private fun moreVoices() {
        val google = runCatching { packageManager.getPackageInfo(app.friendlypear.device.GOOGLE_TTS, 0) }.isSuccess
        val intent = if (google) android.content.Intent("com.android.settings.TTS_SETTINGS")
        else android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${app.friendlypear.device.GOOGLE_TTS}"))
        runCatching { startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) } }
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
