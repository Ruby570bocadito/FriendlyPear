package app.friendlypear.ui.peri

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.config.FamilyStore
import app.friendlypear.config.InvestigationLog
import app.friendlypear.config.Need
import app.friendlypear.config.PhoneContacts
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.Relation
import app.friendlypear.core.trust.CircleRules
import app.friendlypear.core.trust.FamilyConfig
import java.text.DateFormat
import java.util.Date

/**
 * Family setup, done together on the protected person's phone: who the guardians are, each person's relation
 * and nicknames, and whether the family agreed a secret word. Changes are saved at once and stay on the phone.
 */
class FamilyActivity : Activity() {
    private lateinit var store: FamilyStore
    private var config = FamilyConfig()
    private lateinit var status: TextView
    private lateinit var cancel: View
    private var names: Map<String, String> = emptyMap()
    private var newsList: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Out of captures and screen sharing: who the family trusts.
        Guard.secure(this)
        store = FamilyStore(this)
        config = store.load()
        // Once someone protects the person, changing it needs the phone's own lock: not anyone holding the phone.
        if (config.guardians.isNotEmpty() && savedInstanceState == null) {
            setContentView(View(this).apply { setBackgroundColor(Palette.CREAM) })
            confirmIdentity(getString(R.string.family_identity_title), getString(R.string.family_identity_subtitle), { build() }, { finish() })
        } else {
            build()
        }
    }

    private fun build() {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        list.addView(illustrated(R.drawable.peri_family, heading(getString(R.string.family_title)), body(getString(R.string.family_intro))))
        status = body("").apply {
            setTextColor(Palette.AMBER_INK)
            background = rounded(Palette.AMBER_TINT, dp(18).toFloat())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        cancel = Pill(this, getString(R.string.circle_cancel), R.drawable.ic_close, primary = false).apply {
            setOnClickListener {
                store.savePending(null)
                showPending()
            }
        }
        list.addView(status, spaced())
        list.addView(cancel, spaced().apply { height = dp(56) })

        list.addView(card().apply {
            addView(Toggle(this@FamilyActivity, getString(R.string.family_word_agreed), config.hasFamilyWord) { checked ->
                update(config.copy(hasFamilyWord = checked))
            })
            addView(body(getString(R.string.family_word_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, 0) })
        }, spaced())

        // Sharing Peri's news with the guardians is the person's choice, off until they turn it on.
        val hub = PeriApp.hub(this)
        val shared = card()
        // The switch shows what the server ended up with, which may not be what was asked (no network).
        lateinit var sharing: Toggle
        sharing = Toggle(this, getString(R.string.share_news), hub.news.sharing) { checked ->
            sharing.isEnabled = false
            hub.shareNews(checked) { on ->
                sharing.set(on)
                sharing.isEnabled = true
                showNews()
            }
        }
        shared.addView(sharing)
        shared.addView(body(getString(R.string.share_news_note), TextSize.SMALL).apply { setPadding(0, dp(4), 0, 0) })
        newsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        shared.addView(newsList)
        list.addView(shared, spaced())
        showNews()

        // With many contacts, the likely family first, and a search for anyone else. Rows are built when shown.
        val contacts = PhoneContacts.load(this)
        names = contacts.associate { it.id to it.name }
        if (contacts.isEmpty()) {
            // Without contacts there's no one to choose: the way to fix it sits right where the people will appear.
            contactsMissing = !canReadContacts()
            list.addView(card().apply {
                addView(body(getString(if (contactsMissing) R.string.family_no_contacts else R.string.family_contacts_empty)).apply { setTextColor(Palette.INK) })
                if (contactsMissing) addView(Pill(this@FamilyActivity, getString(R.string.family_allow_contacts), R.drawable.ic_family, primary = true).apply {
                    setOnClickListener { askContacts() }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(14) })
            }, spaced())
        } else {
            list.addView(heading(getString(R.string.family_people_title), TextSize.BODY), spaced())
            val people = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val rows = HashMap<String, View>()
            fun showPeople(query: String) {
                people.removeAllViews()
                val q = fold(query)
                val chosen = if (q.isEmpty()) contacts.filter { likelyFamily(it) } else contacts.filter { fold(it.name).contains(q) }.take(MAX_RESULTS)
                if (chosen.isEmpty()) people.addView(body(getString(if (q.isEmpty()) R.string.family_search_empty else R.string.family_search_none), TextSize.SMALL), spaced())
                chosen.forEach { c -> people.addView(rows.getOrPut(c.id) { row(c.id, c.name, c.relation) }, spaced()) }
            }
            list.addView(EditText(this).apply {
                hint = getString(R.string.family_search_hint)
                sizeSp(TextSize.BODY)
                setTextColor(Palette.INK)
                isSingleLine = true
                background = rounded(Palette.WHITE, dp(16).toFloat(), Palette.LINE_STRONG, dp(1.5f))
                setPadding(dp(16), dp(14), dp(16), dp(14))
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) = showPeople(s?.toString().orEmpty())
                })
            }, spaced())
            list.addView(people)
            showPeople("")
        }

        // What the investigator looked into, so the family can see why Peri warned. Never the message itself.
        val looked = InvestigationLog(this).load()
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val report = card().apply {
            addView(illustrated(R.drawable.peri_report, heading(getString(R.string.investigations_title), TextSize.BODY), null, size = 72))
            if (looked.isEmpty()) addView(body(getString(R.string.investigations_empty), TextSize.SMALL).apply { setPadding(0, dp(8), 0, 0) })
            looked.forEach { e ->
                val verdict = getString(VERDICTS[e.verdict] ?: R.string.verdict_doubt)
                addView(heading("${format.format(Date(e.at))} · $verdict", TextSize.BODY).apply { setPadding(0, dp(14), 0, dp(2)) })
                addView(body(e.evidence.joinToString("\n") { "•  $it" }, TextSize.SMALL))
            }
        }
        list.addView(report, spaced())

        val pad = dp(20)
        // A cream strip under the status bar, so the list scrolls behind it rather than under the clock.
        val scrim = View(this).apply { setBackgroundColor(Palette.CREAM) }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            addView(list)
        }
        setContentView(FrameLayout(this).apply {
            background = ground()
            addView(scroll)
            addView(scrim, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 0))
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
                scroll.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom)
                scrim.layoutParams = scrim.layoutParams.apply { height = bars.top }
                insets
            }
        })
        showPending()
    }

    /** Exactly what the guardians see, while sharing is on: the person always knows. */
    private fun showNews() {
        val box = newsList ?: return
        box.removeAllViews()
        val hub = PeriApp.hub(this)
        if (!hub.news.sharing) return
        box.addView(heading(getString(R.string.news_title), TextSize.BODY).apply { setPadding(0, dp(14), 0, dp(4)) })
        val entries = hub.news.load()
        if (entries.isEmpty()) box.addView(body(getString(R.string.news_empty), TextSize.SMALL))
        // The latest ones, as a timeline like the diary's, each channel in words.
        timeline(box, entries.sortedByDescending { it.at }.take(MAX_NEWS).map { e ->
            val channel = getString(CHANNELS[e.channel] ?: R.string.news_channel_message)
            when {
                e.kind == "warning" && e.level == "scam" -> Moment(e.at, R.drawable.ic_shield, Palette.CORAL_TINT, Palette.CORAL_INK, getString(R.string.news_scam, channel))
                e.kind == "warning" -> Moment(e.at, R.drawable.ic_message, Palette.SUN_TINT, Palette.AMBER_INK, getString(R.string.news_doubt, channel))
                e.answer == "call_family" -> Moment(e.at, R.drawable.ic_call, Palette.GREEN_TINT, Palette.GREEN_DARK, getString(R.string.news_pause_call))
                e.answer == "leave" -> Moment(e.at, R.drawable.ic_bank, Palette.GREEN_TINT, Palette.GREEN_DARK, getString(R.string.news_pause_leave))
                else -> Moment(e.at, R.drawable.ic_bank, Palette.SUN_TINT, Palette.AMBER_INK, getString(R.string.news_pause_continue))
            }
        }, Palette.LILAC_INK)
    }

    /** A pending change of guardians, in plain words, with its state and a way to cancel it. */
    private fun showPending() {
        val pending = store.loadPending()
        cancel.visibility = if (pending == null) View.GONE else View.VISIBLE
        if (pending == null) {
            status.visibility = View.GONE
            return
        }
        val change = (CircleRules.added(config, pending).map { getString(R.string.circle_add, names[it] ?: it) } +
            CircleRules.removed(config, pending).map { getString(R.string.circle_remove, names[it] ?: it) }).joinToString(", ")
        val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(pending.effectiveAt))
        status.text = if (pending.approved) getString(R.string.circle_due, change, date) else getString(R.string.circle_waiting, change)
        status.visibility = View.VISIBLE
    }

    /** Changing who the guardians are waits for a guardian's yes and 48 hours; the switch goes back meanwhile ([revert]). */
    private fun onGuardianTicked(revert: () -> Unit, id: String, name: String, checked: Boolean) {
        val next = config.withGuardian(id, checked)
        if (!CircleRules.needsGuardians(config, next)) return update(next)
        revert()
        if (store.loadPending() != null) {
            status.text = getString(R.string.circle_busy)
            status.visibility = View.VISIBLE
            return
        }
        PeriApp.hub(this).requestCircleChange(next, getString(if (checked) R.string.circle_add else R.string.circle_remove, name))
        status.removeCallbacks(recheck)
        status.postDelayed(recheck, 500)
    }

    private var contactsMissing = false

    private fun canReadContacts() = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    /** Android's own question, or its app settings if a "no" was already given for good. */
    private fun askContacts() {
        val asked = getSharedPreferences(WelcomeActivity.PREFS, MODE_PRIVATE)
        if (asked.getBoolean(Need.CONTACTS.name, false) && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            return
        }
        asked.edit().putBoolean(Need.CONTACTS.name, true).apply()
        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACTS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONTACTS && canReadContacts()) recreate()
    }

    override fun onResume() {
        super.onResume()
        // Back from Android's settings with contacts allowed: show the people now.
        if (contactsMissing && canReadContacts()) return recreate()
        if (::status.isInitialized) {
            status.removeCallbacks(recheck)
            recheck.run()
        }
        // News may have arrived while this screen was in the background.
        showNews()
    }

    override fun onPause() {
        if (::status.isInitialized) status.removeCallbacks(recheck)
        super.onPause()
    }

    /**
     * While a change of guardians waits, their answer can arrive with this screen open (Peri hears it and says it):
     * the screen looks again every few seconds until nothing is pending.
     */
    private val recheck = object : Runnable {
        override fun run() {
            showPending()
            if (store.loadPending()?.approved == false) status.postDelayed(this, RECHECK_MS)
        }
    }

    private fun row(id: String, name: String, guessed: Relation?): View {
        val box = card()
        // Who: their initial in a lilac circle, as on the family's page, and their name.
        box.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@FamilyActivity).apply {
                text = name.trim().take(1).uppercase()
                sizeSp(20f)
                typeface = Fonts.display(700)
                setTextColor(Palette.LILAC_INK)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Palette.LILAC_TINT)
                }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(44), dp(44)))
            addView(heading(name, TextSize.BODY).apply { setPadding(dp(12), 0, 0, 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
        // A guardian or not: a change waits for the guardians' yes, so the switch goes back meanwhile.
        lateinit var guardian: Toggle
        guardian = Toggle(this, getString(R.string.family_guardian), id in config.guardians) { checked ->
            onGuardianTicked({ guardian.set(!checked) }, id, name, checked)
        }
        box.addView(guardian, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) })

        // Who they are to the person: a field that opens the list.
        val relation = TextView(this).apply {
            text = relationText(config.relations[id] ?: guessed)
            sizeSp(TextSize.BODY)
            typeface = Fonts.display(600)
            setTextColor(Palette.INK)
        }
        box.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            background = pressable(rounded(Palette.WHITE, dp(16).toFloat(), Palette.LINE_STRONG, dp(1.5f)), Palette.RIPPLE_DARK)
            setPadding(dp(14), dp(6), dp(10), dp(6))
            isClickable = true
            isFocusable = true
            addView(relation, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(this@FamilyActivity).apply {
                setImageResource(R.drawable.ic_chevron)
                setColorFilter(Palette.INK_SOFT)
                rotation = 90f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(24), dp(24)))
            setOnClickListener { pickRelation(id, name, guessed, relation) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        box.addView(EditText(this).apply {
            hint = getString(R.string.family_aliases_hint)
            sizeSp(TextSize.BODY)
            setTextColor(Palette.INK)
            background = rounded(Palette.WHITE, dp(16).toFloat(), Palette.LINE_STRONG, dp(1.5f))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setText(config.aliases[id].orEmpty().joinToString(", "))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) = update(config.withAliases(id, s?.toString().orEmpty()))
            })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        return box
    }

    private fun relationText(relation: Relation?): String =
        RELATIONS.firstOrNull { it.first == relation }?.let { getString(R.string.family_relation_is, getString(it.second)) } ?: getString(R.string.relation_none)

    /** Who [name] is to the person, from the list ("none" too); the field shows the choice at once. */
    private fun pickRelation(id: String, name: String, guessed: Relation?, shown: TextView) {
        val current = config.relations[id] ?: guessed
        val labels = (listOf(getString(R.string.relation_none_item)) + RELATIONS.map { getString(it.second) }).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.family_relation_title, name))
            .setSingleChoiceItems(labels, RELATIONS.indexOfFirst { it.first == current } + 1) { dialog, which ->
                val chosen = RELATIONS.getOrNull(which - 1)?.first
                if (chosen != current) update(config.withRelation(id, chosen))
                shown.text = relationText(chosen)
                dialog.dismiss()
            }
            .show()
            .friendly()
    }

    private fun update(next: FamilyConfig) {
        config = next
        store.save(next)
        PeriApp.hub(this).refresh()
    }

    /** Already set up, or saved as "Laura hija" / "Mi nieto Pablo": very likely family. */
    private fun likelyFamily(c: Contact): Boolean =
        c.id in config.guardians || config.relations[c.id] != null || !config.aliases[c.id].isNullOrEmpty() || c.relation != null

    private fun fold(s: String): String =
        java.text.Normalizer.normalize(s.lowercase().trim(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun spaced() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(14)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = cardBackground()
        setPadding(dp(18), dp(16), dp(18), dp(18))
    }

    /** One of Peri's illustrations beside a title and, optionally, a line under it. */
    /** Peri beside the title; the sentence under both, at the full width, so it isn't squeezed into a column. */
    private fun illustrated(image: Int, title: TextView, line: TextView?, size: Int = 104) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(LinearLayout(this@FamilyActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@FamilyActivity).apply {
                setImageResource(image)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alive()
            }, LinearLayout.LayoutParams(dp(size), dp(size)))
            addView(title.apply { setPadding(dp(12), 0, 0, 0) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
        if (line != null) addView(line.apply { setPadding(0, dp(8), 0, 0) })
    }

    private companion object {
        const val MAX_RESULTS = 20
        const val RECHECK_MS = 3_000L
        /** What the guardians saw lately: enough to see what Peri shares, never a long scroll. */
        const val MAX_NEWS = 8
        val CHANNELS = mapOf(
            "SMS" to R.string.news_channel_sms,
            "WHATSAPP" to R.string.news_channel_whatsapp,
            "EMAIL" to R.string.news_channel_email,
            "BROWSER_NOTIFICATION" to R.string.news_channel_web,
        )
        const val REQUEST_CONTACTS = 7

        val VERDICTS = mapOf("scam" to R.string.verdict_scam, "doubt" to R.string.verdict_doubt, "ok" to R.string.verdict_ok)

        val RELATIONS = listOf(
            Relation.CHILD to R.string.relation_child,
            Relation.GRANDCHILD to R.string.relation_grandchild,
            Relation.NEPHEW_NIECE to R.string.relation_nephew_niece,
            Relation.SIBLING to R.string.relation_sibling,
            Relation.PARTNER to R.string.relation_partner,
            Relation.PARENT to R.string.relation_parent,
            Relation.FRIEND to R.string.relation_friend,
            Relation.CAREGIVER to R.string.relation_caregiver,
            Relation.OTHER to R.string.relation_other,
        )
    }
}
