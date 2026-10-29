package app.friendlypear.ui.peri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Choreographer
import android.view.MotionEvent
import android.widget.FrameLayout
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.services.PeriMoment
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mini Peri as only her head (lote 5), with Peri's own eyes and mouth on it: the layer pieces, placed where her cheeks
 * say ([MiniHeadData]). Small over other apps, a whole Peri's face was too small to read. She breathes and sways,
 * blinks, talks with her mouth, her leaf moves in the wind now and then, she waves hello, holds her umbrella while she
 * protects and sleeps in her cap at night; her moments (a laugh, love, an idea) show on her face with their clay effect.
 * With "remove animations" on, her face still changes, without motion.
 */
class MiniHead(context: Context) : FrameLayout(context) {
    /** A face for a while: eyes, mouth, perhaps a head of her own, and its effect. */
    private class Face(val eyes: String, val mouth: String, val head: String? = null, val effect: Sparkles.Effect? = null)

    private val moves = context.animationsOn()
    private val main = Handler(Looper.getMainLooper())
    private val sparkles = Sparkles(context)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()
    private val pictures = HashMap<String, Bitmap>()
    private var side = 0
    private var decodedSide = 0

    private var mood: Mood? = null
    private var base = MOODS.getValue(Mood.CALM)
    private var moment: Face? = null
    private var gust: String? = null
    private var blinkEyes: String? = null
    private var word: String? = null
    private var talking = false

    private var hopStart = -HOP_MS
    private var press = 0f
    private var pressTarget = 0f
    private var running = false
    private var lastSlot = -1L

    init {
        addView(sparkles, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        setWillNotDraw(false)
        clipChildren = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    /** Her mood's head and face; a new mood hops in with its sign (a drop, a "?", a "!"). */
    fun show(next: Mood) {
        if (next == mood) return
        val first = mood == null
        mood = next
        base = MOODS.getValue(next)
        contentDescription = context.getString(DESCRIPTIONS.getValue(next))
        if (moves && !first) {
            hop()
            base.effect?.let { sparkles.play(it) }
        }
        invalidate()
    }

    /** Her mouth moves while her voice plays. */
    fun speaking(on: Boolean) {
        if (on == talking) return
        talking = on
        main.removeCallbacks(mouth)
        if (on && moves) main.post(mouth) else {
            word = null
            invalidate()
        }
    }

    /** One of Peri's moments, on her face for a while with its clay effect. */
    fun moment(m: PeriMoment) {
        val face = MOMENTS[m] ?: Face("happy", "grin", effect = Sparkles.Effect.SPARKLE)
        moment = face
        if (moves) {
            hop()
            face.effect?.let { sparkles.play(it) }
        }
        main.removeCallbacks(endMoment)
        main.postDelayed(endMoment, MOMENT_MS)
        invalidate()
    }

    private val endMoment = Runnable {
        moment = null
        invalidate()
    }

    /** Hello! Her hand up beside her, smiling with her eyes. */
    fun wave() {
        if (!moves) return
        moment = Face("happy", "grin", head = "wave")
        hop()
        main.removeCallbacks(endMoment)
        main.postDelayed(endMoment, WAVE_MS)
        invalidate()
    }

    private fun hop() {
        if (moves) hopStart = SystemClock.uptimeMillis()
    }

    private fun head(): String = moment?.head ?: gust ?: base.head ?: "noface"

    private fun eyes(): String = blinkEyes ?: moment?.eyes ?: base.eyes

    private fun mouthShape(): String = word ?: moment?.mouth ?: base.mouth

    private val mouth = object : Runnable {
        override fun run() {
            if (!talking) return
            word = VISEMES.filter { it != word }.random()
            invalidate()
            main.postDelayed(this, if (word == "m") Random.nextLong(70, 120) else Random.nextLong(95, 170))
        }
    }

    private val blinker = object : Runnable {
        override fun run() {
            if (moves && !talking && eyes() in BLINKING) {
                listOf("half" to 0L, "closed" to 45L, "half" to 125L, null to 170L).forEach { (e, at) ->
                    main.postDelayed({
                        blinkEyes = e
                        invalidate()
                    }, at)
                }
            }
            main.postDelayed(this, Random.nextLong(3_500, 6_500))
        }
    }

    /** Now and then, calm, a gust: her leaf blows one way, settles and blows the other. */
    private val wind = object : Runnable {
        override fun run() {
            if (moves && !talking && moment == null && base.head == null) {
                listOf("leaf_left" to 0L, null to 320L, "leaf_right" to 560L, null to 880L).forEach { (h, at) ->
                    main.postDelayed({
                        gust = h
                        invalidate()
                    }, at)
                }
            }
            main.postDelayed(this, Random.nextLong(7_000, 12_000))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        side = min(w, h)
        if (side > 0 && side != decodedSide) {
            decodedSide = side
            decodeAll(side)
        }
    }

    /** Her heads at her size and the face pieces at the size her face is drawn, in the background. */
    private fun decodeAll(target: Int) {
        val heads = MiniHeadData.HEADS.map { (name, data) -> "head_$name" to data.first }
        val faceSide = (PeriLayerData.SIZE * MiniHeadData.HEADS.getValue("noface").second[3] * target * 1.15f).toInt().coerceAtLeast(16)
        val pieces = EYES.mapNotNull { e -> PeriLayerData.EYES[e]?.let { "eyes_$e" to it } } +
            MOUTHS.mapNotNull { m -> PeriLayerData.MOUTHS[m]?.let { "mouth_$m" to it } }
        DECODER.execute {
            val ready = heads.mapNotNull { (key, id) -> runCatching { key to head(id, target) }.getOrNull() } +
                pieces.mapNotNull { (key, id) -> runCatching { key to PeriLayers.bitmap(resources, id, faceSide) }.getOrNull() }
            main.post {
                if (target == decodedSide) {
                    pictures.putAll(ready)
                    invalidate()
                }
            }
        }
    }

    private fun head(id: Int, target: Int): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, id)) { decoder, _, _ -> decoder.setTargetSize(target, target) }

    override fun dispatchDraw(canvas: Canvas) {
        val name = head()
        val picture = pictures["head_$name"] ?: pictures["head_noface"]
        if (picture != null && side > 0) drawHead(canvas, picture, if (pictures.containsKey("head_$name")) name else "noface")
        super.dispatchDraw(canvas)
    }

    private fun drawHead(canvas: Canvas, picture: Bitmap, name: String) {
        val now = SystemClock.uptimeMillis()
        val s = side.toFloat()
        // Breathing and a gentle sway; a hop crouches, rises and lands; a press squishes her like clay.
        val breath = if (moves) sin(2 * PI * now / BREATH_MS).toFloat() else 0f
        val sway = if (moves) sin(2 * PI * now / SWAY_MS).toFloat() else 0f
        val p = (now - hopStart) / HOP_MS.toFloat()
        val lift = if (p in 0f..1f) sin(PI * p).toFloat() * s * 0.08f else 0f
        val squash = if (p in 0f..1f) -0.05f * sin(PI * p * 2).toFloat() else 0f
        canvas.save()
        canvas.translate(width / 2f, (height + s) / 2f - lift)
        canvas.rotate(sway * 2f)
        canvas.scale(1 - breath * 0.008f - squash * 0.6f + press * 0.04f, 1 + breath * 0.016f + squash - press * 0.07f)
        canvas.translate(-s / 2f, -s)
        rect.set(0f, 0f, s, s)
        canvas.drawBitmap(picture, null, rect, paint)
        // Her face where this head's cheeks say.
        val place = MiniHeadData.HEADS.getValue(name).second
        canvas.translate(place[0] * s, place[1] * s)
        canvas.rotate(place[2])
        canvas.scale(place[3] * s, place[3] * s)
        canvas.translate(-MiniHeadData.CALM_CHEEKS[0], -MiniHeadData.CALM_CHEEKS[1])
        (pictures["eyes_${eyes()}"] ?: pictures["eyes_open"])?.let { PeriLayers.draw(canvas, it, PeriLayerData.EYES_BOX, paint, rect) }
        (pictures["mouth_${mouthShape()}"] ?: pictures["mouth_smile"])?.let { PeriLayers.draw(canvas, it, PeriLayerData.MOUTH_BOX, paint, rect) }
        canvas.restore()
    }

    /** Mini Peri handles touches itself (tap, drag, long press) and reports a tap here, for accessibility. */
    override fun performClick(): Boolean = super.performClick()

    /** Pressed, she squishes; let go, she springs back. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> pressTarget = 1f
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> pressTarget = 0f
        }
        return super.dispatchTouchEvent(ev)
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            press += (pressTarget - press) * 0.35f
            val lively = talking || now - hopStart < HOP_MS || kotlin.math.abs(pressTarget - press) > 0.01f
            if (!lively) {
                val slot = Rest.slot(frameTimeNanos, Rest.calmFrameMs(now))
                if (slot == lastSlot) {
                    Choreographer.getInstance().postFrameCallback(this)
                    return
                }
                lastSlot = slot
            }
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun start() {
        if (running || !moves || !isAttachedToWindow || windowVisibility != VISIBLE) return
        running = true
        Choreographer.getInstance().postFrameCallback(frame)
        main.postDelayed(blinker, Random.nextLong(1_500, 3_000))
        main.postDelayed(wind, Random.nextLong(4_000, 8_000))
    }

    private fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
        main.removeCallbacks(blinker)
        main.removeCallbacks(wind)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
        // Each time she appears, a little wave hello.
        main.postDelayed({ wave() }, WAVE_DELAY_MS)
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
        /** Each mood's head and face, and the sign it comes in with. */
        val MOODS = mapOf(
            Mood.CALM to Face("open", "smile"),
            Mood.LISTENING to Face("wide", "smile"),
            Mood.THINKING to Face("up", "m", effect = Sparkles.Effect.QUESTION),
            Mood.WORRIED to Face("worried", "worried", effect = Sparkles.Effect.SWEAT),
            Mood.PROTECTIVE to Face("brave", "smile", head = "umbrella", effect = Sparkles.Effect.ALARM),
            Mood.CALLING to Face("open", "smile"),
            Mood.WAITING to Face("open", "smile"),
            Mood.HAPPY to Face("happy", "grin", effect = Sparkles.Effect.SPARKLE),
            Mood.SLEEPING to Face("closed", "smile", head = "sleep", effect = Sparkles.Effect.SLEEPY),
        )
        val MOMENTS = mapOf(
            PeriMoment.LAUGH to Face("laugh", "laugh", effect = Sparkles.Effect.SPARKLE),
            PeriMoment.LOVE to Face("love", "kiss", effect = Sparkles.Effect.HEARTS),
            PeriMoment.SURPRISED to Face("wide", "surprised", effect = Sparkles.Effect.ALARM),
            PeriMoment.IDEA to Face("wide", "o", effect = Sparkles.Effect.IDEA),
            PeriMoment.YAWN to Face("sleepy", "yawn", effect = Sparkles.Effect.SLEEPY),
            PeriMoment.SORRY to Face("worried", "sad", effect = Sparkles.Effect.SWEAT),
            PeriMoment.PARTY to Face("happy", "grin", effect = Sparkles.Effect.CONFETTI),
            PeriMoment.MEDAL to Face("happy", "grin", effect = Sparkles.Effect.CONFETTI),
            PeriMoment.DETECTIVE to Face("up", "m", effect = Sparkles.Effect.QUESTION),
            PeriMoment.BYE to Face("happy", "grin", head = "wave", effect = Sparkles.Effect.HEARTS),
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
        /** The pieces she uses; the closed ones go quicker when she talks. */
        val EYES = listOf("open", "half", "closed", "happy", "laugh", "wide", "worried", "love", "up", "sleepy", "brave")
        val MOUTHS = listOf("smile", "talk", "wide", "o", "ee", "u", "m", "grin", "laugh", "surprised", "worried", "kiss", "yawn", "sad")
        val VISEMES = listOf("talk", "wide", "o", "ee", "u", "m", "talk", "wide")
        val BLINKING = setOf("open", "wide", "up")
        const val HOP_MS = 520L
        const val MOMENT_MS = 2_400L
        const val WAVE_MS = 1_400L
        const val WAVE_DELAY_MS = 600L
        const val BREATH_MS = 3_000.0
        const val SWAY_MS = 4_600.0
    }
}
