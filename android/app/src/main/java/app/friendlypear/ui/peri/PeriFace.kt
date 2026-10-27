package app.friendlypear.ui.peri

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.ImageView
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import kotlin.random.Random

/**
 * Peri on screen. Each mood has a Muse Image illustration plus four frames made from it (assets/peri/): a blink,
 * talking, and the leaf swaying left and right. Two stacked layers cross-fade between frames, so she sways
 * slowly, blinks now and then and moves her mouth while she speaks. With "remove animations" on, she changes
 * mood without moving (docs/DISENO.md).
 */
class PeriFace(context: Context) : FrameLayout(context) {
    private data class Frames(val still: Int, val blink: Int, val talk: Int, val left: Int, val right: Int)

    private val moves = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    private val main = Handler(Looper.getMainLooper())
    private var front = layer()
    private var back = layer()
    private var shown: Mood? = null
    private var frame = 0
    private var talking = false
    private var mouthOpen = false
    private var swayStep = 0
    private var breath: ObjectAnimator? = null

    init {
        addView(back)
        addView(front)
        back.alpha = 0f
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    private fun layer() = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val frames: Frames get() = FRAMES.getValue(shown ?: Mood.CALM)

    /** Mini Peri handles touches itself (tap, drag, long press) and reports a tap here, for accessibility. */
    override fun performClick(): Boolean = super.performClick()

    fun show(mood: Mood) {
        if (mood == shown) return
        val first = shown == null
        shown = mood
        contentDescription = context.getString(DESCRIPTIONS.getValue(mood))
        swayStep = 0
        if (!moves || first) put(frames.still) else fadeTo(frames.still, MOOD_FADE)
    }

    /** Mouth moves while Peri's voice plays. */
    fun speaking(on: Boolean) {
        if (on == talking) return
        talking = on
        main.removeCallbacks(mouth)
        if (on && moves) main.post(mouth) else if (!on) {
            mouthOpen = false
            if (shown != null) put(frames.still)
        }
    }

    /** Shows a frame at once, with no fade: blinks and the mouth are quick. */
    private fun put(res: Int) {
        frame = res
        front.animate().cancel()
        back.animate().cancel()
        front.setImageResource(res)
        front.alpha = 1f
        back.alpha = 0f
    }

    private fun fadeTo(res: Int, millis: Long) {
        if (res == frame) return
        frame = res
        front.animate().cancel()
        back.animate().cancel()
        back.setImageResource(res)
        back.alpha = 0f
        val leaving = front
        val coming = back
        coming.bringToFront()
        coming.animate().alpha(1f).setDuration(millis).withEndAction { leaving.alpha = 0f }.start()
        front = coming
        back = leaving
    }

    private val mouth = object : Runnable {
        override fun run() {
            if (!talking || shown == null) return
            mouthOpen = !mouthOpen
            put(if (mouthOpen) frames.talk else frames.still)
            main.postDelayed(this, if (mouthOpen) Random.nextLong(140, 220) else Random.nextLong(90, 170))
        }
    }

    private val blink = object : Runnable {
        override fun run() {
            if (shown != null && !talking && frame == frames.still) {
                put(frames.blink)
                main.postDelayed({ if (!talking && frame == frames.blink) put(frames.still) }, BLINK_MILLIS)
            }
            main.postDelayed(this, Random.nextLong(2_800, 5_500))
        }
    }

    /** Still, lean left, still, lean right: slow fades so it reads as one movement. */
    private val sway = object : Runnable {
        override fun run() {
            if (shown != null && !talking) {
                swayStep = (swayStep + 1) % 4
                val f = frames
                fadeTo(
                    when (swayStep) {
                        1 -> f.left
                        3 -> f.right
                        else -> f.still
                    },
                    SWAY_FADE,
                )
            }
            main.postDelayed(this, SWAY_HOLD)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!moves) return
        main.postDelayed(blink, 2_000)
        main.postDelayed(sway, SWAY_HOLD)
        if (talking) main.post(mouth)
        breath = ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat(SCALE_X, 1f, 1.03f),
            PropertyValuesHolder.ofFloat(SCALE_Y, 1f, 1.03f),
        ).apply {
            duration = BREATH_MILLIS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    override fun onDetachedFromWindow() {
        main.removeCallbacksAndMessages(null)
        breath?.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val MOOD_FADE = 220L
        const val SWAY_FADE = 650L
        const val SWAY_HOLD = 1_400L
        const val BLINK_MILLIS = 130L
        const val BREATH_MILLIS = 1_500L

        val FRAMES = mapOf(
            Mood.CALM to Frames(R.drawable.peri_calm, R.drawable.peri_calm_blink, R.drawable.peri_calm_talk, R.drawable.peri_calm_swayleft, R.drawable.peri_calm_swayright),
            Mood.LISTENING to Frames(R.drawable.peri_listening, R.drawable.peri_listening_blink, R.drawable.peri_listening_talk, R.drawable.peri_listening_swayleft, R.drawable.peri_listening_swayright),
            Mood.THINKING to Frames(R.drawable.peri_thinking, R.drawable.peri_thinking_blink, R.drawable.peri_thinking_talk, R.drawable.peri_thinking_swayleft, R.drawable.peri_thinking_swayright),
            Mood.WORRIED to Frames(R.drawable.peri_worried, R.drawable.peri_worried_blink, R.drawable.peri_worried_talk, R.drawable.peri_worried_swayleft, R.drawable.peri_worried_swayright),
            Mood.PROTECTIVE to Frames(R.drawable.peri_protective, R.drawable.peri_protective_blink, R.drawable.peri_protective_talk, R.drawable.peri_protective_swayleft, R.drawable.peri_protective_swayright),
            Mood.CALLING to Frames(R.drawable.peri_calling, R.drawable.peri_calling_blink, R.drawable.peri_calling_talk, R.drawable.peri_calling_swayleft, R.drawable.peri_calling_swayright),
            Mood.WAITING to Frames(R.drawable.peri_waiting, R.drawable.peri_waiting_blink, R.drawable.peri_waiting_talk, R.drawable.peri_waiting_swayleft, R.drawable.peri_waiting_swayright),
            Mood.HAPPY to Frames(R.drawable.peri_happy, R.drawable.peri_happy_blink, R.drawable.peri_happy_talk, R.drawable.peri_happy_swayleft, R.drawable.peri_happy_swayright),
            Mood.SLEEPING to Frames(R.drawable.peri_sleeping, R.drawable.peri_sleeping_blink, R.drawable.peri_sleeping_talk, R.drawable.peri_sleeping_swayleft, R.drawable.peri_sleeping_swayright),
        )

        val DESCRIPTIONS = mapOf(
            Mood.CALM to R.string.mood_calm,
            Mood.LISTENING to R.string.mood_listening,
            Mood.THINKING to R.string.mood_thinking,
            Mood.WORRIED to R.string.mood_worried,
            Mood.PROTECTIVE to R.string.mood_protective,
            Mood.CALLING to R.string.mood_calling,
            Mood.WAITING to R.string.mood_waiting,
            Mood.HAPPY to R.string.mood_happy,
            Mood.SLEEPING to R.string.mood_sleeping,
        )
    }
}
