package app.friendlypear.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Hears when an app is installed, so Peri can warn if it's a remote-control app (AnyDesk, TeamViewer…).
 * Android only shows it the packages listed in the manifest's <queries>; it never sees the rest.
 */
class AppWatcher(context: Context, private val onInstalled: (packageName: String) -> Unit) {
    private val context = context.applicationContext

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            intent.data?.schemeSpecificPart?.let(onInstalled)
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }
}
