package app.friendlypear.ui.peri

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.config.FamilyStore
import app.friendlypear.config.InvestigationLog
import app.friendlypear.config.PhoneContacts
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
    private var syncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FamilyStore(this)
        config = store.load()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        list.addView(illustrated(R.drawable.peri_family, heading(getString(R.string.family_title), 28f), body(getString(R.string.family_intro))))
        status = body("", 18f).apply {
            setTextColor(Palette.AMBER_INK)
            background = rounded(Palette.AMBER_TINT, dp(18).toFloat())
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        cancel = Pill(this, getString(R.string.circle_cancel), R.drawable.ic_chevron, primary = false, size = 18f).apply {
            setOnClickListener {
                store.savePending(null)
                showPending()
            }
        }
        list.addView(status, spaced())
        list.addView(cancel, spaced().apply { height = dp(56) })

        list.addView(card().apply {
            addView(CheckBox(this@FamilyActivity).apply {
                text = getString(R.string.family_word_agreed)
                textSize = 20f
                typeface = weight(600)
                setTextColor(Palette.INK)
                isChecked = config.hasFamilyWord
                setOnCheckedChangeListener { _, checked -> update(config.copy(hasFamilyWord = checked)) }
            })
            addView(body(getString(R.string.family_word_note), 16f).apply { setPadding(dp(6), dp(4), 0, 0) })
        }, spaced())

        val contacts = PhoneContacts.load(this)
        names = contacts.associate { it.id to it.name }
        if (contacts.isEmpty()) list.addView(body(getString(R.string.family_no_contacts)), spaced())
        contacts.forEach { c -> list.addView(row(c.id, c.name, c.relation), spaced()) }

        // What the investigator looked into, so the family can see why Peri warned. Never the message itself.
        val looked = InvestigationLog(this).load()
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val report = card().apply {
            addView(illustrated(R.drawable.peri_report, heading(getString(R.string.investigations_title), 21f), null, size = 72))
            if (looked.isEmpty()) addView(body(getString(R.string.investigations_empty), 16f).apply { setPadding(0, dp(8), 0, 0) })
            looked.forEach { e ->
                val verdict = getString(VERDICTS[e.verdict] ?: R.string.verdict_doubt)
                addView(heading("${format.format(Date(e.at))} · $verdict", 17f).apply { setPadding(0, dp(14), 0, dp(2)) })
                addView(body(e.evidence.joinToString("\n") { "•  $it" }, 16f))
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
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.CREAM, Palette.MEADOW))
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

    /** Changing who the guardians are waits for a guardian's yes and 48 hours; the box stays as it is meanwhile. */
    private fun onGuardianTicked(box: CheckBox, id: String, name: String, checked: Boolean) {
        if (syncing) return
        val next = config.withGuardian(id, checked)
        if (!CircleRules.needsGuardians(config, next)) return update(next)
        syncing = true
        box.isChecked = !checked
        syncing = false
        if (store.loadPending() != null) {
            status.text = getString(R.string.circle_busy)
            status.visibility = View.VISIBLE
            return
        }
        PeriApp.hub(this).requestCircleChange(next, getString(if (checked) R.string.circle_add else R.string.circle_remove, name))
        status.postDelayed({ showPending() }, 500)
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) showPending()
    }

    private fun row(id: String, name: String, guessed: Relation?): View {
        val box = card()
        box.addView(heading(name, 22f))
        box.addView(CheckBox(this).apply {
            text = getString(R.string.family_guardian)
            textSize = 18f
            setTextColor(Palette.INK)
            isChecked = id in config.guardians
            setOnCheckedChangeListener { box, checked -> onGuardianTicked(box as CheckBox, id, name, checked) }
        })

        val labels = listOf(getString(R.string.relation_none)) + RELATIONS.map { getString(it.second) }
        box.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@FamilyActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            val current = config.relations[id] ?: guessed
            setSelection(RELATIONS.indexOfFirst { it.first == current } + 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, rowId: Long) {
                    val chosen = RELATIONS.getOrNull(position - 1)?.first
                    if (chosen != (config.relations[id] ?: guessed)) update(config.withRelation(id, chosen))
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        })

        box.addView(EditText(this).apply {
            hint = getString(R.string.family_aliases_hint)
            textSize = 18f
            setTextColor(Palette.INK)
            background = rounded(Palette.CREAM, dp(14).toFloat(), Palette.GREEN_TINT, dp(2))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setText(config.aliases[id].orEmpty().joinToString(", "))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) = update(config.withAliases(id, s?.toString().orEmpty()))
            })
        })
        return box
    }

    private fun update(next: FamilyConfig) {
        config = next
        store.save(next)
        PeriApp.hub(this).refresh()
    }

    private fun spaced() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(14)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = cardBackground()
        elevation = dp(2).toFloat()
        setPadding(dp(18), dp(16), dp(18), dp(18))
    }

    /** One of Peri's illustrations beside a title and, optionally, a line under it. */
    private fun illustrated(image: Int, title: TextView, line: TextView?, size: Int = 104) = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(this@FamilyActivity).apply {
            setImageResource(image)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(size), dp(size)))
        addView(LinearLayout(this@FamilyActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(title)
            if (line != null) addView(line.apply { setPadding(0, dp(4), 0, 0) })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private companion object {
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
