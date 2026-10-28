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
    private companion object {
        /** Quad9: a non-profit resolver that refuses known scam and malware sites, and keeps no personal data. */
        const val SAFE_DNS = "dns.quad9.net"
    }

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
            addView(heading(getString(R.string.settings_title)).apply { setPadding(dp(12), 0, 0, 0) })
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        card.addView(heading(getString(R.string.settings_language), TextSize.BODY))
        card.addView(body(getString(R.string.settings_language_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(6)) })
        val options = listOf("" to getString(R.string.language_phone), "es" to "Español", "en" to "English")
        val group = RadioGroup(this)
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val current = if (supported) chosenLanguage() else ""
        options.forEach { (tag, label) ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = label
                sizeSp(TextSize.BODY)
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
        if (!supported) card.addView(body(getString(R.string.settings_language_old_android), TextSize.SMALL).apply { setPadding(0, dp(6), 0, 0) })
        list.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })

        // Peri's world: a different place each day with the season, or the one the person likes best.
        val places = getSharedPreferences(PeriWorld.PREFS, MODE_PRIVATE)
        val chosenPlace = places.getString(PeriWorld.PLACE, PeriWorld.AUTO)
            .takeIf { it == PeriWorld.RANDOM || PeriWorld.Place.entries.any { p -> p.name == it } } ?: PeriWorld.AUTO
        val worldCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_world), TextSize.BODY))
            addView(body(getString(R.string.settings_world_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(6)) })
            addView(RadioGroup(this@SettingsActivity).apply {
                listOf(
                    PeriWorld.AUTO to R.string.place_auto,
                    PeriWorld.RANDOM to R.string.place_random,
                    PeriWorld.Place.MEADOW.name to R.string.place_meadow,
                    PeriWorld.Place.ORCHARD.name to R.string.place_orchard,
                    PeriWorld.Place.SEASIDE.name to R.string.place_seaside,
                    PeriWorld.Place.AUTUMN.name to R.string.place_autumn,
                    PeriWorld.Place.SNOW.name to R.string.place_snow,
                ).forEach { (key, label) ->
                    addView(RadioButton(this@SettingsActivity).apply {
                        id = View.generateViewId()
                        text = getString(label)
                        sizeSp(TextSize.BODY)
                        typeface = weight(600)
                        setTextColor(Palette.INK)
                        minHeight = dp(56)
                        isChecked = key == chosenPlace
                        setOnClickListener { places.edit().putString(PeriWorld.PLACE, key).apply() }
                    })
                }
            })
        }
        list.addView(worldCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })

        // Peri's ears: Muse Voice Transcribe understands older voices better, but the order leaves the phone.
        val hub = app.friendlypear.PeriApp.hub(this)
        val ears = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_ears), TextSize.BODY))
            addView(android.widget.CheckBox(this@SettingsActivity).apply {
                text = getString(R.string.settings_ears_muse)
                sizeSp(TextSize.BODY)
                typeface = weight(600)
                setTextColor(Palette.INK)
                isChecked = hub.museVoice
                isEnabled = hub.museVoiceAvailable
                setOnCheckedChangeListener { _, on -> hub.museVoice = on }
            })
            addView(body(getString(if (hub.museVoiceAvailable) R.string.settings_ears_note else R.string.settings_ears_unavailable), TextSize.SMALL).apply {
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
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_voice), TextSize.BODY))
            addView(body(getString(if (hub.googleVoice) R.string.settings_voice_note else R.string.settings_voice_no_google), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(6)) })
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
                    sizeSp(TextSize.BODY)
                    setTextColor(Palette.INK)
                    minHeight = dp(52)
                    isChecked = v.name == hub.voiceName
                    setOnClickListener { hub.chooseVoice(v.name) }
                })
            }
            if (voices.isEmpty()) addView(body(getString(R.string.settings_voice_none), TextSize.SMALL))
            addView(picker)
            addView(Pill(this@SettingsActivity, getString(R.string.settings_voice_more), R.drawable.ic_mic, primary = false).apply {
                setOnClickListener { moreVoices() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(10) })
        }
        list.addView(voiceCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })

        // This phone and Peri's server: its own key from pairing in person, or still the shared test one.
        val paired = app.friendlypear.net.DeviceKey.paired(this)
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_device), TextSize.BODY))
            addView(body(getString(if (paired) R.string.settings_device_paired else R.string.settings_device_shared), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(6)) })
            if (paired) {
                addView(Pill(this@SettingsActivity, getString(R.string.settings_device_unpair), R.drawable.ic_close, primary = false).apply {
                    setOnClickListener { unpair() }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(8) })
            } else {
                addView(Pill(this@SettingsActivity, getString(R.string.settings_device_pair), R.drawable.ic_shield, primary = true).apply {
                    setOnClickListener { askCode() }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(8) })
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        // Safer web for the whole phone: Android's Private DNS with a protective resolver, step by step.
        val dns = privateDns()
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_web), TextSize.BODY))
            addView(body(getString(R.string.settings_web_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(6)) })
            addView(body(if (dns != null) getString(R.string.settings_web_on, dns) else getString(R.string.settings_web_off), TextSize.SMALL).apply {
                typeface = weight(700)
                setTextColor(if (dns != null) Palette.GREEN_DARK else Palette.AMBER_INK)
            })
            if (dns == null) addView(Pill(this@SettingsActivity, getString(R.string.settings_web_setup), R.drawable.ic_shield, primary = false).apply {
                setOnClickListener { saferWeb() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(10) })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        // Numbers blocked from a scam's card, each with its way back.
        val blocked = app.friendlypear.config.BlockList(this)
        if (blocked.all().isNotEmpty()) list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            addView(heading(getString(R.string.settings_blocked), TextSize.BODY))
            addView(body(getString(R.string.settings_blocked_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(6)) })
            blocked.all().forEach { sender ->
                val row = LinearLayout(this@SettingsActivity).apply { gravity = Gravity.CENTER_VERTICAL }
                row.addView(body(sender, TextSize.BODY), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(chipButton(getString(R.string.settings_unblock)) {
                    blocked.remove(sender)
                    row.visibility = View.GONE
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)))
                addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        // Every permission, what for and how to switch it off, and what Peri did with them.
        list.addView(Pill(this, getString(R.string.settings_access), R.drawable.ic_shield, primary = false).apply {
            setOnClickListener { startActivity(android.content.Intent(this@SettingsActivity, AccessActivity::class.java)) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(16) })

        // Peri's welcome again, for whoever sets the phone up with the person.
        list.addView(Pill(this, getString(R.string.settings_welcome_again), R.drawable.ic_pear, primary = false).apply {
            setOnClickListener { startActivity(android.content.Intent(this@SettingsActivity, WelcomeActivity::class.java)) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(10) })

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

    /** The Private DNS name the phone uses, if it's set to one ("strict" mode); null when it's off or automatic. */
    private fun privateDns(): String? {
        val connectivity = getSystemService(android.net.ConnectivityManager::class.java) ?: return null
        return runCatching { connectivity.getLinkProperties(connectivity.activeNetwork)?.privateDnsServerName }.getOrNull()
    }

    /**
     * Android has no switch an app can flip for this: Peri explains the three steps, copies the name to paste and
     * opens the network settings, where "Private DNS" lives.
     */
    private fun saferWeb() {
        // Copied at once, as the steps say, so it's ready to paste whichever button is tapped.
        getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("DNS", SAFE_DNS))
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.web_dns_title)
            .setMessage(getString(R.string.web_dns_steps, SAFE_DNS))
            .setNeutralButton(R.string.web_dns_copy) { _, _ ->
                getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("DNS", SAFE_DNS))
                android.widget.Toast.makeText(this, getString(R.string.web_dns_copied, SAFE_DNS), android.widget.Toast.LENGTH_LONG).show()
            }
            .setPositiveButton(R.string.web_dns_open) { _, _ ->
                runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)) }
                    .onFailure { runCatching { startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) } }
            }
            .setNegativeButton(R.string.pair_not_now, null)
            .show()
            .friendly()
    }

    /** Typing the code `bun pair` printed, for when there's no camera at hand. */
    private fun askCode() {
        val input = android.widget.EditText(this).apply {
            hint = "ABCD-EFGH"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            sizeSp(TextSize.BODY)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.settings_device_pair)
            .setMessage(R.string.settings_device_code)
            .setView(FrameLayout(this).apply {
                setPadding(dp(22), 0, dp(22), 0)
                addView(input)
            })
            .setNegativeButton(R.string.pair_not_now, null)
            .setPositiveButton(R.string.pair_connect) { _, _ ->
                startActivity(android.content.Intent(this, PairActivity::class.java).putExtra(PairActivity.EXTRA_CODE, input.text.toString()))
                finish()
            }
            .show()
            .friendly()
    }

    private fun unpair() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            val ok = app.friendlypear.net.DeviceKey.unpair(this)
            runOnUiThread {
                app.friendlypear.PeriApp.hub(this).show(getString(if (ok) R.string.settings_device_unpaired else R.string.pair_failed))
                if (ok) recreate()
            }
        }
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
