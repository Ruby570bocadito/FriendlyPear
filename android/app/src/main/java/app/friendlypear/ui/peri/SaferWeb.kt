package app.friendlypear.ui.peri

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.provider.Settings
import android.widget.Toast
import app.friendlypear.R

/** Quad9: a non-profit resolver that refuses known scam and malware sites, and keeps no personal data. */
internal const val SAFE_DNS = "dns.quad9.net"

/** The Private DNS in use, if any: with one that refuses scam sites, they're blocked in every app. */
internal fun Context.privateDns(): String? {
    val connectivity = getSystemService(ConnectivityManager::class.java) ?: return null
    return runCatching { connectivity.getLinkProperties(connectivity.activeNetwork)?.privateDnsServerName }.getOrNull()
}

/**
 * Android has no switch an app can flip for this: Peri explains the three steps, copies the name to paste and opens
 * the network settings, where "Private DNS" lives. From Settings and from the security checkup.
 */
internal fun Activity.saferWeb() {
    // Copied at once, as the steps say, so it's ready to paste whichever button is tapped.
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("DNS", SAFE_DNS))
    android.app.AlertDialog.Builder(this)
        .setTitle(R.string.web_dns_title)
        .setMessage(getString(R.string.web_dns_steps, SAFE_DNS))
        .setNeutralButton(R.string.web_dns_copy) { _, _ ->
            getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("DNS", SAFE_DNS))
            Toast.makeText(this, getString(R.string.web_dns_copied, SAFE_DNS), Toast.LENGTH_LONG).show()
        }
        .setPositiveButton(R.string.web_dns_open) { _, _ ->
            runCatching { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
                .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
        }
        .setNegativeButton(R.string.pair_not_now, null)
        .show()
        .friendly()
}
