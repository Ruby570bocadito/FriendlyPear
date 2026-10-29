package app.friendlypear.ui.peri

import android.app.Activity
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import app.friendlypear.BuildConfig
import app.friendlypear.PeriApp
import app.friendlypear.R

/**
 * The screens where something is decided, guarded against what a scammer does on a call: have an app that shares the
 * screen installed, then read what is on it, or press "yes" for the person. [secure] keeps a screen out of captures and
 * screen sharing; [button] makes a decisive button ignore a touch that comes through another app's window drawn over it
 * (a fake picture over a "yes"), and [check], fed with the screen's touches, has Peri say why it did nothing.
 */
internal class Guard(private val activity: Activity) {
    private val buttons = mutableListOf<View>()
    private var warnedAt = 0L

    fun <T : View> button(view: T): T = view.apply {
        filterTouchesWhenObscured = true
        buttons += this
    }

    fun check(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_DOWN || event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED == 0) return
        val at = Rect()
        if (buttons.none { it.isShown && it.getGlobalVisibleRect(at) && at.contains(event.rawX.toInt(), event.rawY.toInt()) }) return
        val now = SystemClock.uptimeMillis()
        if (now - warnedAt < WARN_EVERY_MS) return
        warnedAt = now
        val line = activity.getString(R.string.guard_covered)
        Toast.makeText(activity, line, Toast.LENGTH_LONG).show()
        PeriApp.hub(activity).say(line)
    }

    companion object {
        private const val WARN_EVERY_MS = 4_000L

        /** Out of captures and screen sharing (release builds; debug builds stay capturable for tests and the video). */
        fun secure(activity: Activity) {
            if (BuildConfig.SECURE_SCREENS) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
