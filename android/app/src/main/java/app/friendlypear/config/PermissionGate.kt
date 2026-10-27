package app.friendlypear.config

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper

/**
 * Asks for one permission at a time, when it is first needed: Peri says what for, then Android's own dialog
 * appears. The person decides; a "no" is respected and Peri says what still works.
 */
class PermissionGate(context: Context, private val speak: (String) -> Unit) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /** Set by the visible screen, which shows Android's permission dialog and reports the answer. */
    @Volatile
    var presenter: ((Need, (Boolean) -> Unit) -> Unit)? = null

    fun granted(need: Need): Boolean = need.permissions.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /** Without a screen to ask on, the answer is "no": Peri never grants anything by itself. */
    fun ask(need: Need, onResult: (Boolean) -> Unit) {
        main.post {
            if (granted(need)) return@post onResult(true)
            val show = presenter ?: return@post onResult(false)
            speak(context.getString(need.why))
            show(need) { allowed ->
                if (!allowed && need.denied != 0) speak(context.getString(need.denied))
                onResult(allowed)
            }
        }
    }
}
