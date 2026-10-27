package app.friendlypear.ui.peri

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Choreographer
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Peri on screen, cut out of her Muse Image illustrations (assets/peri/) so she stands on any background.
 * The motion is smooth and never blends two poses: she tilts and bobs as one image, with a soft shadow that
 * shrinks as she rises, and each mood moves its own way (a bouncy happy Peri, a steady protective one, slow
 * breathing asleep). Only quick changes swap frames: a four-step blink, a glance to one side now and then, and
 * three mouth shapes while she speaks.
 * Moods cross-fade with a small pop. With "remove animations" on she changes mood without moving.
 */
class PeriFace(context: Context) : FrameLayout(context) {
    private data class Frames(
        val still: Int,
        val half: Int,
        val closed: Int,
        val talk: Int,
        val wide: Int,
        val round: Int,
        val left: Int,
        val right: Int,
    ) {
        val all = setOf(still, half, closed, talk, wide, round, left, right)
    }

    /** How each mood moves: tilt in degrees and its period, bob in dp and its period, breathing depth. */
    private data class Motion(
        val tilt: Float,
        val tiltMs: Long,
        val bob: Float,
        val bobMs: Long,
        val bounce: Boolean = false,
        val breath: Float = 0.012f,
        val breathMs: Long = 3_200,
        val blinks: Boolean = true,
    )

    private val moves = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    private val main = Handler(Looper.getMainLooper())
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private var feetY = 0f
    private var shadowWidth = 0f
    private var rise = 0f
    private val rig = FrameLayout(context)
    private var front = layer()
    private var back = layer()
    private var shown: Mood? = null
    private var frames = FRAMES.getValue(Mood.CALM)
    private val loaded = HashMap<Int, Drawable>()
    private var talking = false
    private var fading = false
    private var popStart = 0L
    private val pop = OvershootInterpolator(2.2f)
    private var running = false

    init {
        rig.addView(back, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        rig.addView(front, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(rig, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        back.alpha = 0f
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        clipChildren = false
        clipToPadding = false
    }

    private fun layer() = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** Mini Peri handles touches itself (tap, drag, long press) and reports a tap here, for accessibility. */
    override fun performClick(): Boolean = super.performClick()

    /** The illustrations are square; her feet are near the bottom of that square, wherever it lands. */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val side = min(w, h).toFloat()
        feetY = (h - side) / 2 + side * FEET
        shadowWidth = side * 0.44f
        rig.pivotX = w / 2f
        rig.pivotY = feetY
        // A view squeezed to nothing (a crowded screen) has no room for a shadow: a zero radius would crash.
        shadow.shader = if (shadowWidth > 1f) RadialGradient(0f, 0f, shadowWidth / 2, Palette.SHADOW, 0x00000000, Shader.TileMode.CLAMP) else null
    }

    /** Her shadow on the ground, under the picture: smaller and lighter as she rises. */
    override fun dispatchDraw(canvas: Canvas) {
        if (shadow.shader != null) {
            val scale = 1 - rise * 0.22f
            canvas.save()
            canvas.translate(width / 2f, feetY)
            canvas.scale(scale, scale * SHADOW_FLATNESS)
            shadow.alpha = (255 * (1 - rise * 0.35f)).toInt()
            canvas.drawCircle(0f, 0f, shadowWidth / 2, shadow)
            canvas.restore()
        }
        super.dispatchDraw(canvas)
    }

    private fun drawable(res: Int): Drawable = loaded.getOrPut(res) { context.getDrawable(res)!! }

    fun show(mood: Mood) {
        if (mood == shown) return
        val first = shown == null
        shown = mood
        frames = FRAMES.getValue(mood)
        contentDescription = context.getString(DESCRIPTIONS.getValue(mood))
        // Only this mood's frames stay in memory: each is a full-quality illustration.
        loaded.keys.retainAll(frames.all)
        val still = drawable(frames.still)
        // A change during the previous fade: stop it first, or its end would hide the new mood.
        front.animate().cancel()
        back.animate().cancel()
        front.alpha = 1f
        back.alpha = 0f
        fading = false
        if (!moves || first) {
            front.setImageDrawable(still)
        } else {
            fading = true
            back.setImageDrawable(still)
            back.alpha = 0f
            back.bringToFront()
            val leaving = front
            back.animate().alpha(1f).setDuration(MOOD_FADE).withEndAction {
                leaving.alpha = 0f
                fading = false
            }.start()
            front = back
            back = leaving
            popStart = SystemClock.uptimeMillis()
        }
        // The other frames decode a moment later, so the change itself is instant.
        main.post { frames.all.forEach { drawable(it) } }
    }

    /** The mouth moves while Peri's voice plays. */
    fun speaking(on: Boolean) {
        if (on == talking) return
        talking = on
        main.removeCallbacks(mouth)
        if (on && moves) main.post(mouth) else if (!on && shown != null) put(frames.still)
    }

    /** A happy jump for a moment (she was tapped), then back to her mood. */
    fun cheer() {
        val mood = shown ?: return
        if (!moves || mood == Mood.HAPPY) return
        show(Mood.HAPPY)
        main.postDelayed({ if (shown == Mood.HAPPY) show(mood) }, CHEER_MILLIS)
    }

    /** A little wave hello, when the app opens and she's calm. */
    fun greet() {
        if (!moves || shown != Mood.CALM || talking) return
        val steps = listOf(R.drawable.peri_calm_wave1, R.drawable.peri_calm_wave2, R.drawable.peri_calm_wave1, R.drawable.peri_calm_wave2, R.drawable.peri_calm_wave1)
        steps.forEachIndexed { i, res -> main.postDelayed({ if (shown == Mood.CALM && !talking) put(res) }, WAVE_START + i * WAVE_STEP) }
        main.postDelayed({ if (shown == Mood.CALM && !talking) put(frames.still) }, WAVE_START + steps.size * WAVE_STEP)
    }

    /** Swaps the frame at once: blinks and the mouth are quick, and a swap never blends two pictures. */
    private fun put(res: Int) {
        // A blink or wave scheduled before a mood change never shows a frame of the old mood.
        val waving = shown == Mood.CALM && (res == R.drawable.peri_calm_wave1 || res == R.drawable.peri_calm_wave2)
        if (fading || (res !in frames.all && !waving)) return
        front.setImageDrawable(drawable(res))
    }

    private val mouth = object : Runnable {
        private var last = 0
        override fun run() {
            if (!talking || shown == null) return
            val next = listOf(frames.talk, frames.wide, frames.round, frames.still).filter { it != last }.random()
            last = next
            put(next)
            main.postDelayed(this, if (next == frames.still) Random.nextLong(70, 130) else Random.nextLong(90, 170))
        }
    }

    private val blink = object : Runnable {
        override fun run() {
            if (shown != null && !talking && MOTIONS.getValue(shown!!).blinks) {
                val steps = listOf(frames.half to 0L, frames.closed to 45L, frames.half to 125L, frames.still to 170L)
                steps.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res) }, at) }
                // Now and then a double blink, as people do.
                if (Random.nextInt(5) == 0) steps.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res) }, 260 + at) }
            }
            main.postDelayed(this, Random.nextLong(2_600, 5_200))
        }
    }

    /** Now and then she glances to one side and back, like someone keeping you company. */
    private val glance = object : Runnable {
        override fun run() {
            if (shown != null && !talking && MOTIONS.getValue(shown!!).blinks) {
                val side = if (Random.nextBoolean()) frames.left else frames.right
                put(side)
                main.postDelayed({ if (!talking) put(frames.still) }, Random.nextLong(700, 1_200))
            }
            main.postDelayed(this, Random.nextLong(6_000, 11_000))
        }
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val t = SystemClock.uptimeMillis()
            val m = MOTIONS.getValue(shown ?: Mood.CALM)
            fun wave(period: Long) = sin(2 * PI * (t % period) / period).toFloat()
            rise = if (m.bounce) abs(sin(PI * (t % m.bobMs) / m.bobMs).toFloat()) else (1 - wave(m.bobMs)) / 2
            val breath = wave(m.breathMs) * m.breath
            val p = ((t - popStart) / POP_MS).coerceIn(0f, 1f)
            val popScale = 0.94f + 0.06f * pop.getInterpolation(p)
            rig.rotation = wave(m.tiltMs) * m.tilt
            rig.translationY = -rise * m.bob * resources.displayMetrics.density
            // Squash and stretch keeps her volume: taller is a touch thinner.
            rig.scaleY = popScale * (1 + breath)
            rig.scaleX = popScale * (1 - breath / 2)
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!moves) return
        running = true
        Choreographer.getInstance().postFrameCallback(frame)
        main.postDelayed(blink, 1_800)
        main.postDelayed(glance, 5_000)
        if (talking) main.post(mouth)
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
        main.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private companion object {
        const val MOOD_FADE = 200L
        const val CHEER_MILLIS = 2_200L
        const val POP_MS = 420f
        const val WAVE_START = 400L
        const val WAVE_STEP = 170L
        /** Where her feet are, as a fraction of the illustration's height. */
        const val FEET = 0.885f
        /** The shadow is a circle squashed flat onto the ground. */
        const val SHADOW_FLATNESS = 0.16f

        val MOTIONS = mapOf(
            Mood.CALM to Motion(tilt = 1.8f, tiltMs = 4_200, bob = 3f, bobMs = 2_100),
            Mood.LISTENING to Motion(tilt = 1.2f, tiltMs = 3_400, bob = 1.5f, bobMs = 1_700),
            Mood.THINKING to Motion(tilt = 2.4f, tiltMs = 3_000, bob = 2f, bobMs = 1_500),
            Mood.WORRIED to Motion(tilt = 1f, tiltMs = 2_400, bob = 1f, bobMs = 1_200),
            Mood.PROTECTIVE to Motion(tilt = 0.6f, tiltMs = 3_600, bob = 1f, bobMs = 1_800),
            Mood.CALLING to Motion(tilt = 1.5f, tiltMs = 3_000, bob = 2f, bobMs = 1_500),
            Mood.WAITING to Motion(tilt = 3f, tiltMs = 5_000, bob = 1.5f, bobMs = 2_500),
            Mood.HAPPY to Motion(tilt = 3f, tiltMs = 1_800, bob = 12f, bobMs = 900, bounce = true),
            Mood.SLEEPING to Motion(tilt = 0f, tiltMs = 1_000, bob = 0f, bobMs = 1_000, breath = 0.022f, breathMs = 4_600, blinks = false),
        )

        val FRAMES = mapOf(
            Mood.CALM to Frames(
                R.drawable.peri_calm, R.drawable.peri_calm_blinkhalf, R.drawable.peri_calm_blink, R.drawable.peri_calm_talk,
                R.drawable.peri_calm_talkwide, R.drawable.peri_calm_talko, R.drawable.peri_calm_glanceleft, R.drawable.peri_calm_glanceright,
            ),
            Mood.LISTENING to Frames(
                R.drawable.peri_listening, R.drawable.peri_listening_blinkhalf, R.drawable.peri_listening_blink, R.drawable.peri_listening_talk,
                R.drawable.peri_listening_talkwide, R.drawable.peri_listening_talko, R.drawable.peri_listening_glanceleft, R.drawable.peri_listening_glanceright,
            ),
            Mood.THINKING to Frames(
                R.drawable.peri_thinking, R.drawable.peri_thinking_blinkhalf, R.drawable.peri_thinking_blink, R.drawable.peri_thinking_talk,
                R.drawable.peri_thinking_talkwide, R.drawable.peri_thinking_talko, R.drawable.peri_thinking_glanceleft, R.drawable.peri_thinking_glanceright,
            ),
            Mood.WORRIED to Frames(
                R.drawable.peri_worried, R.drawable.peri_worried_blinkhalf, R.drawable.peri_worried_blink, R.drawable.peri_worried_talk,
                R.drawable.peri_worried_talkwide, R.drawable.peri_worried_talko, R.drawable.peri_worried_glanceleft, R.drawable.peri_worried_glanceright,
            ),
            Mood.PROTECTIVE to Frames(
                R.drawable.peri_protective, R.drawable.peri_protective_blinkhalf, R.drawable.peri_protective_blink, R.drawable.peri_protective_talk,
                R.drawable.peri_protective_talkwide, R.drawable.peri_protective_talko, R.drawable.peri_protective_glanceleft, R.drawable.peri_protective_glanceright,
            ),
            Mood.CALLING to Frames(
                R.drawable.peri_calling, R.drawable.peri_calling_blinkhalf, R.drawable.peri_calling_blink, R.drawable.peri_calling_talk,
                R.drawable.peri_calling_talkwide, R.drawable.peri_calling_talko, R.drawable.peri_calling_glanceleft, R.drawable.peri_calling_glanceright,
            ),
            Mood.WAITING to Frames(
                R.drawable.peri_waiting, R.drawable.peri_waiting_blinkhalf, R.drawable.peri_waiting_blink, R.drawable.peri_waiting_talk,
                R.drawable.peri_waiting_talkwide, R.drawable.peri_waiting_talko, R.drawable.peri_waiting_glanceleft, R.drawable.peri_waiting_glanceright,
            ),
            Mood.HAPPY to Frames(
                R.drawable.peri_happy, R.drawable.peri_happy_blinkhalf, R.drawable.peri_happy_blink, R.drawable.peri_happy_talk,
                R.drawable.peri_happy_talkwide, R.drawable.peri_happy_talko, R.drawable.peri_happy_glanceleft, R.drawable.peri_happy_glanceright,
            ),
            Mood.SLEEPING to Frames(
                R.drawable.peri_sleeping, R.drawable.peri_sleeping_blinkhalf, R.drawable.peri_sleeping_blink, R.drawable.peri_sleeping_talk,
                R.drawable.peri_sleeping_talkwide, R.drawable.peri_sleeping_talko, R.drawable.peri_sleeping_glanceleft, R.drawable.peri_sleeping_glanceright,
            ),
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
