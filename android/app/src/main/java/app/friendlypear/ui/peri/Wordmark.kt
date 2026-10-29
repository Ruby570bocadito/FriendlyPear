package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import android.os.SystemClock
import android.util.TypedValue
import android.view.Choreographer
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The name as a logo: round Fredoka letters in two pear greens, with a dark clay edge and a soft shadow, and a leaf for
 * the dot of the "i". The letters drop in one by one with a bounce, now and then a little wave runs through them, and a
 * tap makes them hop. It only draws while something moves; at night the shadow turns into a soft light glow.
 */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal class Wordmark(context: Context, private val name: String) : View(context) {
    /** Night sky behind it. */
    var dark = false
        set(value) {
            field = value
            shadowFor()
            invalidate()
        }

    /** False while Peri warns: no waves then. */
    var playful = true

    /** Whether the name fits at a readable size; if not, the round logo stands in for it. */
    val fits: Boolean get() = size >= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MIN_SP, resources.displayMetrics)

    private val moves = context.animationsOn()
    private val d = resources.displayMetrics.density
    private val split = name.indexOf("Pear").takeIf { it > 0 } ?: name.length
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Fonts.display(700) }
    private val probe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.display(700)
        textSize = 100f
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Fonts.display(700)
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = EDGE
    }
    private val leaf = Paint(Paint.ANTI_ALIAS_FLAG)
    private val leafPath = Path()
    private var size = 0f
    private val x = FloatArray(name.length)
    private val widths = FloatArray(name.length)
    private var baseline = 0f
    private var top = 0f
    private var dotX = 0f
    private var dotY = 0f
    private var firstShade: Shader? = null
    private var secondShade: Shader? = null

    private var introAt = -1L
    private var waveAt = -1L
    private var hopAt = -1L
    private var running = false

    init {
        contentDescription = name
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isAccessibilityHeading = true
        // No grey box behind the name when focus lands on it (a keyboard, a remote): it's a logo you can tap, not a field.
        defaultFocusHighlightEnabled = false
        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            hopAt = SystemClock.uptimeMillis()
            run()
        }
        shadowFor()
    }

    private fun shadowFor() {
        if (dark) edge.setShadowLayer(4 * d, 0f, 0f, GLOW) else edge.setShadowLayer(2.5f * d, 0f, 1.5f * d, SHADOW)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val title = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TextSize.TITLE, resources.displayMetrics)
        // Measured with its own paint: measuring with the letters' own and leaving it at 100 px drew the letters bigger
        // than their dark edge after any later layout, and the name looked like one text on top of another.
        val unit = (probe.measureText(name) + (name.length - 1) * 100f * TRACKING) / 100f
        // Room for the edge and the hops on both sides; a little smaller than a title, so it doesn't crowd the screen.
        size = if (width > 0) min(title * SCALE, (width - 8 * d) / unit) else title * SCALE
        val height = (size * 1.55f + 10 * d).toInt()
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fill.textSize = size
        edge.textSize = size
        edge.strokeWidth = size * EDGE_WIDTH
        // A little air between the letters: with their thick clay edge, side by side they ran into each other.
        var at = 4 * d
        for (i in name.indices) {
            x[i] = at
            widths[i] = fill.measureText(name, i, i + 1)
            at += widths[i] + size * TRACKING
        }
        val bounds = Rect()
        fill.getTextBounds("F", 0, 1, bounds)
        baseline = h / 2f - bounds.exactCenterY() + 2 * d
        top = baseline + bounds.top
        firstShade = LinearGradient(0f, top, 0f, baseline, FIRST_TOP, FIRST_BOTTOM, Shader.TileMode.CLAMP)
        secondShade = LinearGradient(0f, top, 0f, baseline, SECOND_TOP, SECOND_BOTTOM, Shader.TileMode.CLAMP)
        // The leaf sits on the "i": where its dot is.
        val i = name.indexOf('i')
        if (i >= 0) {
            fill.getTextBounds("i", 0, 1, bounds)
            dotX = x[i] + fill.measureText("i") / 2f
            dotY = baseline + bounds.top + size * 0.07f
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!moves) return
        introAt = SystemClock.uptimeMillis()
        run()
        scheduleWave()
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(wave)
        Choreographer.getInstance().removeFrameCallback(frame)
        super.onDetachedFromWindow()
    }

    private val wave = Runnable {
        if (playful && isShown) {
            waveAt = SystemClock.uptimeMillis()
            run()
        }
        scheduleWave()
    }

    private fun scheduleWave() {
        removeCallbacks(wave)
        if (moves) postDelayed(wave, Random.nextLong(WAVE_EVERY_MIN, WAVE_EVERY_MAX))
    }

    private fun run() {
        if (running || !moves) return invalidate()
        running = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            invalidate()
            val now = SystemClock.uptimeMillis()
            val busy = now - introAt < INTRO_MS + name.length * STAGGER_MS || now - waveAt < WAVE_MS || now - hopAt < HOP_MS
            if (busy) Choreographer.getInstance().postFrameCallback(this) else running = false
        }
    }

    /** How high letter [i] is lifted (px, up is negative), and how it squashes as it lands. */
    private fun motion(i: Int, now: Long, out: FloatArray) {
        var lift = 0f
        var squash = 0f
        if (introAt >= 0) {
            val t = (now - introAt - i * STAGGER_MS) / INTRO_MS.toFloat()
            when {
                t < 0f -> lift = -size * 1.2f
                t < 1f -> {
                    // Dropping from above, with a bounce that settles.
                    lift = -size * 1.2f * exp(-5f * t) * kotlin.math.abs(kotlin.math.cos(t * PI.toFloat() * 1.6f))
                    squash = 0.12f * exp(-4f * t) * sin(t * PI.toFloat() * 3.2f)
                }
            }
        }
        if (waveAt >= 0) {
            val t = (now - waveAt - i * 60) / 420f
            if (t in 0f..1f) lift -= size * 0.16f * sin(t * PI.toFloat())
        }
        if (hopAt >= 0) {
            val t = (now - hopAt - i * 25) / 360f
            if (t in 0f..1f) {
                lift -= size * 0.3f * sin(t * PI.toFloat())
                squash += 0.06f * sin(t * PI.toFloat() * 2)
            }
        }
        out[0] = lift
        out[1] = squash
    }

    private val m = FloatArray(2)
    private val lifts = FloatArray(name.length)
    private val squashes = FloatArray(name.length)

    /**
     * Every letter's edge first, then every letter, then the light on each: drawn letter by letter, an edge covered
     * the letter before it, and the name looked like one text on top of another.
     */
    override fun onDraw(canvas: Canvas) {
        if (size <= 0f) return
        val now = SystemClock.uptimeMillis()
        for (i in name.indices) {
            motion(i, now, m)
            lifts[i] = m[0]
            squashes[i] = m[1]
        }
        for (pass in 0..2) for (i in name.indices) {
            canvas.save()
            canvas.translate(0f, lifts[i])
            canvas.scale(1f + squashes[i], 1f - squashes[i], x[i] + widths[i] / 2, baseline)
            when (pass) {
                0 -> canvas.drawText(name, i, i + 1, x[i], baseline, edge)
                1 -> {
                    fill.shader = if (i < split) firstShade else secondShade
                    canvas.drawText(name, i, i + 1, x[i], baseline, fill)
                    fill.shader = null
                }
                // A soft light on the top of each letter, like light on plasticine: the letter itself, clipped.
                else -> {
                    fill.color = HIGHLIGHT
                    canvas.clipRect(x[i] - size, top - size, x[i] + widths[i] + size, top + (baseline - top) * LIGHT)
                    canvas.drawText(name, i, i + 1, x[i], baseline, fill)
                    fill.color = 0xFF000000.toInt()
                }
            }
            canvas.restore()
        }
        val dot = name.indexOf('i')
        if (dot >= 0) drawLeaf(canvas, lifts[dot])
    }

    /** A pear leaf for the dot of the "i", tilted, with its vein. */
    private fun drawLeaf(canvas: Canvas, lift: Float) {
        val l = size * 0.30f
        canvas.save()
        canvas.translate(dotX, dotY + lift)
        canvas.rotate(-32f + if (moves && waveAt >= 0) 8f * sin((SystemClock.uptimeMillis() - waveAt) / 90f) * max(0f, 1f - (SystemClock.uptimeMillis() - waveAt) / WAVE_MS.toFloat()) else 0f)
        leafPath.reset()
        leafPath.moveTo(-l * 0.5f, 0f)
        leafPath.quadTo(0f, -l * 0.42f, l * 0.5f, 0f)
        leafPath.quadTo(0f, l * 0.42f, -l * 0.5f, 0f)
        leaf.style = Paint.Style.FILL
        leaf.color = EDGE
        canvas.save()
        canvas.scale(1.22f, 1.3f)
        canvas.drawPath(leafPath, leaf)
        canvas.restore()
        leaf.color = LEAF
        canvas.drawPath(leafPath, leaf)
        leaf.style = Paint.Style.STROKE
        leaf.strokeWidth = size * 0.025f
        leaf.color = LEAF_VEIN
        canvas.drawLine(-l * 0.36f, 0f, l * 0.36f, 0f, leaf)
        canvas.restore()
    }

    private companion object {
        const val MIN_SP = 17f
        /** Its size against a title's, and the air between letters as a share of their size. */
        const val SCALE = 0.82f
        const val TRACKING = 0.07f
        const val INTRO_MS = 760L
        const val STAGGER_MS = 55L
        const val WAVE_MS = 900L
        const val HOP_MS = 600L
        const val WAVE_EVERY_MIN = 9_000L
        const val WAVE_EVERY_MAX = 16_000L
        /** The dark clay edge around the letters, as a share of their size; and how far down the light reaches. */
        const val EDGE_WIDTH = 0.10f
        const val LIGHT = 0.36f
        const val EDGE = 0xFF1B4612.toInt()
        const val SHADOW = 0x55173A10
        const val GLOW = 0x88FFF8EC.toInt()
        const val HIGHLIGHT = 0x33FFFFFF
        const val FIRST_TOP = 0xFF6CC03A.toInt()
        const val FIRST_BOTTOM = 0xFF2F7A1E.toInt()
        const val SECOND_TOP = 0xFFC6E86A.toInt()
        const val SECOND_BOTTOM = 0xFF7CC242.toInt()
        const val LEAF = 0xFF5E9E40.toInt()
        const val LEAF_VEIN = 0xFF3E7A2E.toInt()
    }
}
