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
 * The motion is smooth and never blends two poses: she tilts, shifts her weight and bobs as one image, with a
 * soft shadow that shrinks as she rises, squashes a little when she lands and nods as she speaks; each mood
 * moves its own way (a happy Peri runs on the spot, a steady protective one, slow breathing asleep). Now and
 * then she does something by herself: a hop, steps or a little dance, a spin, a wink, a look around, a stretch,
 * a wave. She drops in when the screen opens, hops when her mood changes, squishes like clay when pressed and
 * talks with her hands. Only quick changes swap frames: blinks, glances, steps, gestures and mouth shapes.
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
        /** One foot lifted (left or right side of the picture), for steps; none while asleep. */
        val stepLeft: Int? = null,
        val stepRight: Int? = null,
        /** Talking with her hands: one hand raised, then both arms open; and a wink. */
        val gestureA: Int? = null,
        val gestureB: Int? = null,
        val wink: Int? = null,
    ) {
        val all = setOfNotNull(still, half, closed, talk, wide, round, left, right, stepLeft, stepRight, gestureA, gestureB, wink)
    }

    /** What she does by herself now and then, when nothing else is going on; DROP is her entrance. */
    private enum class Idle { HOP, STEPS, LOOK, WAVE, STRETCH, WINK, SPIN, DANCE, DROP }

    /** One moment of an action: how high (dp), squash (−) or stretch (+), extra tilt (°) and width (−1 is turned round). */
    private data class Shape(val lift: Float = 0f, val squash: Float = 0f, val tilt: Float = 0f, val width: Float = 1f)

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
        /** Side-to-side weight shift in dp, in time with the tilt. */
        val sway: Float = 1f,
        val idles: List<Idle> = emptyList(),
        /** Runs on the spot: a foot up on every bounce. */
        val runs: Boolean = false,
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
    /** While a pose (an idea, "sorry", a thumbs up) is on, blinks and glances wait. */
    private var posing = false
    /** The idle action going on (a hop, steps, a stretch) and when it started. */
    private var action: Idle? = null
    private var actionStart = 0L
    /** Until then a blink or glance owns the frame, so running steps don't cut it short. */
    private var busyUntil = 0L
    private var lastRun = -1L
    /** The start of the little nod that goes with each word. */
    private var nodStart = 0L
    /** Pressed, she squishes; let go, she springs back past straight and settles, like clay. */
    private var pressTarget = 0f
    private var pressX = 0f
    private var pressV = 0f
    private var lastFrame = 0L

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
        action = null
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
            if (mood != Mood.SLEEPING) act(Idle.HOP)
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

    /** One of the poses for a couple of seconds, with a little pop, then back to her mood. */
    fun moment(moment: app.friendlypear.services.PeriMoment) {
        if (shown == null) return
        val res = when (moment) {
            app.friendlypear.services.PeriMoment.IDEA -> R.drawable.peri_calm_idea
            app.friendlypear.services.PeriMoment.SORRY -> R.drawable.peri_calm_sorry
            app.friendlypear.services.PeriMoment.THUMBS_UP -> R.drawable.peri_calm_thumbsup
        }
        front.animate().cancel()
        back.animate().cancel()
        front.alpha = 1f
        back.alpha = 0f
        fading = false
        posing = true
        action = null
        front.setImageDrawable(context.getDrawable(res))
        popStart = SystemClock.uptimeMillis()
        main.removeCallbacks(endPose)
        main.postDelayed(endPose, POSE_MILLIS)
    }

    private val endPose = Runnable {
        posing = false
        put(frames.still)
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
        if (fading || posing || (res !in frames.all && !waving)) return
        front.setImageDrawable(drawable(res))
    }

    private val mouth = object : Runnable {
        private var last = 0
        override fun run() {
            if (!talking || shown == null) return
            // Now and then a hand gesture (her mouth open in it too), held a little longer, like people do.
            val gesture = listOfNotNull(frames.gestureA, frames.gestureB).filter { it != last }
            if (gesture.isNotEmpty() && last !in gesture && Random.nextInt(9) == 0) {
                val g = gesture.random()
                last = g
                put(g)
                nodStart = SystemClock.uptimeMillis()
                main.postDelayed(this, Random.nextLong(700, 1_050))
                return
            }
            val next = listOf(frames.talk, frames.wide, frames.round, frames.still).filter { it != last }.random()
            last = next
            put(next)
            if (next != frames.still) nodStart = SystemClock.uptimeMillis()
            main.postDelayed(this, if (next == frames.still) Random.nextLong(110, 180) else Random.nextLong(140, 230))
        }
    }

    private val blink = object : Runnable {
        override fun run() {
            if (shown != null && !talking && MOTIONS.getValue(shown!!).blinks) {
                busyUntil = SystemClock.uptimeMillis() + 480
                val steps = listOf(frames.half to 0L, frames.closed to 45L, frames.half to 125L, frames.still to 170L)
                steps.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res) }, at) }
                // Now and then a double blink, as people do.
                if (Random.nextInt(7) == 0) steps.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res) }, 260 + at) }
            }
            main.postDelayed(this, Random.nextLong(3_500, 6_500))
        }
    }

    /** Now and then she glances to one side and back, like someone keeping you company. */
    private val glance = object : Runnable {
        override fun run() {
            if (shown != null && !talking && MOTIONS.getValue(shown!!).blinks) {
                val side = if (Random.nextBoolean()) frames.left else frames.right
                val back = Random.nextLong(1_000, 1_600)
                busyUntil = SystemClock.uptimeMillis() + back
                put(side)
                main.postDelayed({ if (!talking) put(frames.still) }, back)
            }
            main.postDelayed(this, Random.nextLong(8_000, 14_000))
        }
    }

    /** Every several seconds, if she's free, one of her mood's idle actions. */
    private val idle = object : Runnable {
        override fun run() {
            val mood = shown
            if (mood != null && !talking && !posing && !fading && action == null) {
                MOTIONS.getValue(mood).idles.randomOrNull()?.let { act(it) }
            }
            main.postDelayed(this, Random.nextLong(IDLE_MIN, IDLE_MAX))
        }
    }

    private fun act(idle: Idle) {
        val now = SystemClock.uptimeMillis()
        when (idle) {
            Idle.HOP, Idle.STRETCH -> {
                action = idle
                actionStart = now
                // Eyes softly half closed for a stretch, as if it felt good.
                if (idle == Idle.STRETCH) {
                    put(frames.half)
                    main.postDelayed({ if (!talking) put(frames.still) }, STRETCH_MS)
                }
            }
            Idle.STEPS -> {
                val l = frames.stepLeft ?: return
                val r = frames.stepRight ?: return
                action = idle
                actionStart = now
                busyUntil = now + STEPS * STEP_MS
                val steps = (0 until STEPS).map { if (it % 2 == 0) (if (it % 4 == 0) l else r) else frames.still }
                steps.forEachIndexed { i, res -> main.postDelayed({ if (!talking && action == Idle.STEPS) put(res) }, i * STEP_MS) }
                main.postDelayed({ if (!talking) put(frames.still) }, STEPS * STEP_MS)
            }
            Idle.LOOK -> {
                busyUntil = now + 2_100
                put(frames.left)
                main.postDelayed({ if (!talking) put(frames.right) }, 950)
                main.postDelayed({ if (!talking) put(frames.still) }, 1_950)
            }
            Idle.WAVE -> greet()
            Idle.WINK -> {
                val w = frames.wink ?: return
                busyUntil = now + 850
                put(w)
                main.postDelayed({ if (!talking) put(frames.still) }, 750)
            }
            Idle.SPIN, Idle.DROP -> {
                action = idle
                actionStart = now
            }
            Idle.DANCE -> {
                val l = frames.stepLeft ?: return
                val r = frames.stepRight ?: return
                action = idle
                actionStart = now
                busyUntil = now + DANCE_STEPS * DANCE_MS
                (0 until DANCE_STEPS).forEach { i ->
                    main.postDelayed({ if (!talking && action == Idle.DANCE) put(if (i % 2 == 0) l else r) }, i * DANCE_MS)
                }
                main.postDelayed({ if (!talking) put(frames.still) }, DANCE_STEPS * DANCE_MS)
            }
        }
    }

    /** Her entrance: she drops in from above and lands with a squash. */
    fun enter() {
        if (!moves) return
        act(Idle.DROP)
    }

    /** Pressed and let go, for the squish. */
    fun press(on: Boolean) {
        pressTarget = if (on) 1f else 0f
    }

    /**
     * The current action at time [t]. A hop crouches first, springs up a little stretched and lands with a squash;
     * a spin turns her round with a small jump; a dance rocks her side to side on each step.
     */
    private fun actionShape(t: Long): Shape {
        val e = (t - actionStart).toFloat()
        fun done(): Shape = Shape().also { action = null }
        return when (action) {
            Idle.HOP -> when {
                e < 130 -> Shape(squash = -0.08f * sin(PI / 2 * e / 130).toFloat())
                e < 480 -> {
                    val a = (e - 130) / 350
                    Shape(lift = HOP_DP * 4 * a * (1 - a), squash = 0.05f * (1 - 2 * a).coerceAtLeast(0f))
                }
                e < HOP_MS -> Shape(squash = -0.08f * sin(PI * (e - 480) / (HOP_MS - 480)).toFloat())
                else -> done()
            }
            Idle.STEPS -> if (e < STEPS * STEP_MS) {
                // A small bounce on each step.
                val ph = (e % STEP_MS) / STEP_MS
                Shape(lift = 3f * sin(PI * ph).toFloat())
            } else done()
            Idle.STRETCH -> if (e < STRETCH_MS) Shape(squash = 0.06f * sin(PI * e / STRETCH_MS).toFloat()) else done()
            Idle.SPIN -> if (e < SPIN_MS) {
                val a = e / SPIN_MS
                Shape(lift = 10f * 4 * a * (1 - a), width = kotlin.math.cos(2 * PI * a).toFloat())
            } else done()
            Idle.DANCE -> if (e < DANCE_STEPS * DANCE_MS) {
                val ph = (e % DANCE_MS) / DANCE_MS
                val side = if ((e / DANCE_MS).toInt() % 2 == 0) 1f else -1f
                Shape(lift = 5f * sin(PI * ph).toFloat(), tilt = side * 7f * sin(PI * ph).toFloat())
            } else done()
            Idle.DROP -> when {
                e < DROP_MS -> {
                    // Falling under gravity from high above, a touch stretched by the speed.
                    val a = e / DROP_MS
                    Shape(lift = DROP_DP * (1 - a * a), squash = 0.06f * a)
                }
                e < DROP_MS + 220 -> Shape(squash = -0.12f * sin(PI * (e - DROP_MS) / 220).toFloat())
                else -> done()
            }
            else -> Shape()
        }
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val t = SystemClock.uptimeMillis()
            val m = MOTIONS.getValue(shown ?: Mood.CALM)
            fun wave(period: Long) = sin(2 * PI * (t % period) / period).toFloat()
            val density = resources.displayMetrics.density
            rise = if (m.bounce) abs(sin(PI * (t % m.bobMs) / m.bobMs).toFloat()) else (1 - wave(m.bobMs)) / 2
            val breath = wave(m.breathMs) * m.breath
            val p = ((t - popStart) / POP_MS).coerceIn(0f, 1f)
            val popScale = 0.94f + 0.06f * pop.getInterpolation(p)
            // Bouncing, she squashes a little each time she touches the ground.
            val landing = if (m.bounce) -LAND_SQUASH * (1 - rise).let { it * it * it * it * it * it } else 0f
            // A little nod with each word.
            val n = ((t - nodStart) / NOD_MS).coerceIn(0f, 1f)
            val nod = if (talking) sin(PI * n).toFloat() * NOD else 0f
            val shape = actionShape(t)
            val lift = shape.lift
            // The squish: a spring towards pressed (1) or free (0), so letting go overshoots and wobbles.
            val dt = ((t - lastFrame).coerceIn(1, 50)) / 1000f
            lastFrame = t
            pressV += (SPRING_K * (pressTarget - pressX) - SPRING_C * pressV) * dt
            pressX += pressV * dt
            val squash = breath + landing + shape.squash + nod - PRESS_SQUASH * pressX
            // Two waves at unrelated speeds: her sway never looks like a metronome.
            val sway = (wave(m.tiltMs) + 0.4f * wave((m.tiltMs * 0.61f).toLong())) / 1.4f
            rig.rotation = sway * m.tilt + shape.tilt
            rig.translationX = sway * m.sway * density
            rig.translationY = -(rise * m.bob + lift) * density
            // Squash and stretch keeps her volume: taller is a touch thinner.
            rig.scaleY = popScale * (1 + squash)
            rig.scaleX = popScale * (1 - squash / 2) * shape.width
            if (lift > 0f) rise = maxOf(rise, (lift / HOP_DP).coerceAtMost(1f))
            // Happy, she runs on the spot: the other foot up on every bounce.
            if (m.runs && !talking && !posing && action == null && t > busyUntil) {
                val step = t / m.bobMs
                if (step != lastRun) {
                    lastRun = step
                    (if (step % 2 == 0L) frames.stepLeft else frames.stepRight)?.let { put(it) }
                }
            }
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
        main.postDelayed(idle, Random.nextLong(IDLE_FIRST, IDLE_MIN))
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
        const val POSE_MILLIS = 2_800L
        const val POP_MS = 420f
        const val IDLE_FIRST = 3_500L
        const val IDLE_MIN = 7_000L
        const val IDLE_MAX = 12_000L
        const val SPIN_MS = 720f
        const val DANCE_MS = 380L
        const val DANCE_STEPS = 8
        const val DROP_MS = 420f
        const val DROP_DP = 90f
        const val SPRING_K = 260f
        const val SPRING_C = 15f
        const val PRESS_SQUASH = 0.1f
        const val HOP_MS = 640f
        const val HOP_DP = 18f
        const val STEP_MS = 340L
        const val STEPS = 8
        const val STRETCH_MS = 900L
        const val LAND_SQUASH = 0.07f
        const val NOD_MS = 170f
        const val NOD = 0.018f
        const val WAVE_START = 400L
        const val WAVE_STEP = 240L
        /** Where her feet are, as a fraction of the illustration's height. */
        const val FEET = 0.885f
        /** The shadow is a circle squashed flat onto the ground. */
        const val SHADOW_FLATNESS = 0.16f

        val MOTIONS = mapOf(
            Mood.CALM to Motion(tilt = 3f, tiltMs = 3_800, bob = 5f, bobMs = 1_900, sway = 2.5f, idles = listOf(Idle.HOP, Idle.STEPS, Idle.LOOK, Idle.WAVE, Idle.STRETCH, Idle.WINK, Idle.SPIN, Idle.DANCE)),
            Mood.LISTENING to Motion(tilt = 2f, tiltMs = 3_000, bob = 2.5f, bobMs = 1_500, sway = 1f),
            Mood.THINKING to Motion(tilt = 3.5f, tiltMs = 2_800, bob = 3f, bobMs = 1_400, sway = 1.5f, idles = listOf(Idle.LOOK, Idle.STEPS, Idle.STRETCH)),
            Mood.WORRIED to Motion(tilt = 1.5f, tiltMs = 2_000, bob = 1.5f, bobMs = 1_000, sway = 1.2f, idles = listOf(Idle.STEPS, Idle.LOOK)),
            Mood.PROTECTIVE to Motion(tilt = 1.2f, tiltMs = 3_200, bob = 2f, bobMs = 1_600, sway = 0.8f, idles = listOf(Idle.LOOK, Idle.STEPS)),
            Mood.CALLING to Motion(tilt = 2.5f, tiltMs = 2_800, bob = 3f, bobMs = 1_400, sway = 1.5f, idles = listOf(Idle.STEPS, Idle.LOOK, Idle.WINK)),
            Mood.WAITING to Motion(tilt = 4f, tiltMs = 4_400, bob = 2.5f, bobMs = 2_200, sway = 2.5f, idles = listOf(Idle.STEPS, Idle.LOOK, Idle.STRETCH, Idle.DANCE)),
            Mood.HAPPY to Motion(tilt = 4.5f, tiltMs = 1_800, bob = 16f, bobMs = 1_000, bounce = true, sway = 3f, idles = listOf(Idle.SPIN, Idle.WINK, Idle.HOP), runs = true),
            Mood.SLEEPING to Motion(tilt = 0.8f, tiltMs = 6_000, bob = 0f, bobMs = 1_000, breath = 0.03f, breathMs = 4_600, blinks = false, sway = 0f),
        )

        val FRAMES = mapOf(
            Mood.CALM to Frames(
                R.drawable.peri_calm, R.drawable.peri_calm_blinkhalf, R.drawable.peri_calm_blink, R.drawable.peri_calm_talk,
                R.drawable.peri_calm_talkwide, R.drawable.peri_calm_talko, R.drawable.peri_calm_glanceleft, R.drawable.peri_calm_glanceright,
                R.drawable.peri_calm_stepleft, R.drawable.peri_calm_stepright,
                R.drawable.peri_calm_gesturea, R.drawable.peri_calm_gestureb, R.drawable.peri_calm_wink,
            ),
            Mood.LISTENING to Frames(
                R.drawable.peri_listening, R.drawable.peri_listening_blinkhalf, R.drawable.peri_listening_blink, R.drawable.peri_listening_talk,
                R.drawable.peri_listening_talkwide, R.drawable.peri_listening_talko, R.drawable.peri_listening_glanceleft, R.drawable.peri_listening_glanceright,
                R.drawable.peri_listening_stepleft, R.drawable.peri_listening_stepright,
                R.drawable.peri_listening_gesturea, R.drawable.peri_listening_gestureb, R.drawable.peri_listening_wink,
            ),
            Mood.THINKING to Frames(
                R.drawable.peri_thinking, R.drawable.peri_thinking_blinkhalf, R.drawable.peri_thinking_blink, R.drawable.peri_thinking_talk,
                R.drawable.peri_thinking_talkwide, R.drawable.peri_thinking_talko, R.drawable.peri_thinking_glanceleft, R.drawable.peri_thinking_glanceright,
                R.drawable.peri_thinking_stepleft, R.drawable.peri_thinking_stepright,
                R.drawable.peri_thinking_gesturea, R.drawable.peri_thinking_gestureb, R.drawable.peri_thinking_wink,
            ),
            Mood.WORRIED to Frames(
                R.drawable.peri_worried, R.drawable.peri_worried_blinkhalf, R.drawable.peri_worried_blink, R.drawable.peri_worried_talk,
                R.drawable.peri_worried_talkwide, R.drawable.peri_worried_talko, R.drawable.peri_worried_glanceleft, R.drawable.peri_worried_glanceright,
                R.drawable.peri_worried_stepleft, R.drawable.peri_worried_stepright,
                R.drawable.peri_worried_gesturea, R.drawable.peri_worried_gestureb, R.drawable.peri_worried_wink,
            ),
            Mood.PROTECTIVE to Frames(
                R.drawable.peri_protective, R.drawable.peri_protective_blinkhalf, R.drawable.peri_protective_blink, R.drawable.peri_protective_talk,
                R.drawable.peri_protective_talkwide, R.drawable.peri_protective_talko, R.drawable.peri_protective_glanceleft, R.drawable.peri_protective_glanceright,
                R.drawable.peri_protective_stepleft, R.drawable.peri_protective_stepright,
                R.drawable.peri_protective_gesturea, R.drawable.peri_protective_gestureb, R.drawable.peri_protective_wink,
            ),
            Mood.CALLING to Frames(
                R.drawable.peri_calling, R.drawable.peri_calling_blinkhalf, R.drawable.peri_calling_blink, R.drawable.peri_calling_talk,
                R.drawable.peri_calling_talkwide, R.drawable.peri_calling_talko, R.drawable.peri_calling_glanceleft, R.drawable.peri_calling_glanceright,
                R.drawable.peri_calling_stepleft, R.drawable.peri_calling_stepright,
                R.drawable.peri_calling_gesturea, R.drawable.peri_calling_gestureb, R.drawable.peri_calling_wink,
            ),
            Mood.WAITING to Frames(
                R.drawable.peri_waiting, R.drawable.peri_waiting_blinkhalf, R.drawable.peri_waiting_blink, R.drawable.peri_waiting_talk,
                R.drawable.peri_waiting_talkwide, R.drawable.peri_waiting_talko, R.drawable.peri_waiting_glanceleft, R.drawable.peri_waiting_glanceright,
                R.drawable.peri_waiting_stepleft, R.drawable.peri_waiting_stepright,
                R.drawable.peri_waiting_gesturea, R.drawable.peri_waiting_gestureb, R.drawable.peri_waiting_wink,
            ),
            Mood.HAPPY to Frames(
                R.drawable.peri_happy, R.drawable.peri_happy_blinkhalf, R.drawable.peri_happy_blink, R.drawable.peri_happy_talk,
                R.drawable.peri_happy_talkwide, R.drawable.peri_happy_talko, R.drawable.peri_happy_glanceleft, R.drawable.peri_happy_glanceright,
                R.drawable.peri_happy_stepleft, R.drawable.peri_happy_stepright,
                R.drawable.peri_happy_gesturea, R.drawable.peri_happy_gestureb, R.drawable.peri_happy_wink,
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
