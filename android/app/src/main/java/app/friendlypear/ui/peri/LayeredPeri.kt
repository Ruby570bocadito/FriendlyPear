package app.friendlypear.ui.peri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import android.widget.FrameLayout
import app.friendlypear.R
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Peri by layers (round 4, a preview): a body without a face, and her eyes and her mouth drawn on it as pieces cut by
 * assets/peri/layers.py. Any eyes go with any mouth on any body, placed by the eyes of the picture the body was made
 * from ([PeriAnchors]): she talks in every expression and every pose, with her hands too, blinks, follows a finger
 * with her eyes and turns her face a little towards it, and breathes, sways and hops as one clay figure. Calm, she
 * keeps the calm frame rate ([Rest]); with "remove animations" on, her face still changes, without motion.
 */
class LayeredPeri(context: Context) : FrameLayout(context) {
    /** Her expressions: eyes, mouth, a body for some, and for how long. */
    enum class Look(val eyes: String, val mouth: String, val body: String? = null, val millis: Long = 2_400) {
        HAPPY("happy", "grin"),
        LAUGH("laugh", "laugh", millis = 1_900),
        SURPRISED("wide", "surprised", millis = 1_800),
        LOVE("love", "kiss", body = "calm_love"),
        STARS("stars", "grin"),
        WORRIED("worried", "worried", millis = 2_800),
        BRAVE("brave", "smirk", body = "calm_thumbsup"),
        SLEEPY("sleepy", "yawn", millis = 3_000),
        WINK("wink_left", "tongue", millis = 1_400),
        SAD("teary", "sad", millis = 2_800),
    }

    private val moves = context.animationsOn()
    private val main = Handler(Looper.getMainLooper())
    private val sparkles = Sparkles(context)
    private val pictures = HashMap<String, Bitmap>()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var side = 0f
    private var decodedSide = 0

    /** Where her feet are, in this view's pixels: the ground of Peri's world goes there. */
    var feetY = 0f
        private set

    // What she shows. Each piece comes from the first that has one: a blink, a word, a moment (an idle bit, a wave,
    // a laugh), her look, or calm.
    private var look: Look? = null
    private var blinkEyes: String? = null
    private var word: String? = null
    private var momentEyes: String? = null
    private var momentMouth: String? = null
    /** A body for a moment: a wave, or talking with her hands. */
    private var gesture: String? = null
    private var talking = false
    private var waving = false

    /** The body on show, and when it last changed: she squashes a touch as her arms move, like clay. */
    private var bodyShown = "calm"
    private var snapStart = -SNAP_MS

    // Where she looks, from -1 to 1 each way (left and up are negative), where she wants to look and how fast it moves.
    private var gazeX = 0f
    private var gazeY = 0f
    private var gazeVX = 0f
    private var gazeVY = 0f
    private var wantX = 0f
    private var wantY = 0f
    private var touching = false

    // Pressed she squishes, let go she springs back past straight, like clay; sad, she sags a little.
    private var press = 0f
    private var pressV = 0f
    private var pressTarget = 0f
    private var sag = 0f

    private var hopStart = -HOP_MS
    private var hopHeight = 1f
    private var nodStart = -NOD_MS
    private var shiverStart = -SHIVER_MS
    private var running = false
    private var lastSlot = -1L
    private var lastStep = 0L

    // Her pose this frame.
    private var lift = 0f
    private var stretchX = 1f
    private var stretchY = 1f
    private var tilt = 0f
    private var shift = 0f

    init {
        addView(sparkles, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setWillNotDraw(false)
        clipChildren = false
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.layers_peri)
    }

    /** One of her expressions for a while, with its little hop or sparkle; then calm again, with a blink. */
    fun express(next: Look) {
        look = next
        momentEyes = null
        momentMouth = null
        main.removeCallbacks(endLook)
        main.postDelayed(endLook, next.millis)
        if (moves) when (next) {
            Look.HAPPY -> {
                hop()
                sparkles.burst(18)
            }
            Look.LAUGH -> laugh()
            Look.SURPRISED -> {
                hop(0.5f)
                sparkles.burst(6)
            }
            Look.LOVE -> sparkles.play(Sparkles.Effect.HEARTS)
            Look.STARS -> {
                hop()
                sparkles.play(Sparkles.Effect.STARS, 14)
            }
            Look.WORRIED -> shiverStart = SystemClock.uptimeMillis()
            Look.BRAVE -> {
                hop(0.7f)
                sparkles.burst(8)
            }
            // A big yawn, then her mouth closes, sleepy.
            Look.SLEEPY -> {
                sparkles.play(Sparkles.Effect.SLEEPY)
                main.postDelayed({ if (look == Look.SLEEPY) moment(null, "m", next.millis) }, YAWN_MS)
            }
            Look.WINK -> hop(0.4f)
            Look.SAD -> Unit
        }
        refresh()
    }

    private val endLook = Runnable {
        look = null
        momentMouth = null
        refresh()
        blink()
    }

    /** Ha ha: two hops and her mouth opening and closing with the laugh. */
    private fun laugh() {
        hop()
        main.postDelayed({ hop(0.55f) }, LAUGH_HOP_MS)
        sparkles.burst(12)
        repeat(6) { i -> main.postDelayed({ if (look == Look.LAUGH && !talking) moment(null, if (i % 2 == 0) "grin" else "laugh", 0L) }, LAUGH_FROM_MS + i * LAUGH_STEP_MS) }
        main.postDelayed({ if (look == Look.LAUGH) moment(null, null, 0L) }, LAUGH_FROM_MS + 6 * LAUGH_STEP_MS)
    }

    /** Her mouth moves while she talks, now and then her hands too. */
    fun talk(on: Boolean) {
        if (on == talking) return
        talking = on
        main.removeCallbacks(mouth)
        main.removeCallbacks(hands)
        if (on && moves) {
            main.post(mouth)
            main.postDelayed(hands, Random.nextLong(700, 1_400))
        } else {
            word = null
            if (!waving) gesture = null
            refresh()
        }
    }

    private val mouth = object : Runnable {
        override fun run() {
            if (!talking) return
            val next = VISEMES.filter { it != word }.random()
            word = next
            if (next !in CLOSED) nodStart = SystemClock.uptimeMillis()
            refresh()
            main.postDelayed(this, if (next in CLOSED) Random.nextLong(70, 120) else Random.nextLong(95, 175))
        }
    }

    /** Talking with her hands: one gesture now and then, held a little, like people do. */
    private val hands = object : Runnable {
        override fun run() {
            if (!talking) return
            if (!waving && look?.body == null) {
                gesture = if (gesture == null) GESTURES.random() else null
                refresh()
            }
            main.postDelayed(this, if (gesture != null) Random.nextLong(750, 1_200) else Random.nextLong(1_300, 2_600))
        }
    }

    /** Hello! Her arm up and down a few times, smiling with her eyes. */
    fun wave() {
        if (!moves || waving) return
        waving = true
        moment("happy", null, WAVES.size * WAVE_STEP_MS)
        WAVES.forEachIndexed { i, body -> main.postDelayed({ gesture = body; refresh() }, i * WAVE_STEP_MS) }
        main.postDelayed({
            waving = false
            gesture = null
            refresh()
        }, WAVES.size * WAVE_STEP_MS)
        hop(0.35f)
    }

    /** A blink: half, closed, half, open; now and then twice, as people do. */
    fun blink() {
        if (!moves || blinkEyes != null || eyes() !in BLINKING) return
        val steps = listOf("half" to 0L, "closed" to 45L, "half" to 125L, null to 170L)
        val twice = Random.nextInt(7) == 0
        (if (twice) steps + steps.map { (e, at) -> e to at + 260L } else steps).forEach { (e, at) ->
            main.postDelayed({
                blinkEyes = e
                refresh()
            }, at)
        }
    }

    /** Eyes and mouth for a moment ([millis], or until changed when 0), over her look. */
    private fun moment(eyes: String?, mouth: String?, millis: Long) {
        momentEyes = eyes
        momentMouth = mouth
        main.removeCallbacks(endMoment)
        if (millis > 0) main.postDelayed(endMoment, millis)
        refresh()
    }

    private val endMoment = Runnable {
        momentEyes = null
        momentMouth = null
        refresh()
    }

    /** She looks at a point on the screen (a finger anywhere), turning her face a little towards it. */
    fun follow(screenX: Float, screenY: Float) {
        val at = IntArray(2).also { getLocationOnScreen(it) }
        val reach = side * REACH
        if (reach <= 0f) return
        touching = true
        main.removeCallbacks(lookBack)
        wantX = ((screenX - at[0] - width / 2f) / reach).coerceIn(-1f, 1f)
        wantY = ((screenY - at[1] - faceY()) / reach).coerceIn(-1f, 1f)
        wake()
    }

    /** The finger went: in a moment she looks ahead again. */
    fun letGo() {
        touching = false
        main.removeCallbacks(lookBack)
        main.postDelayed(lookBack, LOOK_BACK_MS)
    }

    private val lookBack = Runnable {
        wantX = 0f
        wantY = 0f
        wake()
    }

    /** Where her eyes are on screen, in this view's pixels. */
    private fun faceY() = (height - side) / 2f + side * (PeriLayers.CALM_Y / PeriLayerData.SIZE)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressTarget = 1f
                parent?.requestDisallowInterceptTouchEvent(true)
                follow(event.rawX, event.rawY)
            }
            MotionEvent.ACTION_MOVE -> follow(event.rawX, event.rawY)
            MotionEvent.ACTION_UP -> {
                pressTarget = 0f
                letGo()
                if (event.eventTime - event.downTime < TAP_MS) performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressTarget = 0f
                letGo()
            }
        }
        return true
    }

    /** Touched (or chosen with TalkBack), she laughs. */
    override fun performClick(): Boolean {
        express(Look.LAUGH)
        return super.performClick()
    }

    private fun hop(height: Float = 1f) {
        if (!moves) return
        hopStart = SystemClock.uptimeMillis()
        hopHeight = height
        wake()
    }

    // ----- What's on show -----

    private fun eyes(): String {
        blinkEyes?.let { return it }
        momentEyes?.let { return it }
        look?.let { return it.eyes }
        // Looking well up or down, her eyes say so; to the sides, they move.
        return when {
            gazeY < -GAZE_TURN -> "up"
            gazeY > GAZE_TURN -> "down"
            else -> "open"
        }
    }

    private fun mouthShape(): String = word ?: momentMouth ?: look?.mouth ?: "smile"

    private fun refresh() {
        val next = gesture ?: look?.body ?: "calm"
        if (next != bodyShown) {
            bodyShown = next
            if (moves) snapStart = SystemClock.uptimeMillis()
        }
        wake()
    }

    // ----- Pictures -----

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        side = min(w, h).toFloat()
        feetY = (h - side) / 2 + side * FEET
        shadow.shader = if (side > 1f) RadialGradient(0f, 0f, side * SHADOW_SIZE, Palette.SHADOW, 0x00000000, Shader.TileMode.CLAMP) else null
        // Decoded for her size, as PeriFace's frames: a clearly different size decodes them again.
        val target = if (side <= 0f) 0 else min(1024, ((side * 1.12f).toInt() + 127) / 128 * 128)
        if (target > decodedSide || target < decodedSide * 3 / 4) {
            decodedSide = target
            pictures.clear()
            if (target > 0) decodeAll(target)
        }
    }

    /**
     * The pieces she uses, in the background, the calm body and face first, so she shows up at once. Only her own bodies:
     * all of them would be some 150 MB.
     */
    private fun decodeAll(target: Int) {
        val bodies = BODIES
        val all = bodies.mapNotNull { b -> PeriLayerData.BODIES[b]?.let { "body_$b" to it.first } } +
            bodies.mapNotNull { b -> PeriLayerData.FRONTS[b]?.let { "front_$b" to it.first } } +
            PeriLayerData.EYES.map { (name, id) -> "eyes_$name" to id } +
            PeriLayerData.MOUTHS.map { (name, id) -> "mouth_$name" to id }
        val ordered = all.sortedBy { (key, _) -> if (key in FIRST) 0 else 1 }
        DECODER.execute {
            ordered.forEach { (key, id) ->
                val bitmap = runCatching { PeriLayers.bitmap(context.resources, id, target) }.getOrNull() ?: return@forEach
                main.post {
                    if (target == decodedSide) {
                        pictures[key] = bitmap
                        if (key in FIRST || key.endsWith(bodyShown)) invalidate()
                    }
                }
            }
        }
    }

    private fun piece(kind: String, name: String): Bitmap? = pictures["${kind}_$name"] ?: pictures[if (kind == "eyes") "eyes_open" else "mouth_smile"]

    // ----- Drawing -----

    override fun dispatchDraw(canvas: Canvas) {
        val body = pictures["body_$bodyShown"] ?: pictures["body_calm"]
        if (body != null && side > 0f) drawPeri(canvas, body)
        super.dispatchDraw(canvas)
    }

    private fun drawPeri(canvas: Canvas, body: Bitmap) {
        // Her shadow on the ground, smaller and lighter as she rises.
        if (shadow.shader != null) {
            val rise = (lift / (side * 0.12f)).coerceIn(0f, 1f)
            val s = 1 - rise * 0.22f
            canvas.save()
            canvas.translate(width / 2f + shift, feetY)
            canvas.scale(s, s * SHADOW_FLATNESS)
            shadow.alpha = (255 * (1 - rise * 0.35f)).toInt()
            canvas.drawCircle(0f, 0f, side * SHADOW_SIZE, shadow)
            canvas.restore()
        }
        val k = side / PeriLayerData.SIZE
        canvas.save()
        canvas.translate(width / 2f + shift, feetY - lift)
        canvas.rotate(tilt)
        canvas.scale(stretchX * k, stretchY * k)
        canvas.translate(-PeriLayerData.SIZE / 2, -PeriLayerData.SIZE * FEET)
        // One body at a time (two, one fading over the other, showed two pairs of arms).
        PeriLayers.draw(canvas, body, PeriLayers.WHOLE, paint, rect)
        // Her face where this body's picture has its eyes, turned a little towards what she looks at; then what she
        // holds in front of it (her hands making a heart), so her mouth never rubs half of it out.
        PeriLayers.drawFace(canvas, PeriLayers.place(bodyShown), piece("eyes", eyes()), piece("mouth", mouthShape()), gazeX, gazeY, gazeX, gazeY, paint, rect)
        PeriLayerData.FRONTS[bodyShown]?.let { (_, box) -> pictures["front_$bodyShown"]?.let { PeriLayers.draw(canvas, it, box, paint, rect) } }
        canvas.restore()
    }

    // ----- Motion -----

    private fun step(now: Long) {
        val dt = (now - lastStep).coerceIn(0L, 50L) / 1000f
        lastStep = now
        // Eyes and face follow where she wants to look, a little bouncy; a press squishes and springs back.
        gazeVX += (GAZE_K * (wantX - gazeX) - GAZE_D * gazeVX) * dt
        gazeVY += (GAZE_K * (wantY - gazeY) - GAZE_D * gazeVY) * dt
        gazeX += gazeVX * dt
        gazeY += gazeVY * dt
        pressV += (PRESS_K * (pressTarget - press) - PRESS_D * pressV) * dt
        press += pressV * dt
        sag += ((if (look == Look.SAD) 1f else 0f) - sag) * min(1f, dt * 4f)
        val breath = sin(2 * PI * now / BREATH_MS).toFloat()
        val sway = sin(2 * PI * now / SWAY_MS).toFloat()
        // A hop: crouch, up stretched, down, and a squash as she lands.
        var squash = -press * 0.06f
        var up = 0f
        val p = (now - hopStart) / HOP_MS.toFloat()
        if (p in 0f..1f) when {
            p < 0.16f -> squash -= 0.06f * sin(PI * p / 0.16f).toFloat() * hopHeight
            p < 0.78f -> {
                val q = (p - 0.16f) / 0.62f
                up = sin(PI * q).toFloat() * side * 0.10f * hopHeight
                squash += 0.045f * abs(cos(PI * q).toFloat()) * hopHeight
            }
            else -> squash -= 0.08f * sin(PI * (p - 0.78f) / 0.22f).toFloat() * hopHeight
        }
        // A little nod with each word.
        val n = (now - nodStart) / NOD_MS.toFloat()
        val nod = if (n in 0f..1f) sin(PI * n).toFloat() else 0f
        // A new body: a quick squash and back.
        val sn = (now - snapStart) / SNAP_MS.toFloat()
        if (sn in 0f..1f) squash -= SNAP * sin(PI * sn).toFloat()
        // A shiver when worried.
        val sh = (now - shiverStart) / SHIVER_MS.toFloat()
        val shiver = if (sh in 0f..1f) sin(2 * PI * (now - shiverStart) / 90.0).toFloat() * side * 0.006f * (1 - sh) else 0f
        lift = up
        stretchY = 1 + breath * 0.012f + squash - nod * 0.012f - sag * 0.03f
        stretchX = 1 - breath * 0.006f - squash * 0.6f + sag * 0.012f
        tilt = sway * 1.2f + gazeX * 2f + nod * 1.1f
        shift = sway * side * 0.004f + shiver
    }

    /** Anything moving beyond her breathing: then every frame, otherwise the calm frame rate. */
    private fun lively(now: Long): Boolean =
        talking || touching || now - snapStart < SNAP_MS || now - hopStart < HOP_MS || now - nodStart < NOD_MS || now - shiverStart < SHIVER_MS ||
            abs(wantX - gazeX) + abs(wantY - gazeY) > 0.004f || abs(gazeVX) + abs(gazeVY) > 0.01f ||
            abs(pressTarget - press) > 0.004f || abs(pressV) > 0.01f

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            if (!lively(now)) {
                val slot = Rest.slot(frameTimeNanos, Rest.calmFrameMs(now))
                if (slot == lastSlot) {
                    Choreographer.getInstance().postFrameCallback(this)
                    return
                }
                lastSlot = slot
            }
            step(now)
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Something changed: draw it; with motion on, the frame loop does (and runs every frame while lively). */
    private fun wake() {
        if (!moves) invalidate()
    }

    private val blinker = object : Runnable {
        override fun run() {
            if (!talking) blink()
            main.postDelayed(this, Random.nextLong(3_500, 6_500))
        }
    }

    /** Now and then, if nothing else is going on, a little something of her own. */
    private val idle = object : Runnable {
        override fun run() {
            if (!talking && !waving && !touching && look == null && momentEyes == null && momentMouth == null) when (Random.nextInt(7)) {
                0, 1 -> glance()
                2 -> {
                    wantY = -0.9f
                    main.removeCallbacks(lookBack)
                    main.postDelayed(lookBack, 1_200)
                }
                3 -> moment("happy", "grin", 1_300)
                4 -> moment(null, "whistle", 1_800)
                5 -> moment("wink_left", "tongue", 700)
                else -> wave()
            }
            main.postDelayed(this, Random.nextLong(6_000, 11_000))
        }
    }

    private fun glance() {
        wantX = if (Random.nextBoolean()) 0.85f else -0.85f
        wantY = Random.nextDouble(-0.15, 0.25).toFloat()
        main.removeCallbacks(lookBack)
        main.postDelayed(lookBack, Random.nextLong(1_000, 1_600))
    }

    private fun start() {
        if (running || !moves || !isAttachedToWindow || windowVisibility != VISIBLE) return
        running = true
        lastStep = SystemClock.uptimeMillis()
        Choreographer.getInstance().postFrameCallback(frame)
        main.postDelayed(blinker, Random.nextLong(1_500, 3_000))
        main.postDelayed(idle, Random.nextLong(4_000, 7_000))
    }

    private fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
        main.removeCallbacks(blinker)
        main.removeCallbacks(idle)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        main.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    private companion object {
        val DECODER = Executors.newSingleThreadExecutor()
        val FIRST = setOf("body_calm", "eyes_open", "mouth_smile")
        /** Her mouth shapes while talking (the open ones twice: they're most of speech); the closed ones go quicker. */
        val VISEMES = listOf("talk", "wide", "o", "ee", "u", "m", "l", "f", "talk", "wide", "ee")
        val CLOSED = setOf("m", "f")
        val GESTURES = listOf("calm_gesturea", "calm_gestureb")
        val WAVES = listOf("calm_wave1", "calm_wave2", "calm_wave1", "calm_wave2", "calm_wave1", "calm_wave2")
        /** The bodies she uses: calm, talking with her hands, waving, and those of her looks. */
        val BODIES = setOf("calm") + GESTURES + WAVES + Look.entries.mapNotNull { it.body }
        /** Eyes she blinks with: the others are closed already, or have brows, hearts or stars. */
        val BLINKING = setOf("open", "up", "down", "teary")

        /** As in PeriFace: her feet on the picture, and her shadow's size and flatness. */
        const val FEET = 0.885f
        const val SHADOW_SIZE = 0.22f
        const val SHADOW_FLATNESS = 0.16f
        /** Looking further up or down than this, her eyes change. */
        const val GAZE_TURN = 0.6f
        /** How far from her eyes a finger is "all the way" to a side, in her sizes. */
        const val REACH = 0.6f
        const val GAZE_K = 90f
        const val GAZE_D = 13f
        const val PRESS_K = 260f
        const val PRESS_D = 14f
        const val BREATH_MS = 3_200.0
        const val SWAY_MS = 5_200.0
        const val HOP_MS = 640L
        const val NOD_MS = 220L
        const val SHIVER_MS = 600L
        const val SNAP_MS = 140L
        const val SNAP = 0.025f
        const val WAVE_STEP_MS = 260L
        const val YAWN_MS = 1_500L
        const val LAUGH_HOP_MS = 520L
        const val LAUGH_FROM_MS = 160L
        const val LAUGH_STEP_MS = 150L
        const val LOOK_BACK_MS = 700L
        const val TAP_MS = 350L
    }
}
