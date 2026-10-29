package app.friendlypear.ui.peri

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import app.friendlypear.device.Obstacles
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A security checkup of the phone, whenever the person likes: what Peri can see from here that a scammer could use,
 * each with its fix one tap away. The screen lock, how old the last security update is, USB debugging, apps that can
 * control the screen, scam websites blocked for the whole phone, and Peri's own eyes on alerts and calls. And what
 * Android puts in her way on a real phone: battery saving that puts her to sleep, no permission to show the pause
 * over other apps, and from Android 15 the codes it hides from her unless texts are allowed. Nothing leaves the phone. Peri says how many are in order; coming back from a fix, the checkup looks again.
 */
class CheckupActivity : Activity() {
    /** [art] is Peri showing what the check is about: a padlock, an update arrow, a cable, a bell... */
    private data class Check(val art: Int, val ok: Boolean, val title: String, val text: String, val fix: Int, val act: () -> Unit)

    private val hub by lazy { PeriApp.hub(this) }
    private lateinit var list: LinearLayout
    private var said = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            addView(list)
        }
        setContentView(FrameLayout(this).apply {
            background = ground()
            addView(scroll)
            setOnApplyWindowInsetsListener { _, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                scroll.setPadding(dp(20) + bars.left, dp(16) + bars.top, dp(20) + bars.right, dp(20) + bars.bottom)
                insets
            }
        })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val checks = checks()
        val ok = checks.count { it.ok }
        list.removeAllViews()
        list.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(ImageView(this@CheckupActivity).apply {
                setImageResource(if (ok == checks.size) R.drawable.peri_scene_cheer else R.drawable.peri_scene_checkup)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alive()
            }, LinearLayout.LayoutParams(dp(130), dp(130)))
            addView(heading(getString(R.string.checkup_title)).apply { gravity = Gravity.CENTER })
            addView(TextView(this@CheckupActivity).apply {
                text = getString(R.string.checkup_score, ok, checks.size)
                sizeSp(TextSize.SMALL)
                typeface = weight(800)
                setTextColor(if (ok == checks.size) Palette.GREEN_DARK else Palette.CORAL_INK)
                background = rounded(if (ok == checks.size) Palette.GREEN_TINT else Palette.CORAL_TINT, dp(16).toFloat())
                setPadding(dp(14), dp(6), dp(14), dp(6))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        })
        // What needs doing first, then what's already in order.
        checks.sortedBy { it.ok }.forEach { list.addView(card(it)) }
        list.addView(Pill(this, getString(R.string.trap_done), R.drawable.ic_shield, primary = true).apply {
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(64)).apply { topMargin = dp(16) })
        // Said once, when it opens: back from a fix, the screen is enough.
        if (!said) {
            said = true
            hub.say(if (ok == checks.size) getString(R.string.checkup_all) else getString(R.string.checkup_some, ok, checks.size))
        }
    }

    private fun card(check: Check): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = cardBackground()
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) }
        addView(LinearLayout(this@CheckupActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(this@CheckupActivity).apply {
                setImageResource(check.art)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(56), dp(56)))
            addView(heading(check.title, TextSize.BODY).apply { setPadding(dp(10), 0, dp(8), 0) },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            // In order or not, at a glance: a tick or a warning, at the end.
            addView(ImageView(this@CheckupActivity).apply {
                setImageResource(if (check.ok) R.drawable.ic_check else R.drawable.ic_warning)
                setColorFilter(if (check.ok) Palette.GREEN_DARK else Palette.CORAL_INK)
                background = rounded(if (check.ok) Palette.GREEN_TINT else Palette.CORAL_TINT, dp(18).toFloat())
                setPadding(dp(8), dp(8), dp(8), dp(8))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(36), dp(36)))
        })
        if (check.ok) return@apply
        addView(body(check.text, TextSize.SMALL).apply {
            setTextColor(Palette.INK)
            setPadding(0, dp(8), 0, 0)
        })
        // The fix as a real button, big enough to find and tap, with the arrow of something that opens.
        addView(Pill(this@CheckupActivity, getString(check.fix), R.drawable.ic_chevron, primary = false, size = 18f, trailing = true, rubber = Rubber.GREEN).apply {
            setOnClickListener { runCatching { check.act() }.onFailure { hub.show(getString(R.string.after_no_app)) } }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(54)).apply { topMargin = dp(12) })
    }

    private fun open(action: String, fallback: String = Settings.ACTION_SETTINGS) {
        runCatching { startActivity(Intent(action)) }.onFailure { startActivity(Intent(fallback)) }
    }

    private fun checks(): List<Check> {
        val locked = getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
        // How old the last security patch is, in months ("2026-09-05").
        val months = runCatching { ChronoUnit.MONTHS.between(LocalDate.parse(Build.VERSION.SECURITY_PATCH), LocalDate.now()).toInt() }.getOrDefault(0)
        val adb = Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        val intruder = app.friendlypear.device.ScreenAccess.intruders(this).firstOrNull()?.second
        val dns = privateDns() != null
        val alerts = UnlockActivity.hasNotificationAccess(this)
        val roles = getSystemService(RoleManager::class.java)
        val calls = roles?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        // Some phones don't offer the call filter at all: nothing to fix there.
        val filters = roles?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        val free = Obstacles.runsFreely(this)
        val over = Settings.canDrawOverlays(this)
        val codes = !Obstacles.codesHidden(this)
        return listOfNotNull(
            Check(R.drawable.peri_check_lock, locked, getString(if (locked) R.string.checkup_lock_ok else R.string.checkup_lock_bad), getString(R.string.checkup_lock_text), R.string.checkup_lock_fix) {
                open(DevicePolicyManager.ACTION_SET_NEW_PASSWORD, Settings.ACTION_SECURITY_SETTINGS)
            },
            Check(R.drawable.peri_check_update, months <= OLD_PATCH_MONTHS, getString(if (months <= OLD_PATCH_MONTHS) R.string.checkup_update_ok else R.string.checkup_update_bad),
                getString(R.string.checkup_update_text), R.string.checkup_update_fix) {
                open("android.settings.SYSTEM_UPDATE_SETTINGS", Settings.ACTION_DEVICE_INFO_SETTINGS)
            },
            Check(R.drawable.peri_check_adb, !adb, getString(if (adb) R.string.checkup_adb_bad else R.string.checkup_adb_ok), getString(R.string.checkup_adb_text), R.string.checkup_adb_fix) {
                open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            },
            Check(R.drawable.peri_check_screen, intruder == null, if (intruder == null) getString(R.string.checkup_screen_ok) else getString(R.string.checkup_screen_bad, intruder),
                getString(R.string.checkup_screen_text), R.string.checkup_screen_fix) {
                open(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            },
            Check(R.drawable.peri_check_dns, dns, getString(if (dns) R.string.checkup_dns_ok else R.string.checkup_dns_bad), getString(R.string.checkup_dns_text), R.string.checkup_dns_fix) {
                saferWeb()
            },
            Check(R.drawable.peri_check_alerts, alerts, getString(if (alerts) R.string.checkup_alerts_ok else R.string.checkup_alerts_bad), getString(R.string.checkup_alerts_text), R.string.checkup_allow) {
                if (UnlockActivity.restricted(this)) startActivity(Intent(this, UnlockActivity::class.java)) else open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            },
            if (filters) Check(R.drawable.peri_check_calls, calls, getString(if (calls) R.string.checkup_calls_ok else R.string.checkup_calls_bad), getString(R.string.checkup_calls_text), R.string.checkup_allow) {
                roles?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)?.let { startActivity(it) }
            } else null,
            Check(R.drawable.peri_ui_perm_battery, free, getString(if (free) R.string.checkup_battery_ok else R.string.checkup_battery_bad), getString(R.string.checkup_battery_text), R.string.checkup_allow) {
                runCatching { startActivity(Obstacles.freeIntent(this)) }.onFailure { startActivity(Obstacles.appInfo(this)) }
            },
            Check(R.drawable.peri_ui_perm_overlay, over, getString(if (over) R.string.checkup_overlay_ok else R.string.checkup_overlay_bad), getString(R.string.checkup_overlay_text), R.string.checkup_allow) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            },
            // Only where Android hides them (15 and later).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) Check(R.drawable.peri_ui_perm_sms, codes, getString(if (codes) R.string.checkup_codes_ok else R.string.checkup_codes_bad),
                getString(R.string.checkup_codes_text), R.string.checkup_codes_fix) { allowTexts() } else null,
        )
    }

    /** Texts, so Peri sees a code arrive: the unlock guide if Android locked them, or Android's own question. */
    private fun allowTexts() {
        if (UnlockActivity.restricted(this)) return startActivity(Intent(this, UnlockActivity::class.java))
        requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), REQUEST_TEXTS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_TEXTS) return
        // Android won't ask again after two "no"s: the app's page, where texts can be allowed by hand.
        val allowed = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (!allowed && !shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS)) startActivity(Obstacles.appInfo(this))
    }

    private companion object {
        /** A phone without a security update for longer than this has known doors open. */
        const val OLD_PATCH_MONTHS = 6
        const val REQUEST_TEXTS = 41
    }
}
