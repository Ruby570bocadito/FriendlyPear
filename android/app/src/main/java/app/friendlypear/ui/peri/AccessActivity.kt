package app.friendlypear.ui.peri

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
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
import app.friendlypear.config.UseLog

/**
 * "What Peri can do": every permission and special access Peri has on this phone, what it's for and what she
 * never does with it, whether it's on, and the way to switch it on or off (Android's own screen, as only the
 * person can change these). Below, what Peri did with them: each call, text, location share, picture and voice
 * order, from [UseLog]. Opened from Settings and from the "Protecting you" chip.
 */
class AccessActivity : Activity() {
    private class Item(
        val icon: Int,
        val title: Int,
        val note: Int,
        val isOn: () -> Boolean,
        val turnOn: () -> Unit,
        val turnOff: () -> Unit,
    )

    private val hub by lazy { PeriApp.hub(this) }
    private val asked by lazy { getSharedPreferences(WelcomeActivity.PREFS, MODE_PRIVATE) }
    private lateinit var items: List<Item>
    private lateinit var rows: LinearLayout
    private lateinit var summary: TextView
    private lateinit var log: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = buildItems()
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        list.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@AccessActivity).apply {
                setImageResource(R.drawable.peri_shield)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(96), dp(96)))
            addView(heading(getString(R.string.access_title)).apply { setPadding(dp(12), 0, 0, 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
        list.addView(body(getString(R.string.access_intro)).apply { setPadding(0, dp(8), 0, 0) })
        summary = body("", TextSize.SMALL).apply {
            typeface = weight(600)
            setTextColor(Palette.GREEN_DARK)
            setPadding(0, dp(10), 0, 0)
        }
        list.addView(summary)
        // One card for all of them, each row split by a thin rule.
        rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        list.addView(rows, full(top = 12))

        list.addView(heading(getString(R.string.access_log_title), TextSize.BODY), full(top = 24))
        list.addView(body(getString(R.string.access_log_note), TextSize.SMALL).apply { setPadding(0, dp(2), 0, 0) })
        log = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(18), dp(6), dp(18), dp(6))
        }
        list.addView(log, full(top = 10))

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
        list.floatIn()
    }

    /** Back from Android's settings or a permission dialog: the states are read again. */
    override fun onResume() {
        super.onResume()
        showItems()
        showLog()
    }

    private fun full(top: Int) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }

    private fun buildItems(): List<Item> {
        val roles = getSystemService(RoleManager::class.java)
        val screening = roles?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        return listOfNotNull(
            runtime(Need.CONTACTS, R.drawable.ic_family, R.string.access_contacts, R.string.access_contacts_note),
            runtime(Need.READ_SMS, R.drawable.ic_message, R.string.access_texts, R.string.access_texts_note),
            runtime(Need.PHONE_STATE, R.drawable.ic_call, R.string.access_calls, R.string.access_calls_note),
            Item(
                R.drawable.ic_notifications, R.string.access_alerts, R.string.access_alerts_note,
                isOn = { UnlockActivity.hasNotificationAccess(this) },
                turnOn = {
                    if (UnlockActivity.restricted(this)) startActivity(Intent(this, UnlockActivity::class.java))
                    else open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                turnOff = { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            ),
            if (!screening) null else Item(
                R.drawable.ic_shield, R.string.access_screening, R.string.access_screening_note,
                isOn = { roles?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true },
                turnOn = {
                    @Suppress("DEPRECATION")
                    roles?.let { startActivityForResult(it.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQUEST_ROLE) }
                },
                turnOff = { open(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) },
            ),
            Item(
                R.drawable.ic_bank, R.string.access_usage, R.string.access_usage_note,
                isOn = { hub.bank.allowed() },
                turnOn = { open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                turnOff = { open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            ),
            runtime(Need.MIC, R.drawable.ic_mic, R.string.access_mic, R.string.access_mic_note),
            runtime(Need.CALL, R.drawable.ic_call, R.string.access_call, R.string.access_call_note),
            runtime(Need.SEND_SMS, R.drawable.ic_message, R.string.access_sms, R.string.access_sms_note),
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) null
            else runtime(Need.NOTIFY, R.drawable.ic_notifications, R.string.access_notify, R.string.access_notify_note),
            runtime(Need.LOCATION, R.drawable.ic_location, R.string.access_location, R.string.access_location_note),
            Item(
                R.drawable.ic_mini, R.string.access_overlay, R.string.access_overlay_note,
                isOn = { Settings.canDrawOverlays(this) },
                turnOn = { open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
                turnOff = { open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) },
            ),
        )
    }

    /**
     * A permission Android asks about in its own dialog. Switching one off happens in the app's permissions
     * screen: apps can't take a permission back by themselves while they run.
     */
    private fun runtime(need: Need, icon: Int, title: Int, note: Int) = Item(
        icon, title, note,
        isOn = { hub.permissions.granted(need) },
        turnOn = {
            val canAsk = need.permissions.any { shouldShowRequestPermissionRationale(it) } || !asked.getBoolean(need.name, false)
            if (canAsk) {
                asked.edit().putBoolean(need.name, true).apply()
                requestPermissions(need.permissions.toTypedArray(), REQUEST_NEED)
            } else {
                appSettings()
            }
        },
        turnOff = { appSettings() },
    )

    private fun appSettings() {
        hub.show(getString(R.string.access_how_off))
        open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
    }

    private fun showItems() {
        rows.removeAllViews()
        var on = 0
        items.forEachIndexed { i, item ->
            val enabled = item.isOn()
            if (enabled) on++
            if (i > 0) rows.addView(View(this).apply { setBackgroundColor(RULE) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                marginStart = dp(14)
                marginEnd = dp(14)
            })
            rows.addView(row(item, enabled))
        }
        summary.text = getString(R.string.access_summary, on, items.size)
    }

    /** One row: an icon, what it is and what for, whether it's on, and what tapping it does. */
    private fun row(item: Item, enabled: Boolean): View = LinearLayout(this).apply {
        setPadding(dp(14), dp(14), dp(14), dp(14))
        minimumHeight = dp(72)
        isClickable = true
        isFocusable = true
        background = pressable(rounded(0, dp(20).toFloat()), Palette.RIPPLE_DARK)
        val title = getString(item.title)
        val state = getString(if (enabled) R.string.access_on else R.string.access_off)
        val action = getString(if (enabled) R.string.access_turn_off else R.string.access_turn_on)
        contentDescription = "$title. $state. ${getString(item.note)}. $action"
        setOnClickListener { if (enabled) item.turnOff() else item.turnOn() }
        addView(ImageView(this@AccessActivity).apply {
            setImageResource(item.icon)
            setColorFilter(if (enabled) Palette.GREEN_DARK else Palette.INK_SOFT)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(24), dp(24)).apply { topMargin = dp(2) })
        addView(LinearLayout(this@AccessActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(LinearLayout(this@AccessActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(body(title).apply {
                    typeface = weight(600)
                    setTextColor(Palette.INK)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(TextView(this@AccessActivity).apply {
                    text = state
                    sizeSp(TextSize.SMALL)
                    typeface = weight(600)
                    setTextColor(if (enabled) Palette.GREEN_DARK else Palette.AMBER_INK)
                    background = rounded(if (enabled) Palette.GREEN_TINT else Palette.AMBER_TINT, dp(14).toFloat())
                    setPadding(dp(10), dp(2), dp(10), dp(3))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(8) })
            })
            addView(body(getString(item.note), TextSize.SMALL).apply { setPadding(0, dp(2), 0, 0) })
            addView(body(action, TextSize.SMALL).apply {
                typeface = weight(700)
                setTextColor(Palette.GREEN_DARK)
                setPadding(0, dp(6), 0, 0)
            })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun showLog() {
        log.removeAllViews()
        val entries = hub.uses.all()
        if (entries.isEmpty()) {
            log.addView(body(getString(R.string.access_log_empty), TextSize.SMALL).apply { setPadding(0, dp(10), 0, dp(10)) })
            return
        }
        entries.take(SHOWN).forEachIndexed { i, e ->
            if (i > 0) log.addView(View(this).apply { setBackgroundColor(RULE) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
            log.addView(LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, dp(10), 0, dp(10))
                addView(body(describe(e)).apply { setTextColor(Palette.INK) })
                addView(body(DateUtils.formatDateTime(this@AccessActivity, e.at, DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH), TextSize.SMALL))
            })
        }
    }

    private fun describe(e: UseLog.Entry): String {
        val who = e.who ?: getString(R.string.use_someone)
        return when (e.kind) {
            UseLog.Kind.CALL -> getString(R.string.use_call, who)
            UseLog.Kind.TEXT -> getString(R.string.use_text, who)
            UseLog.Kind.LOCATION -> getString(R.string.use_location)
            UseLog.Kind.PICTURE -> getString(R.string.use_picture)
            UseLog.Kind.VOICE -> getString(R.string.use_voice)
            UseLog.Kind.REPORT -> getString(R.string.use_report)
            UseLog.Kind.HANG_UP -> getString(R.string.use_hang_up)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_NEED) return
        if (grantResults.none { it == PackageManager.PERMISSION_GRANTED }) hub.show(getString(R.string.access_still_works))
        hub.refresh()
        showItems()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ROLE) showItems()
    }

    private companion object {
        const val REQUEST_NEED = 11
        const val REQUEST_ROLE = 12
        const val SHOWN = 12
        /** The thin line between rows: a little darker than the white card. */
        val RULE = 0xFFF3ECDD.toInt()
    }
}
