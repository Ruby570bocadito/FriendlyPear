package app.friendlypear.ui.peri

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
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
    private lateinit var cancel: Button
    private var names: Map<String, String> = emptyMap()
    private var syncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FamilyStore(this)
        config = store.load()
        val pad = (20 * resources.displayMetrics.density).toInt()
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        list.addView(text(getString(R.string.family_title), 26f))
        list.addView(text(getString(R.string.family_intro), 18f))
        status = text("", 18f)
        cancel = Button(this).apply {
            text = getString(R.string.circle_cancel)
            setOnClickListener {
                store.savePending(null)
                showPending()
            }
        }
        list.addView(status)
        list.addView(cancel)

        list.addView(CheckBox(this).apply {
            text = getString(R.string.family_word_agreed)
            textSize = 20f
            isChecked = config.hasFamilyWord
            setOnCheckedChangeListener { _, checked -> update(config.copy(hasFamilyWord = checked)) }
        })
        list.addView(text(getString(R.string.family_word_note), 16f))

        val contacts = PhoneContacts.load(this)
        names = contacts.associate { it.id to it.name }
        if (contacts.isEmpty()) list.addView(text(getString(R.string.family_no_contacts), 18f))
        contacts.forEach { c -> list.addView(row(c.id, c.name, c.relation)) }

        // What the investigator looked into, so the family can see why Peri warned. Never the message itself.
        val looked = InvestigationLog(this).load()
        if (looked.isNotEmpty()) list.addView(text(getString(R.string.investigations_title), 22f))
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        looked.forEach { e ->
            val verdict = getString(VERDICTS[e.verdict] ?: R.string.verdict_doubt)
            list.addView(text("${format.format(Date(e.at))} · $verdict\n" + e.evidence.joinToString("\n") { "• $it" }, 16f))
        }

        setContentView(ScrollView(this).apply { addView(list) })
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
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (18 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        box.addView(text(name, 22f))
        box.addView(CheckBox(this).apply {
            text = getString(R.string.family_guardian)
            textSize = 18f
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

    private fun text(value: String, size: Float) = TextView(this).apply {
        text = value
        textSize = size
    }

    private companion object {
        val VERDICTS = mapOf("scam" to R.string.verdict_scam, "doubt" to R.string.verdict_doubt, "ok" to R.string.verdict_ok)

        val RELATIONS = listOf(
            Relation.CHILD to R.string.relation_child,
            Relation.GRANDCHILD to R.string.relation_grandchild,
            Relation.NEPHEW_NIECE to R.string.relation_nephew_niece,
            Relation.SIBLING to R.string.relation_sibling,
            Relation.PARTNER to R.string.relation_partner,
            Relation.FRIEND to R.string.relation_friend,
            Relation.CAREGIVER to R.string.relation_caregiver,
            Relation.OTHER to R.string.relation_other,
        )
    }
}
