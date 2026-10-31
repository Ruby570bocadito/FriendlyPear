package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import app.friendlypear.R
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Rive
import java.util.WeakHashMap

/**
 * The app's Rive animations (res/raw/rive_*.riv, made by assets/rive): vector pieces that change with what is
 * happening, each a state machine with one number input. Decoration only: the words next to them say the same, so
 * screen readers skip them. With "remove animations" on there is none, and the screen keeps its still picture; so it
 * does where Rive's library can't load.
 */
internal enum class RiveArt(val raw: Int, val machine: String, val input: String) {
    /** 0 watching, 1 danger, 2 safe. */
    SHIELD(R.raw.rive_shield, "Shield", "level"),
    /** How many of the quiz's six stars light up, 0 to 6. */
    STARS(R.raw.rive_stars, "Stars", "score"),
    /** 0 nothing, 1 looking, 2 fine, 3 in doubt, 4 a trap. */
    SCAN(R.raw.rive_scan, "Scan", "state"),
    /** The checkup's ring, 0 to 100 in tens. */
    RING(R.raw.rive_ring, "Ring", "percent"),
    /** The "Protecting you" dot: 1 radar rings, 0 a slow amber blink. */
    PULSE(R.raw.rive_pulse, "Pulse", "on"),
    /** A celebration behind Peri: rays turning, confetti once. No input: it plays as it appears. */
    BURST(R.raw.rive_burst, "Burst", ""),
    /** A pencil writing line after line, while the report is written. No input. */
    WRITE(R.raw.rive_write, "Write", ""),
    /** The tab bar's icons, drawn from ic_pear, ic_diary, ic_learn and ic_family: 1 picked (its moment), 0 not. */
    TAB_PERI(R.raw.rive_tab_peri, "Tab", "on"),
    TAB_DIARY(R.raw.rive_tab_diary, "Tab", "on"),
    TAB_LEARN(R.raw.rive_tab_learn, "Tab", "on"),
    TAB_FAMILY(R.raw.rive_tab_family, "Tab", "on"),
    /** "Talk to Peri"'s microphone, white: 1 sound bars bouncing while she listens, 0 the microphone. */
    VOICE(R.raw.rive_voice, "Voice", "on"),
    /** The shields chip's shield, glinting now and then. No input. */
    GLINT(R.raw.rive_glint, "Glint", ""),
    /** Pairing: 0 waiting, 1 connecting, 2 connected, 3 it failed. */
    LINK(R.raw.rive_link, "Link", "state"),
    /** The chat's send arrow, white: it flies off and back each time "sent" goes from 1 to 2 or back; 0 at rest. */
    SEND(R.raw.rive_send, "Send", "sent"),
    /** The welcome's "Next" arrow, white, nudging forward now and then. No input. */
    NEXT(R.raw.rive_next, "Next", ""),
}

/** [art] showing [value] (none for those without an input), or null with animations off or without Rive's library. */
internal fun Context.rive(art: RiveArt, value: Float = 0f): RiveAnimationView? {
    if (!animationsOn() || !RiveLife.ready(this)) return null
    return Decoration(this).apply {
        setRiveResource(art.raw, stateMachineName = art.machine, fit = Fit.CONTAIN)
        if (art.input.isNotEmpty()) setNumberState(art.machine, art.input, value)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}

/**
 * Rive's view keeps every touch for the animation's own listeners; ours have none, and sit on buttons, tabs and rows:
 * their touches go to those, as with an icon. It also needs a lifecycle to give its memory back (see [RiveLife]).
 */
private class Decoration(context: Context) : RiveAnimationView(context) {
    /** Its own lifecycle in a window over other apps, where there is no screen's: it ends when the window goes. */
    private var own: Life? = null

    override fun onAttachedToWindow() {
        if (findViewTreeLifecycleOwner() == null) setViewTreeLifecycleOwner(RiveLife.of(context) ?: Life().also { own = it })
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        own?.end()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = false
}

/**
 * Rive's native side, loaded the first time a screen shows an animation (never when a text or a call wakes Peri up in
 * the background, where nothing is drawn), and the lifecycles its views need. Rive frees an animation's native memory
 * (its file, artboard and state machine) only when the lifecycle it finds in the view tree ends, and the app's plain
 * Activities and its windows over other apps have none: without one, each animation kept about 140 KB after its
 * screen closed (measured with the debug gallery: 13 MB after opening it six times). So each screen gets a lifecycle
 * that ends with it, and each piece in a window over other apps its own, ended when the window is taken away (the
 * pause builds a new window every time).
 */
private object RiveLife : Application.ActivityLifecycleCallbacks {
    /** Null until tried; false where the library didn't load (a phone whose architecture the APK leaves out). */
    private var loaded: Boolean? = null
    private val screens = WeakHashMap<Activity, Life>()

    fun ready(context: Context): Boolean = loaded ?: runCatching {
        val app = context.applicationContext as Application
        Rive.init(app)
        app.registerActivityLifecycleCallbacks(this)
    }.isSuccess.also { loaded = it }

    /** The lifecycle of the screen [context] belongs to, or null in a window over other apps. */
    fun of(context: Context): Life? = context.activity()?.let { screens.getOrPut(it) { Life() } }

    override fun onActivityPreDestroyed(activity: Activity) {
        screens.remove(activity)?.end()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

/** A lifecycle that only lives, then ends: Rive does nothing on the steps between. */
private class Life : LifecycleOwner {
    private val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
    override val lifecycle: Lifecycle get() = registry

    fun end() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}

private fun Context.activity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Moves [art] on to [value]: it blends from where it is. */
internal fun RiveAnimationView.show(art: RiveArt, value: Float) = setNumberState(art.machine, art.input, value)
