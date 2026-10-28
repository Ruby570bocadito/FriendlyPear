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
import android.widget.ScrollView
import app.friendlypear.R

/**
 * Settings: for now, the language of the screens and of Peri's voice. "Same as the phone" follows Android;
 * Spanish or English stay put whatever the phone says (families mix them). Peri still understands both.
 */
class SettingsActivity : Activity() {
    /** Each place's little world, by its key; the chosen one moves. */
    private val worlds = LinkedHashMap<String, PeriWorld>()
    private var chosenPlace = PeriWorld.AUTO
    /** "A surprise every time" shows a different place every couple of seconds. */
    private val surprise = object : Runnable {
        override fun run() {
            worlds[PeriWorld.RANDOM]?.let { w -> w.preview(PeriWorld.Place.entries.filter { it != w.place }.random(), chosenPlace == PeriWorld.RANDOM, ZOOM) }
            window.decorView.postDelayed(this, SURPRISE_MS)
        }
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
                alive()
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
        val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val current = if (supported) chosenLanguage() else ""
        val group = ChoiceGroup(this, columns = 1) { tag -> if (supported) choose(tag) }
        options.forEach { (tag, label) -> group.add(tag, ChoiceTile(this, label).apply { isEnabled = supported }) }
        group.choose(current)
        card.addView(group, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
        if (!supported) card.addView(body(getString(R.string.settings_language_old_android), TextSize.SMALL).apply { setPadding(0, dp(6), 0, 0) })
        list.addView(card, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(14)
        })

        // Peri's world: a different place each day with the season, or the one the person likes best.
        // Each place is shown as a little window onto it, drawn by the world itself; the chosen one moves.
        val places = getSharedPreferences(PeriWorld.PREFS, MODE_PRIVATE)
        chosenPlace = places.getString(PeriWorld.PLACE, PeriWorld.AUTO)
            .takeIf { it == PeriWorld.RANDOM || PeriWorld.Place.entries.any { p -> p.name == it } } ?: PeriWorld.AUTO
        val worldChoice = ChoiceGroup(this, columns = 2) { key ->
            places.edit().putString(PeriWorld.PLACE, key).apply()
            chosenPlace = key
            worlds.forEach { (k, w) -> w.preview(w.place, k == key, ZOOM) }
        }
        listOf(
            PeriWorld.AUTO to R.string.place_auto,
            PeriWorld.RANDOM to R.string.place_random,
            PeriWorld.Place.MEADOW.name to R.string.place_meadow,
            PeriWorld.Place.ORCHARD.name to R.string.place_orchard,
            PeriWorld.Place.SEASIDE.name to R.string.place_seaside,
            PeriWorld.Place.AUTUMN.name to R.string.place_autumn,
            PeriWorld.Place.SNOW.name to R.string.place_snow,
        ).forEach { (key, label) ->
            val shown = when (key) {
                PeriWorld.AUTO -> PeriWorld.seasonal()
                PeriWorld.RANDOM -> PeriWorld.Place.entries.random()
                else -> PeriWorld.Place.valueOf(key)
            }
            val world = PeriWorld(this).apply { preview(shown, key == chosenPlace, ZOOM) }
            worlds[key] = world
            worldChoice.add(key, ChoiceTile(this, getString(label), preview = world))
        }
        worldChoice.done()
        worldChoice.choose(chosenPlace)
        val worldCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(16), dp(18), dp(18))
            clipChildren = false
            addView(heading(getString(R.string.settings_world), TextSize.BODY))
            addView(body(getString(R.string.settings_world_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, dp(10)) })
            addView(worldChoice)
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
            addView(Toggle(this@SettingsActivity, getString(R.string.settings_ears_muse), hub.museVoice) { on -> hub.museVoice = on }.apply {
                isEnabled = hub.museVoiceAvailable
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })
            addView(body(getString(if (hub.museVoiceAvailable) R.string.settings_ears_note else R.string.settings_ears_unavailable), TextSize.SMALL).apply {
                setPadding(0, dp(4), 0, 0)
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
            val picker = ChoiceGroup(this@SettingsActivity, columns = 1) { name -> hub.chooseVoice(name) }
            voices.forEachIndexed { i, v ->
                // Only what tells them apart: the accent if it isn't the usual one, and whether it needs internet.
                val notes = listOfNotNull(
                    getString(R.string.voice_best).takeIf { i == 0 },
                    v.locale.displayCountry.takeIf { it.isNotEmpty() && v.locale.country != voices[0].locale.country },
                    getString(R.string.voice_online).takeIf { v.isNetworkConnectionRequired },
                )
                val label = if (notes.isEmpty()) getString(R.string.voice_label, i + 1)
                else getString(R.string.voice_label_notes, i + 1, notes.joinToString(" · "))
                picker.add(v.name, ChoiceTile(this@SettingsActivity, label, icon = R.drawable.ic_volume))
            }
            picker.choose(hub.voiceName.orEmpty())
            if (voices.isEmpty()) addView(body(getString(R.string.settings_voice_none), TextSize.SMALL))
            addView(picker, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) })
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
            // Its state as a chip, like the permissions': green when on, amber when off.
            addView(body(if (dns != null) getString(R.string.settings_web_on, dns) else getString(R.string.settings_web_off), TextSize.SMALL).apply {
                typeface = weight(600)
                setTextColor(if (dns != null) Palette.GREEN_DARK else Palette.AMBER_INK)
                background = rounded(if (dns != null) Palette.GREEN_TINT else Palette.AMBER_TINT, dp(14).toFloat())
                setPadding(dp(12), dp(4), dp(12), dp(5))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
            if (dns == null) addView(Pill(this@SettingsActivity, getString(R.string.settings_web_setup), R.drawable.ic_shield, primary = false).apply {
                setOnClickListener { saferWeb() }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(10) })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        // Numbers blocked from a scam's card, each with its way back.
        val blocked = hub.blocked
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
                    hub.unblock(sender)
                    row.visibility = View.GONE
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)))
                addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
            }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

        // More from Peri, as in Learn's tools: the phone's security checkup, every permission with what she did with it,
        // and her welcome again for whoever sets the phone up with the person.
        list.addView(actionCard(listOf(
            actionRow(R.drawable.ic_shield, getString(R.string.checkup_open), getString(R.string.tool_checkup_line)) {
                startActivity(android.content.Intent(this, CheckupActivity::class.java))
            },
            actionRow(R.drawable.ic_settings, getString(R.string.settings_access), getString(R.string.settings_access_line)) {
                startActivity(android.content.Intent(this, AccessActivity::class.java))
            },
            actionRow(R.drawable.ic_pear, getString(R.string.settings_welcome_again), getString(R.string.settings_welcome_line)) {
                startActivity(android.content.Intent(this, WelcomeActivity::class.java))
            },
        ), getString(R.string.settings_more)), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })

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

    override fun onResume() {
        super.onResume()
        if (animationsOn()) window.decorView.postDelayed(surprise, SURPRISE_MS)
    }

    override fun onPause() {
        window.decorView.removeCallbacks(surprise)
        super.onPause()
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

    private companion object {
        /** The little worlds are the real one drawn at this size, so hills and trees keep their proportions. */
        const val ZOOM = 0.45f
        const val SURPRISE_MS = 2_200L
    }
}
