package app.friendlypear.ui.peri

import app.friendlypear.R
import android.graphics.LightingColorFilter
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import app.friendlypear.core.agent.Mood
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Peri's world, behind every section of her screen. The sky follows the hour (dawn, day, dusk, night) and Peri's
 * mood: a soft coral veil and everything still while she warns, confetti when she's happy, night while she sleeps.
 * The sun (a ball of warm clay with slow rays) or the moon floats in it, clouds drift by, stars twinkle at night. On
 * the ground, a place that changes every day with the season (a meadow, an orchard, the seaside, autumn hills, snow),
 * each with its own little life: petals, a butterfly, a boat, falling leaves, a snowman, fireflies at night. Far
 * away, hills in the haze, snowy mountains or a lighthouse whose beam turns at night. Everything is lit like
 * plasticine, from the top left like Peri, and sits on a soft shadow. Leaning the phone moves the far world a little
 * ([tilt]). On Peri's section the hills rise to her feet ([setGround]); elsewhere they sink to make room. Decorative:
 * TalkBack skips it, and with "remove animations" it's drawn once, still. It draws at most 30 times a second, stops
 * when it can't be seen, and keeps its shapes and gradients until the size, the place or the hour changes them.
 */
class PeriWorld(context: Context) : View(context) {
    enum class Phase { DAWN, DAY, DUSK, NIGHT }

    /** [VILLAGE]: Peri's village, its cottage, trees, bushes and clouds made with Muse Image (the others are drawn in code). */
    enum class Place { MEADOW, ORCHARD, SEASIDE, AUTUMN, SNOW, VILLAGE }

    private class Cloud(var x: Float, val y: Float, val scale: Float, val speed: Float)

    private class Bit(var x: Float, var y: Float, var vx: Float, var vy: Float, val size: Float, val color: Int, var spin: Float, val spinSpeed: Float, val seed: Float)

    private val d = resources.displayMetrics.density
    private val moves = context.animationsOn()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private val cloud = Path()
    private val oval = RectF()
    private val skyMatrix = Matrix()
    private var skyShader: LinearGradient? = null
    private var glow: RadialGradient? = null
    private var moonGlow: RadialGradient? = null

    private var phase = phaseAt(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    private var sky = SKIES.getValue(phase).copyOf()
    private var skyFrom = sky.copyOf()
    private var skyStart = 0L
    /** 0 by day, 1 at night: the stars and the moon. */
    private var night = if (phase == Phase.NIGHT) 1f else 0f
    /** How the hour lights the ground, moving from one phase's light to the next's. */
    private var tintFrom = TINTS.getValue(phase)
    private var tintTo = tintFrom
    private var tintMix = 1f
    private var lastDark = phase == Phase.NIGHT
    private var veilShader: LinearGradient? = null

    var place: Place = Place.MEADOW
        private set

    private var mood = Mood.CALM
    private var asleep = false
    private var alarm = false
    private var veil = 0f

    private var ground = -1f
    private var groundTarget = -1f

    private val clouds = listOf(
        Cloud(0.05f, 0.18f, 1.25f, 7f),
        Cloud(0.62f, 0.32f, 0.9f, 10f),
        Cloud(0.36f, 0.55f, 0.7f, 13f),
        Cloud(0.88f, 0.70f, 1.05f, 8f),
    )
    private val stars = List(38) { floatArrayOf(Random.nextFloat(), 0.03f + Random.nextFloat() * 0.85f, 0.8f + Random.nextFloat() * 1.6f, Random.nextFloat() * 6.3f, 0.6f + Random.nextFloat() * 1.6f) }
    private val bits = ArrayList<Bit>()
    private val confetti = ArrayList<Bit>()
    private var confettiStart = 0L
    private var shooting = 0L
    private var nextShooting = 0L
    private var shootFrom = floatArrayOf(0f, 0f)

    private var running = false
    private var lastStep = 0L
    private var lastClock = 0L
    private var time = 0f
    /** How far the phone leans sideways (-1 to 1), eased: the far world drifts a little, the front hill stays. */
    private var lean = 0f
    private var leanTarget = 0f

    /** Called when the sky turns dark or light, so the titles over it can change colour. */
    var onTone: ((dark: Boolean) -> Unit)? = null
    val dark: Boolean get() = shownPhase() == Phase.NIGHT

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        place = chosenPlace(context)
        seedBits()
    }

    /** The mood shows in the sky: a coral veil while she warns, night while she sleeps, confetti when she's happy. */
    fun mood(next: Mood) {
        if (next == mood) return
        val was = mood
        mood = next
        alarm = next == Mood.WORRIED || next == Mood.PROTECTIVE
        val sleeping = next == Mood.SLEEPING
        if (sleeping != asleep) {
            asleep = sleeping
            turnTo(shownPhase())
        }
        if (next == Mood.HAPPY && was != Mood.HAPPY && moves) throwConfetti()
        if (alarm) confetti.clear()
        if (!running) invalidate()
    }

    /** Where the ground is, in this view's pixels: Peri's feet on her section, low behind the cards elsewhere. */
    fun setGround(y: Float, animate: Boolean = true) {
        groundTarget = y
        if (ground < 0f || !animate || !moves) ground = y
        if (!running) invalidate()
    }

    /** The place chosen in Settings may have changed. */
    fun refreshPlace() {
        val chosen = chosenPlace(context)
        if (chosen == place) return
        place = chosen
        seedBits()
        invalidate()
    }

    /** The phone's sideways lean, from -1 to 1 (the accelerometer's x over gravity); still with "remove animations". */
    fun tilt(x: Float) {
        if (moves) leanTarget = x.coerceIn(-1f, 1f)
    }

    private fun shownPhase(): Phase = if (asleep) Phase.NIGHT else phase

    private fun turnTo(next: Phase) {
        skyFrom = sky.copyOf()
        skyStart = SystemClock.uptimeMillis()
        tintFrom = tintTo
        tintTo = TINTS.getValue(next)
        tintMix = 0f
        if (!moves) {
            sky = SKIES.getValue(next).copyOf()
            night = if (next == Phase.NIGHT) 1f else 0f
            tintMix = 1f
            skyShader = null
        }
        if (dark != lastDark) {
            lastDark = dark
            onTone?.invoke(dark)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (ground < 0f) {
            ground = h * 0.55f
            groundTarget = ground
        }
        skyShader = null
        glow = null
        moonGlow = null
        skyGlow = null
        veilShader = null
    }

    // The frame loop: a step every FRAME_MS at most, and nothing at all when the world can't be seen.

    private val tick = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            if (now - lastStep >= FRAME_MS) {
                step(now)
                invalidate()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun start() {
        if (running || !moves || windowVisibility != VISIBLE) return
        running = true
        lastStep = SystemClock.uptimeMillis()
        Choreographer.getInstance().postFrameCallback(tick)
    }

    private fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(tick)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkClock(force = true)
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            checkClock(force = true)
            start()
        } else {
            stop()
        }
    }

    /** The hour changes the sky; looked at every half minute. */
    private fun checkClock(force: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        if (!force && now - lastClock < 30_000) return
        lastClock = now
        val next = phaseAt(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        if (next != phase) {
            phase = next
            turnTo(shownPhase())
        }
    }

    private fun step(now: Long) {
        val dt = ((now - lastStep).coerceIn(1, 100)) / 1000f
        lastStep = now
        time += dt
        checkClock()
        // The sky, the stars and the ground's light move towards the phase on show.
        val target = shownPhase()
        val a = ((now - skyStart) / SKY_MS).coerceIn(0f, 1f)
        val to = SKIES.getValue(target)
        if (a < 1f || !sky.contentEquals(to)) {
            for (i in sky.indices) sky[i] = blend(skyFrom[i], to[i], a)
            skyShader = null
        }
        tintMix = a
        night += ((if (target == Phase.NIGHT) 1f else 0f) - night) * (1 - exp(-dt * 2.5f))
        veil += ((if (alarm) 1f else 0f) - veil) * (1 - exp(-dt * 4f))
        if (groundTarget >= 0f) ground += (groundTarget - ground) * (1 - exp(-dt * 7f))
        // While Peri warns, the world holds still.
        if (alarm) return
        lean += (leanTarget - lean) * (1 - exp(-dt * 2.5f))
        clouds.forEach { c ->
            c.x += c.speed * d * dt / width.coerceAtLeast(1)
            if (c.x > 1.25f) c.x = -0.3f
        }
        moveBits(dt)
        moveConfetti(dt, now)
        if (target == Phase.NIGHT && now > nextShooting) {
            shooting = now
            nextShooting = now + Random.nextLong(12_000, 26_000)
            shootFrom = floatArrayOf(width * (0.1f + Random.nextFloat() * 0.5f), ground * (0.05f + Random.nextFloat() * 0.25f))
        }
    }

    // What lives in each place: petals, leaves, snowflakes, fireflies at night.

    private fun seedBits() {
        bits.clear()
        val count = when (place) {
            Place.SNOW -> 34
            Place.AUTUMN -> 9
            Place.MEADOW, Place.VILLAGE -> 7
            Place.ORCHARD -> 5
            Place.SEASIDE -> 0
        }
        repeat(count) { bits += newBit(Random.nextFloat()) }
    }

    private fun newBit(heightShare: Float): Bit {
        val colors = when (place) {
            Place.SNOW -> listOf(0xFFFFFFFF.toInt())
            Place.AUTUMN -> AUTUMN_COLORS.toList()
            Place.MEADOW, Place.VILLAGE -> listOf(0xFFFFFFFF.toInt(), 0xFFFFC2CF.toInt(), 0xFFFFE9A8.toInt())
            else -> listOf(0xFF9CCB72.toInt(), 0xFFC3E39E.toInt())
        }
        val snow = place == Place.SNOW
        return Bit(
            x = Random.nextFloat(),
            y = heightShare,
            vx = if (snow) 0f else 0.012f + Random.nextFloat() * 0.02f,
            vy = if (snow) 0.03f + Random.nextFloat() * 0.035f else 0.025f + Random.nextFloat() * 0.03f,
            size = if (snow) 1.4f + Random.nextFloat() * 2.2f else 3.5f + Random.nextFloat() * 2.5f,
            color = colors.random(),
            spin = Random.nextFloat() * 360f,
            spinSpeed = (Random.nextFloat() - 0.5f) * 160f,
            seed = Random.nextFloat() * 6.3f,
        )
    }

    private fun moveBits(dt: Float) {
        for (b in bits) {
            b.y += b.vy * dt
            b.x += (b.vx + 0.012f * sin(time * 1.3f + b.seed)) * dt
            b.spin += b.spinSpeed * dt
            if (b.y > 1.02f || b.x > 1.05f) {
                val fresh = newBit(-0.03f)
                b.x = if (place == Place.SNOW) Random.nextFloat() else Random.nextFloat() * 0.8f - 0.1f
                b.y = fresh.y
            }
        }
    }

    private fun throwConfetti() {
        confetti.clear()
        val colors = listOf(Palette.GREEN, Palette.SUN, Palette.CORAL, Palette.SKY, Palette.LILAC, Palette.WHITE)
        repeat(46) {
            confetti += Bit(
                x = Random.nextFloat(),
                y = -0.05f - Random.nextFloat() * 0.25f,
                vx = (Random.nextFloat() - 0.5f) * 0.08f,
                vy = 0.28f + Random.nextFloat() * 0.25f,
                size = 3f + Random.nextFloat() * 2.5f,
                color = colors.random(),
                spin = Random.nextFloat() * 360f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 720f,
                seed = Random.nextFloat() * 6.3f,
            )
        }
        confettiStart = SystemClock.uptimeMillis()
    }

    private fun moveConfetti(dt: Float, now: Long) {
        if (confetti.isEmpty()) return
        if (now - confettiStart > CONFETTI_MS) return confetti.clear()
        for (c in confetti) {
            c.x += (c.vx + 0.03f * sin(time * 4f + c.seed)) * dt
            c.y += c.vy * dt
            c.spin += c.spinSpeed * dt
        }
    }

    // Drawing, from the back to the front. The hills are shaped once for each size and place, around the ground line,
    // so a tab moving the ground only moves them; their light is a gradient kept until the hour changes its colours.

    /** A vertical gradient from [y0] to [y1] in three colours, made again only when they change. */
    private class Shade(private val y0: Float, private val y1: Float, private val stops: FloatArray) {
        private var top = 0
        private var mid = 0
        private var bottom = 0
        private var shader: LinearGradient? = null

        fun of(top: Int, mid: Int, bottom: Int): Shader {
            shader?.let { if (top == this.top && mid == this.mid && bottom == this.bottom) return it }
            this.top = top
            this.mid = mid
            this.bottom = bottom
            return LinearGradient(0f, y0, 0f, y1, intArrayOf(top, mid, bottom), stops, Shader.TileMode.CLAMP).also { shader = it }
        }
    }

    private val distant = Path()
    private val caps = Path()
    private val far = Path()
    private val farCrest = Path()
    private val front = Path()
    private val crest = Path()
    private val rays = Path()
    private val sparkle = Path()
    private val beam = Path()
    private var shapedW = 0f
    private var shapedH = 0f
    private var shapedPlace: Place? = null
    private val distantShade = Shade(-180 * d, 0f, floatArrayOf(0f, 0.5f, 1f))
    private val farShade = Shade(-84 * d, 12 * d, floatArrayOf(0f, 0.45f, 1f))
    private val frontShade = Shade(0f, 170 * d, floatArrayOf(0f, 0.3f, 1f))
    private val seaShade = Shade(0f, 90 * d, floatArrayOf(0f, 0.3f, 1f))
    /** In the cloud's own units (1 dp, before its scale): lighter on top. */
    private val cloudShade = Shade(-29f, 15f, floatArrayOf(0f, 0.55f, 1f))
    private var skyGlow: RadialGradient? = null
    private var skyGlowTone = 0
    private var sunBall: RadialGradient? = null
    private var sunBallColor = 0
    private var moonBall: RadialGradient? = null
    private var rayLight: RadialGradient? = null
    private var lampGlow: RadialGradient? = null
    private var beamLight: LinearGradient? = null
    /** Fingerprints in the front hill's clay: where (share of the width), how far under the crest (dp), how big, light or dark. */
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val spriteBox = RectF()
    private val sceneryCache = HashMap<Int, Bitmap>()
    private var lightTone = 0
    private var light = LightingColorFilter(0xFFFFFF, 0)
    private val speckles = Random(7).let { r -> List(26) { floatArrayOf(r.nextFloat(), 8f + r.nextFloat() * 100f, 1.1f + r.nextFloat() * 1.5f, if (r.nextFloat() < 0.4f) 1f else 0f) } }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val g = if (ground < 0f) h * 0.55f else ground
        shape(w, h)
        // Each part sets its own alpha; nothing carries over from the last frame (the sky would fade).
        fill.alpha = 255
        stroke.alpha = 255
        drawSky(canvas, w, h, g)
        drawStars(canvas, w, g)
        drawSunAndMoon(canvas, w, g)
        for (i in clouds.indices) drawCloud(canvas, clouds[i], w, g)
        drawDistance(canvas, w, g)
        when (place) {
            Place.SEASIDE -> drawSea(canvas, w, h, g)
            else -> drawFarHills(canvas, w, g)
        }
        drawFrontHill(canvas, w, g)
        drawPlaceLife(canvas, w, h, g)
        drawConfetti(canvas, w, h)
        drawVeil(canvas, w, h)
    }

    /** The hills for this size and place, around the ground line (y = 0), reaching past the bottom from anywhere. */
    private fun shape(w: Float, h: Float) {
        if (w == shapedW && h == shapedH && place == shapedPlace) return
        shapedW = w
        shapedH = h
        shapedPlace = place
        val down = h + 60 * d
        // A little past each side, so leaning the phone never shows a gap.
        val edge = 24 * d
        distant.reset()
        caps.reset()
        when (place) {
            Place.SEASIDE -> {
                // A green headland on the horizon, for the lighthouse.
                distant.moveTo(-edge, -52 * d)
                distant.cubicTo(w * 0.02f, -78 * d, w * 0.16f, -86 * d, w * 0.28f, -52 * d)
                distant.close()
            }
            Place.SNOW -> ridge(w, down, MOUNTAINS, snowy = true)
            else -> ridge(w, down, ROLLING, snowy = false)
        }
        far.reset()
        far.moveTo(-edge, -24 * d)
        far.lineTo(0f, -26 * d)
        far.cubicTo(w * 0.10f, -60 * d, w * 0.26f, -78 * d, w * 0.38f, -52 * d)
        far.cubicTo(w * 0.48f, -34 * d, w * 0.58f, -40 * d, w * 0.68f, -58 * d)
        far.cubicTo(w * 0.80f, -82 * d, w * 0.93f, -62 * d, w, -40 * d)
        far.lineTo(w + edge, -36 * d)
        farCrest.set(far)
        far.lineTo(w + edge, down)
        far.lineTo(-edge, down)
        far.close()
        crest.reset()
        crest.moveTo(0f, 40 * d)
        crest.cubicTo(w * 0.18f, 8 * d, w * 0.32f, 0f, w * 0.5f, 0f)
        crest.cubicTo(w * 0.68f, 0f, w * 0.82f, 6 * d, w, 34 * d)
        front.set(crest)
        front.lineTo(w, down)
        front.lineTo(0f, down)
        front.close()
    }

    /**
     * Hills far away, or mountains with snow on their peaks: one smooth line through [points] (share of the width, dp
     * above the ground; Catmull-Rom as Béziers), so the snow can follow that very line down from each peak.
     */
    private fun ridge(w: Float, down: Float, points: FloatArray, snowy: Boolean) {
        val n = points.size / 2
        fun x(i: Int) = points[2 * i.coerceIn(0, n - 1)] * w
        fun y(i: Int) = -points[2 * i.coerceIn(0, n - 1) + 1] * d
        fun c1x(i: Int) = x(i) + (x(i + 1) - x(i - 1)) / 6
        fun c1y(i: Int) = y(i) + (y(i + 1) - y(i - 1)) / 6
        fun c2x(i: Int) = x(i + 1) - (x(i + 2) - x(i)) / 6
        fun c2y(i: Int) = y(i + 1) - (y(i + 2) - y(i)) / 6
        distant.moveTo(x(0), y(0))
        for (i in 0 until n - 1) distant.cubicTo(c1x(i), c1y(i), c2x(i), c2y(i), x(i + 1), y(i + 1))
        distant.lineTo(x(n - 1), down)
        distant.lineTo(x(0), down)
        distant.close()
        if (!snowy) return
        val p = FloatArray(2)
        fun at(i: Int, t: Float) {
            p[0] = bez(t, x(i), c1x(i), c2x(i), x(i + 1))
            p[1] = bez(t, y(i), c1y(i), c2y(i), y(i + 1))
        }
        for (i in 1 until n - 1) {
            if (y(i) >= y(i - 1) || y(i) >= y(i + 1)) continue
            // The snow line: two fifths of the way down to the higher neighbour.
            val line = y(i) + 0.4f * (minOf(y(i - 1), y(i + 1)) - y(i))
            var t = 0f
            do {
                at(i - 1, t)
                t += SNOW_STEP
            } while (p[1] > line && t < 1f)
            val startX = p[0]
            caps.moveTo(p[0], p[1])
            while (t < 1f) {
                at(i - 1, t)
                caps.lineTo(p[0], p[1])
                t += SNOW_STEP
            }
            caps.lineTo(x(i), y(i))
            var endX = x(i)
            t = SNOW_STEP
            while (t <= 1f) {
                at(i, t)
                if (p[1] > line) break
                caps.lineTo(p[0], p[1])
                endX = p[0]
                t += SNOW_STEP
            }
            // A ragged edge back across, like snow on the rock.
            for (k in 1..4) caps.lineTo(endX + (startX - endX) * k / 5f, line + if (k % 2 == 1) 6 * d else -2 * d)
            caps.close()
        }
    }

    private fun drawSky(canvas: Canvas, w: Float, h: Float, g: Float) {
        val shader = skyShader ?: LinearGradient(0f, 0f, 0f, SKY_UNIT, sky, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP).also { skyShader = it }
        // The gradient reaches the horizon wherever the ground is.
        skyMatrix.setScale(1f, maxOf(g, h * 0.35f) / SKY_UNIT)
        shader.setLocalMatrix(skyMatrix)
        fill.color = 0xFFFFFFFF.toInt()
        fill.shader = shader
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
    }

    private fun drawStars(canvas: Canvas, w: Float, g: Float) {
        if (night < 0.05f) return
        if (sparkle.isEmpty) {
            sparkle.moveTo(0f, -1f)
            sparkle.quadTo(0.14f, -0.14f, 1f, 0f)
            sparkle.quadTo(0.14f, 0.14f, 0f, 1f)
            sparkle.quadTo(-0.14f, 0.14f, -1f, 0f)
            sparkle.quadTo(-0.14f, -0.14f, 0f, -1f)
            sparkle.close()
        }
        val shift = lean * LEAN_DP * d
        for (i in stars.indices) {
            val s = stars[i]
            val twinkle = 0.55f + 0.45f * sin(time * s[4] + s[3])
            fill.color = 0xFFFFF8E1.toInt()
            fill.alpha = (255 * night * twinkle).toInt().coerceIn(0, 255)
            val x = s[0] * w + shift
            val y = s[1] * g * 0.9f
            if (i < SPARKLES) {
                // The brightest few are little four-pointed sparkles.
                val size = (s[2] + 1.6f) * d * (0.8f + 0.4f * twinkle)
                canvas.save()
                canvas.translate(x, y)
                canvas.scale(size, size)
                canvas.drawPath(sparkle, fill)
                canvas.restore()
            } else {
                canvas.drawCircle(x, y, s[2] * d, fill)
            }
        }
        // Now and then, a shooting star.
        val age = (SystemClock.uptimeMillis() - shooting) / SHOOT_MS
        if (shooting > 0 && age in 0f..1f && night > 0.5f) {
            val x0 = shootFrom[0]
            val y0 = shootFrom[1]
            val len = 120 * d
            val x = x0 + age * 160 * d
            val y = y0 + age * 70 * d
            stroke.shader = LinearGradient(x, y, x - len * 0.9f, y - len * 0.4f, 0xFFFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
            stroke.strokeWidth = 2.2f * d
            stroke.alpha = (255 * (1 - age)).toInt()
            canvas.drawLine(x, y, x - len * 0.9f, y - len * 0.4f, stroke)
            stroke.shader = null
            stroke.alpha = 255
        }
    }

    private fun drawSunAndMoon(canvas: Canvas, w: Float, g: Float) {
        val x = w * 0.80f + lean * LEAN_DP * d
        val bob = if (moves) sin(time * 0.9f) * 4 * d else 0f
        val p = shownPhase()
        // High in the sky, behind the sections' first cards; lower at dawn and dusk.
        val y = (if (p == Phase.DAY || p == Phase.NIGHT) 118 else 150) * d + bob
        val r = 26 * d
        val sun = 1f - night
        canvas.save()
        canvas.translate(x, y)
        if (sun > 0.02f) {
            // The sky is brighter all around the sun: pale by day, warm at dawn and dusk.
            val tone = GLOWS.getValue(p)
            fill.shader = skyGlow?.takeIf { skyGlowTone == tone } ?: RadialGradient(0f, 0f, w * 0.75f, tone, tone and 0x00FFFFFF, Shader.TileMode.CLAMP).also {
                skyGlow = it
                skyGlowTone = tone
            }
            fill.alpha = (255 * sun).toInt()
            canvas.drawCircle(0f, 0f, w * 0.75f, fill)
            // Soft rays, turning slowly.
            if (rays.isEmpty) makeRays(r)
            fill.shader = rayLight ?: RadialGradient(0f, 0f, r * 3.4f, 0x80FFF3BF.toInt(), 0x00FFF3BF, Shader.TileMode.CLAMP).also { rayLight = it }
            fill.alpha = (255 * sun).toInt()
            canvas.save()
            if (moves) canvas.rotate(time * 4f)
            canvas.drawPath(rays, fill)
            canvas.restore()
            // The halo, breathing.
            val pulse = 1f + 0.06f * sin(time * 1.4f)
            canvas.save()
            canvas.scale(pulse, pulse)
            fill.shader = glow ?: RadialGradient(0f, 0f, r * 2.8f, 0x99FFE27A.toInt(), 0x00FFE27A, Shader.TileMode.CLAMP).also { glow = it }
            fill.alpha = (255 * sun).toInt()
            canvas.drawCircle(0f, 0f, r * 2.8f, fill)
            canvas.restore()
            // A ball of warm clay, lit from the top left like Peri, with a glint.
            val color = if (p == Phase.DUSK) 0xFFFF9A4D.toInt() else if (p == Phase.DAWN) 0xFFFFB347.toInt() else Palette.SUN
            fill.shader = sunBall?.takeIf { sunBallColor == color } ?: RadialGradient(-r * 0.35f, -r * 0.4f, r * 1.45f, 0xFFFFF4C4.toInt(), color, Shader.TileMode.CLAMP).also {
                sunBall = it
                sunBallColor = color
            }
            fill.alpha = (255 * sun).toInt()
            canvas.drawCircle(0f, 0f, r, fill)
            fill.shader = null
            fill.color = Palette.WHITE
            fill.alpha = (190 * sun).toInt()
            canvas.drawCircle(-r * 0.42f, -r * 0.46f, r * 0.15f, fill)
        }
        if (night > 0.02f) {
            fill.shader = moonGlow ?: RadialGradient(0f, 0f, r * 2.4f, 0x66FFF6D8, 0x00FFF6D8, Shader.TileMode.CLAMP).also { moonGlow = it }
            fill.alpha = (255 * night).toInt()
            canvas.drawCircle(0f, 0f, r * 2.4f, fill)
            fill.shader = moonBall ?: RadialGradient(-r * 0.3f, -r * 0.35f, r * 1.2f, 0xFFFFFCEE.toInt(), 0xFFEADFB8.toInt(), Shader.TileMode.CLAMP).also { moonBall = it }
            fill.alpha = (255 * night).toInt()
            canvas.drawCircle(0f, 0f, r * 0.85f, fill)
            fill.shader = null
            fill.color = 0xFFE2D5A8.toInt()
            fill.alpha = (255 * night).toInt()
            canvas.drawCircle(r * 0.25f, -r * 0.2f, r * 0.18f, fill)
            canvas.drawCircle(-r * 0.3f, r * 0.25f, r * 0.13f, fill)
            canvas.drawCircle(r * 0.05f, r * 0.42f, r * 0.08f, fill)
        }
        canvas.restore()
    }

    /** Twelve soft rays around the sun, made once. */
    private fun makeRays(r: Float) {
        for (i in 0 until 12) {
            val a = 2 * PI * i / 12
            rays.moveTo((cos(a) * r * 1.3f).toFloat(), (sin(a) * r * 1.3f).toFloat())
            rays.lineTo((cos(a - 0.1) * r * 3.4f).toFloat(), (sin(a - 0.1) * r * 3.4f).toFloat())
            rays.lineTo((cos(a + 0.1) * r * 3.4f).toFloat(), (sin(a + 0.1) * r * 3.4f).toFloat())
            rays.close()
        }
    }

    private fun drawCloud(canvas: Canvas, c: Cloud, w: Float, g: Float) {
        if (place == Place.VILLAGE) {
            // A clay cloud from Muse Image, faint at night like the others.
            val x = c.x * w + lean * LEAN_DP * d * 0.8f
            val y = g * c.y * 0.82f + 40 * d
            return sprite(canvas, R.drawable.peri_scene_cloud, x, y + 26 * d * c.scale, 96 * d * c.scale, shadow = false, alpha = (240 - 170 * night).toInt())
        }
        if (cloud.isEmpty) {
            // A puffy cloud on a flat base, drawn once at 1 dp and scaled.
            cloud.addCircle(-22f, 0f, 15f, Path.Direction.CW)
            cloud.addCircle(-4f, -10f, 19f, Path.Direction.CW)
            cloud.addCircle(15f, -3f, 15f, Path.Direction.CW)
            cloud.addCircle(29f, 5f, 10f, Path.Direction.CW)
            cloud.addRoundRect(RectF(-36f, 0f, 38f, 15f), 7.5f, 7.5f, Path.Direction.CW)
        }
        val colors = CLOUD_COLORS.getValue(shownPhase())
        val alpha = (235 - 170 * night).toInt()
        canvas.save()
        canvas.translate(c.x * w + lean * LEAN_DP * d * 0.8f, g * c.y * 0.82f + 40 * d)
        canvas.scale(c.scale * d * 1.3f, c.scale * d * 1.3f)
        // Its underside in shadow, a little lower; then the cloud itself, lit from above.
        fill.color = colors[2]
        fill.alpha = alpha
        canvas.translate(1.5f, 3.5f)
        canvas.drawPath(cloud, fill)
        canvas.translate(-1.5f, -3.5f)
        fill.shader = cloudShade.of(colors[0], colors[1], colors[1])
        fill.alpha = alpha
        canvas.drawPath(cloud, fill)
        fill.shader = null
        canvas.restore()
    }

    /** A colour of the place, darkened or warmed by the hour. */
    private fun lit(color: Int): Int {
        val to = blend(color, tintTo.first, tintTo.second)
        if (tintMix >= 1f) return to
        return blend(blend(color, tintFrom.first, tintFrom.second), to, tintMix)
    }

    /** Far away: hazy hills, snowy mountains or a headland with a lighthouse, paler towards the horizon. */
    private fun drawDistance(canvas: Canvas, w: Float, g: Float) {
        val base = lit(DISTANT.getValue(place))
        canvas.save()
        canvas.translate(lean * LEAN_DP * d * 0.55f, g)
        fill.shader = distantShade.of(blend(base, sky[1], 0.28f), blend(base, sky[2], 0.45f), blend(base, sky[2], 0.66f))
        canvas.drawPath(distant, fill)
        fill.shader = null
        if (place == Place.SNOW) {
            fill.color = blend(lit(Palette.WHITE), sky[2], 0.15f)
            canvas.drawPath(caps, fill)
        }
        if (place == Place.SEASIDE) lighthouse(canvas, w * 0.11f, -70 * d)
        canvas.restore()
    }

    /** A little lighthouse on the headland. At night its lamp glows and turns, its beam sweeping over the sea. */
    private fun lighthouse(canvas: Canvas, x: Float, base: Float) {
        val haze = sky[2]
        fill.color = blend(lit(Palette.WHITE), haze, 0.2f)
        path.reset()
        path.moveTo(x - 5.5f * d, base)
        path.lineTo(x - 3.8f * d, base - 30 * d)
        path.lineTo(x + 3.8f * d, base - 30 * d)
        path.lineTo(x + 5.5f * d, base)
        path.close()
        canvas.drawPath(path, fill)
        fill.color = blend(lit(Palette.CORAL), haze, 0.2f)
        canvas.drawRect(x - 5f * d, base - 10 * d, x + 5f * d, base - 5 * d, fill)
        canvas.drawRect(x - 4.4f * d, base - 21 * d, x + 4.4f * d, base - 16 * d, fill)
        path.reset()
        path.moveTo(x - 5f * d, base - 35 * d)
        path.lineTo(x, base - 41 * d)
        path.lineTo(x + 5f * d, base - 35 * d)
        path.close()
        canvas.drawPath(path, fill)
        fill.color = blend(0xFFFFE9A8.toInt(), Palette.WHITE, night * 0.5f)
        canvas.drawRect(x - 3 * d, base - 35 * d, x + 3 * d, base - 30 * d, fill)
        if (night < 0.05f) return
        canvas.save()
        canvas.translate(x, base - 32.5f * d)
        fill.shader = lampGlow ?: RadialGradient(0f, 0f, 16 * d, 0xCCFFF3C4.toInt(), 0x00FFF3C4, Shader.TileMode.CLAMP).also { lampGlow = it }
        fill.alpha = (255 * night).toInt()
        canvas.drawCircle(0f, 0f, 16 * d, fill)
        if (beam.isEmpty) {
            beam.moveTo(0f, -2 * d)
            beam.lineTo(170 * d, -18 * d)
            beam.lineTo(170 * d, 18 * d)
            beam.lineTo(0f, 2 * d)
            beam.close()
        }
        // The lamp turns: seen from here, its beam grows, shrinks and swings to the other side.
        if (moves) canvas.scale(cos(time * 0.7f), 1f)
        fill.shader = beamLight ?: LinearGradient(0f, 0f, 170 * d, 0f, 0x88FFF3C4.toInt(), 0x00FFF3C4, Shader.TileMode.CLAMP).also { beamLight = it }
        fill.alpha = (255 * night).toInt()
        canvas.drawPath(beam, fill)
        canvas.restore()
        fill.shader = null
        fill.alpha = 255
    }

    private fun drawFarHills(canvas: Canvas, w: Float, g: Float) {
        val colors = HILLS.getValue(place)
        canvas.save()
        canvas.translate(lean * LEAN_DP * d * 0.3f, g)
        fill.shader = farShade.of(lit(blend(colors[0], Palette.WHITE, 0.14f)), lit(colors[0]), lit(blend(colors[0], SHADES.getValue(place), 0.28f)))
        canvas.drawPath(far, fill)
        fill.shader = null
        // Light on its crest too.
        stroke.color = lit(blend(colors[0], Palette.WHITE, 0.45f))
        stroke.alpha = 170
        stroke.strokeWidth = 2.5f * d
        canvas.drawPath(farCrest, stroke)
        stroke.alpha = 255
        when (place) {
            Place.ORCHARD -> {
                tree(canvas, w * 0.11f, -50 * d, 1f, 0xFF6FAE4C.toInt(), 0xFF5E9E40.toInt(), pears = true)
                tree(canvas, w * 0.26f, -70 * d, 0.7f, 0xFF7DBA55.toInt(), 0xFF6FAE4C.toInt(), pears = true)
                tree(canvas, w * 0.88f, -62 * d, 1.1f, 0xFF6FAE4C.toInt(), 0xFF5E9E40.toInt(), pears = true)
            }
            Place.AUTUMN -> {
                tree(canvas, w * 0.12f, -52 * d, 1f, 0xFFE8833A.toInt(), 0xFFD9532F.toInt(), pears = false)
                tree(canvas, w * 0.27f, -70 * d, 0.72f, 0xFFF2B33D.toInt(), 0xFFE8833A.toInt(), pears = false)
                tree(canvas, w * 0.87f, -64 * d, 1.1f, 0xFFD9532F.toInt(), 0xFFF2B33D.toInt(), pears = false)
            }
            Place.SNOW -> {
                pine(canvas, w * 0.08f, -40 * d, 1f)
                pine(canvas, w * 0.18f, -58 * d, 0.8f)
                pine(canvas, w * 0.84f, -66 * d, 1.1f)
                snowman(canvas, w * 0.95f, -50 * d, 0.85f)
            }
            Place.VILLAGE -> {
                sprite(canvas, R.drawable.peri_scene_tree, w * 0.1f, -44 * d, 74 * d)
                sprite(canvas, R.drawable.peri_scene_tree, w * 0.25f, -66 * d, 52 * d)
                cottage(canvas, w * 0.87f, -56 * d, 76 * d)
            }
            else -> Unit
        }
        canvas.restore()
    }

    private fun drawSea(canvas: Canvas, w: Float, h: Float, g: Float) {
        val horizon = g - 52 * d
        canvas.save()
        canvas.translate(0f, horizon)
        fill.shader = seaShade.of(lit(0xFFA3E0F4.toInt()), lit(0xFF63C4EA.toInt()), lit(0xFF3C9CCF.toInt()))
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
        canvas.restore()
        // The sun or the moon on the water: little dashes of light that come and go.
        val sx = w * 0.80f + lean * LEAN_DP * d
        val shine = if (night > 0.5f) night else 1f - night
        stroke.color = if (night > 0.5f) 0xFFFFF6D8.toInt() else 0xFFFFFBEA.toInt()
        stroke.strokeWidth = 2 * d
        for (i in 0 until 6) {
            val y = horizon + (5 + i * 7f) * d
            val half = (5 + i * 2.8f) * d
            val drift = if (moves) sin(time * 1.3f + i * 2.1f) * 3 * d else 0f
            stroke.alpha = (210 * shine * (0.35f + 0.65f * abs(sin(time * 2.2f + i * 1.3f)))).toInt()
            canvas.drawLine(sx - half + drift, y, sx - half * 0.25f + drift, y, stroke)
            canvas.drawLine(sx + half * 0.2f - drift, y, sx + half - drift, y, stroke)
        }
        // Waves rolling by.
        stroke.color = 0xFFFFFFFF.toInt()
        stroke.alpha = (170 - 90 * night).toInt()
        stroke.strokeWidth = 2.2f * d
        for (row in 0..1) {
            val y = horizon + (12 + row * 22) * d
            val shift = time * (18 + row * 10) * d
            var x = -((shift % (46 * d)))
            while (x < w) {
                path.reset()
                path.moveTo(x, y)
                path.quadTo(x + 9 * d, y - 5 * d, x + 18 * d, y)
                canvas.drawPath(path, stroke)
                x += 46 * d
            }
        }
        stroke.alpha = 255
        // A little sailing boat going to and fro on the horizon.
        val bx = w * (0.5f + 0.38f * sin(time * 0.05f))
        val by = horizon + sin(time * 1.6f) * 1.5f * d
        fill.color = lit(0xFFFF8A7A.toInt())
        path.reset()
        path.moveTo(bx - 16 * d, by - 2 * d)
        path.lineTo(bx + 16 * d, by - 2 * d)
        path.lineTo(bx + 11 * d, by + 6 * d)
        path.lineTo(bx - 11 * d, by + 6 * d)
        path.close()
        canvas.drawPath(path, fill)
        fill.color = lit(0xFFFFFFFF.toInt())
        path.reset()
        path.moveTo(bx - 1 * d, by - 4 * d)
        path.lineTo(bx - 1 * d, by - 30 * d)
        path.lineTo(bx + 15 * d, by - 6 * d)
        path.close()
        canvas.drawPath(path, fill)
    }

    private fun frontAt(fx: Float, g: Float): Float {
        // Follows the front hill's two curves closely enough to set things on it.
        return if (fx <= 0.5f) bez(fx / 0.5f, g + 40 * d, g + 8 * d, g, g) else bez((fx - 0.5f) / 0.5f, g, g, g + 6 * d, g + 34 * d)
    }

    private fun drawFrontHill(canvas: Canvas, w: Float, g: Float) {
        val colors = HILLS.getValue(place)
        val shade = SHADES.getValue(place)
        canvas.save()
        canvas.translate(0f, g)
        fill.shader = frontShade.of(lit(blend(colors[1], Palette.WHITE, 0.16f)), lit(colors[1]), lit(blend(colors[1], shade, 0.34f)))
        canvas.drawPath(front, fill)
        fill.shader = null
        // A lighter rim along the top, like light on plasticine.
        stroke.color = lit(colors[2])
        stroke.strokeWidth = 4 * d
        canvas.drawPath(crest, stroke)
        canvas.restore()
        // Fingerprints in the clay, lighter and darker, under the crest.
        val dark = lit(blend(colors[1], shade, 0.55f))
        val light = lit(blend(colors[1], Palette.WHITE, 0.5f))
        for (i in speckles.indices) {
            val s = speckles[i]
            val bright = s[3] > 0.5f
            fill.color = if (bright) light else dark
            fill.alpha = if (bright) 115 else 70
            canvas.drawCircle(s[0] * w, frontAt(s[0], g) + s[1] * d, s[2] * d, fill)
        }
        fill.alpha = 255
    }

    private fun drawPlaceLife(canvas: Canvas, w: Float, h: Float, g: Float) {
        val hidden = 1f - veil
        val grass = lit(blend(HILLS.getValue(place)[1], SHADES.getValue(place), 0.5f))
        when (place) {
            Place.MEADOW -> {
                tufts(canvas, w, g, grass)
                for (i in FLOWER_X.indices) flower(canvas, w * FLOWER_X[i], frontAt(FLOWER_X[i], g) + (14 + (i % 3) * 7) * d, FLOWER_COLORS[i])
                butterfly(canvas, w, g, hidden)
            }
            Place.ORCHARD -> {
                tufts(canvas, w, g, grass)
                for (fx in BUSHES) bush(canvas, w * fx, frontAt(fx, g) + 16 * d, 0xFF6FAE4C.toInt())
                butterfly(canvas, w, g, hidden)
            }
            Place.SEASIDE -> {
                starfish(canvas, w * 0.09f, frontAt(0.09f, g) + 26 * d)
                shell(canvas, w * 0.96f, frontAt(0.96f, g) + 28 * d)
                parasol(canvas, w * 0.87f, frontAt(0.87f, g) + 10 * d)
                gulls(canvas, w, g, hidden)
            }
            Place.VILLAGE -> {
                tufts(canvas, w, g, grass)
                sprite(canvas, R.drawable.peri_scene_bush, w * 0.06f, frontAt(0.06f, g) + 22 * d, 44 * d)
                sprite(canvas, R.drawable.peri_scene_bush, w * 0.95f, frontAt(0.95f, g) + 24 * d, 40 * d)
                butterfly(canvas, w, g, hidden)
            }
            Place.AUTUMN -> {
                tufts(canvas, w, g, grass)
                fallenLeaves(canvas, w, g)
                for (fx in AUTUMN_BUSHES) bush(canvas, w * fx, frontAt(fx, g) + 16 * d, 0xFFC9792F.toInt())
            }
            Place.SNOW -> Unit
        }
        // Petals, leaves and snowflakes, everywhere in the sky; fireflies by the ground at night.
        val snow = place == Place.SNOW
        for (i in bits.indices) {
            val b = bits[i]
            fill.color = b.color
            fill.alpha = (255 * hidden * if (snow) 0.9f else 1f).toInt()
            val x = b.x * w
            val y = b.y * h
            if (snow) {
                canvas.drawCircle(x, y, b.size * d, fill)
            } else {
                canvas.save()
                canvas.translate(x, y)
                canvas.rotate(b.spin)
                oval.set(-b.size * d, -b.size * 0.55f * d, b.size * d, b.size * 0.55f * d)
                canvas.drawOval(oval, fill)
                canvas.restore()
            }
        }
        if (night > 0.4f && !snow) fireflies(canvas, w, g, hidden * (night - 0.4f) / 0.6f)
    }

    /**
     * One of the village's Muse Image pictures, standing on ([x], [base]) at [wide] px across, lit by the hour like
     * everything drawn in code, on its soft shadow. Each picture is decoded once, at the size it's drawn.
     */
    private fun sprite(canvas: Canvas, res: Int, x: Float, base: Float, wide: Float, shadow: Boolean = true, alpha: Int = 255) {
        val bmp = scenery(res, wide) ?: return
        val high = wide * bmp.height / bmp.width
        if (shadow) groundShadow(canvas, x + 2 * d, base, wide * 0.42f)
        spritePaint.colorFilter = lightFor(lit(Palette.WHITE))
        spritePaint.alpha = alpha.coerceIn(0, 255)
        spriteBox.set(x - wide / 2, base - high, x + wide / 2, base)
        canvas.drawBitmap(bmp, null, spriteBox, spritePaint)
    }

    /** Peri's cottage, with smoke from its chimney that rises and fades. */
    private fun cottage(canvas: Canvas, x: Float, base: Float, wide: Float) {
        sprite(canvas, R.drawable.peri_scene_cottage, x, base, wide)
        val bmp = scenery(R.drawable.peri_scene_cottage, wide) ?: return
        val top = base - wide * bmp.height / bmp.width
        val chimneyX = x - wide * 0.07f
        fill.color = lit(0xFFF4F1EC.toInt())
        for (i in 0 until 3) {
            val t = ((time * 0.35f + i / 3f) % 1f)
            fill.alpha = (150 * (1 - t) * (1f - veil)).toInt()
            canvas.drawCircle(chimneyX + sin(time * 0.9f + i) * 4 * d + t * 10 * d, top - t * 36 * d, (4 + t * 7) * d, fill)
        }
        fill.alpha = 255
    }

    private fun scenery(res: Int, wide: Float): Bitmap? {
        val target = wide.toInt().coerceAtLeast(8)
        sceneryCache[res]?.takeIf { it.width >= target }?.let { return it }
        val bmp = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, res)) { decoder, info, _ ->
                if (info.size.width > target) decoder.setTargetSize(target, maxOf(1, target * info.size.height / info.size.width))
            }
        }.getOrNull() ?: return null
        sceneryCache[res] = bmp
        return bmp
    }

    /** The hour's light on a picture: white for day, warmer at dusk, blue and dim at night. */
    private fun lightFor(tone: Int): LightingColorFilter {
        if (tone != lightTone) {
            lightTone = tone
            light = LightingColorFilter(tone and 0x00FFFFFF, 0)
        }
        return light
    }

    /** A soft shadow on the ground, under something standing on it. */
    private fun groundShadow(canvas: Canvas, x: Float, y: Float, rx: Float) {
        fill.color = 0xFF000000.toInt()
        fill.alpha = (34 * (1f - 0.5f * night)).toInt()
        oval.set(x - rx, y - rx * 0.22f, x + rx, y + rx * 0.22f)
        canvas.drawOval(oval, fill)
        fill.alpha = 255
    }

    /** Tufts of grass along the crest, their tips moving a little in the wind. */
    private fun tufts(canvas: Canvas, w: Float, g: Float, color: Int) {
        stroke.color = color
        stroke.strokeWidth = 1.8f * d
        for (fx in TUFTS) {
            val x = w * fx
            val y = frontAt(fx, g) + 3 * d
            val sway = if (moves) sin(time * 1.6f + fx * 9f) * 1.2f * d else 0f
            canvas.drawLine(x - 1 * d, y, x - 4 * d + sway, y - 7 * d, stroke)
            canvas.drawLine(x, y, x + sway, y - 10 * d, stroke)
            canvas.drawLine(x + 1 * d, y, x + 4 * d + sway, y - 7 * d, stroke)
        }
    }

    private fun tree(canvas: Canvas, x: Float, base: Float, s: Float, light: Int, dark: Int, pears: Boolean) {
        val u = d * s
        groundShadow(canvas, x + 2 * u, base + 3 * u, 15 * u)
        fill.color = lit(0xFF8A5A3C.toInt())
        canvas.drawRoundRect(x - 3.5f * u, base - 26 * u, x + 3.5f * u, base + 4 * u, 3 * u, 3 * u, fill)
        // The crown moves a little in the wind, lit from the top left like Peri.
        val cx = x + if (moves) sin(time * 0.8f + x * 0.013f) * 0.9f * u else 0f
        fill.color = lit(dark)
        canvas.drawCircle(cx - 11 * u, base - 30 * u, 13 * u, fill)
        canvas.drawCircle(cx + 11 * u, base - 31 * u, 13 * u, fill)
        fill.color = lit(light)
        canvas.drawCircle(cx, base - 42 * u, 17 * u, fill)
        fill.color = lit(blend(light, Palette.WHITE, 0.3f))
        fill.alpha = 170
        canvas.drawCircle(cx - 6 * u, base - 49 * u, 6.5f * u, fill)
        fill.alpha = 255
        if (pears) {
            fill.color = lit(0xFFD8E163.toInt())
            for (i in 0 until PEARS.size / 2) {
                val dx = PEARS[2 * i]
                val dy = PEARS[2 * i + 1]
                oval.set(cx + (dx - 3.2f) * u, base + (dy - 4.2f) * u, cx + (dx + 3.2f) * u, base + (dy + 4.2f) * u)
                canvas.drawOval(oval, fill)
            }
        }
    }

    private fun pine(canvas: Canvas, x: Float, base: Float, s: Float) {
        val u = d * s
        groundShadow(canvas, x + 2 * u, base + 3 * u, 13 * u)
        fill.color = lit(0xFF7A5238.toInt())
        canvas.drawRect(x - 2.5f * u, base - 6 * u, x + 2.5f * u, base + 4 * u, fill)
        for (tier in 0..2) {
            val top = base - (22 + tier * 14) * u
            val half = (16 - tier * 4) * u
            fill.color = lit(0xFF3E7A58.toInt())
            path.reset()
            path.moveTo(x, top - 14 * u)
            path.lineTo(x + half, top + 8 * u)
            path.lineTo(x - half, top + 8 * u)
            path.close()
            canvas.drawPath(path, fill)
            // Its right side in shade.
            fill.color = lit(0xFF2F6447.toInt())
            path.reset()
            path.moveTo(x, top - 14 * u)
            path.lineTo(x + half, top + 8 * u)
            path.lineTo(x, top + 8 * u)
            path.close()
            canvas.drawPath(path, fill)
            fill.color = lit(0xFFFFFFFF.toInt())
            path.reset()
            path.moveTo(x, top - 14 * u)
            path.lineTo(x + half * 0.42f, top - 6 * u)
            path.lineTo(x - half * 0.42f, top - 6 * u)
            path.close()
            canvas.drawPath(path, fill)
        }
    }

    /** A little snowman in a coral scarf, shaded on the right like everything here. */
    private fun snowman(canvas: Canvas, x: Float, base: Float, s: Float) {
        val u = d * s
        groundShadow(canvas, x + 2 * u, base + 1 * u, 13 * u)
        val snow = lit(Palette.WHITE)
        val shade = lit(0xFFC9D6EA.toInt())
        fill.color = shade
        canvas.drawCircle(x + 1.5f * u, base - 10 * u, 11 * u, fill)
        canvas.drawCircle(x + 1.2f * u, base - 27 * u, 7.5f * u, fill)
        fill.color = snow
        canvas.drawCircle(x, base - 11 * u, 10.5f * u, fill)
        canvas.drawCircle(x, base - 28 * u, 7 * u, fill)
        fill.color = lit(Palette.CORAL)
        canvas.drawRoundRect(x - 7 * u, base - 22.5f * u, x + 7 * u, base - 19.5f * u, 1.5f * u, 1.5f * u, fill)
        canvas.drawRoundRect(x + 2 * u, base - 21 * u, x + 5 * u, base - 13 * u, 1.5f * u, 1.5f * u, fill)
        fill.color = lit(0xFF2E3440.toInt())
        canvas.drawCircle(x - 2.6f * u, base - 30 * u, 1.1f * u, fill)
        canvas.drawCircle(x + 2.6f * u, base - 30 * u, 1.1f * u, fill)
        fill.color = lit(0xFFF28C38.toInt())
        path.reset()
        path.moveTo(x, base - 28.5f * u)
        path.lineTo(x + 7 * u, base - 27 * u)
        path.lineTo(x, base - 26 * u)
        path.close()
        canvas.drawPath(path, fill)
    }

    private fun flower(canvas: Canvas, x: Float, y: Float, petals: Int) {
        stroke.color = lit(0xFF4E8A33.toInt())
        stroke.strokeWidth = 2 * d
        canvas.drawLine(x, y, x, y - 10 * d, stroke)
        fill.color = lit(petals)
        val sway = if (moves) sin(time * 1.2f + x) * 1.5f * d else 0f
        for (i in 0 until 5) {
            val a = 2 * PI * i / 5
            canvas.drawCircle(x + sway + (cos(a) * 3.6f * d).toFloat(), y - 13 * d + (sin(a) * 3.6f * d).toFloat(), 3 * d, fill)
        }
        fill.color = lit(Palette.SUN)
        canvas.drawCircle(x + sway, y - 13 * d, 2.4f * d, fill)
    }

    /** A little round bush sitting on the hill: three soft bumps, with a light on top like the hills. */
    private fun bush(canvas: Canvas, x: Float, y: Float, color: Int) {
        groundShadow(canvas, x + 1 * d, y + 7 * d, 17 * d)
        fill.color = lit(color)
        canvas.drawCircle(x - 9 * d, y, 8 * d, fill)
        canvas.drawCircle(x + 9 * d, y + 1 * d, 7 * d, fill)
        canvas.drawCircle(x, y - 5 * d, 10 * d, fill)
        fill.color = lit(blend(color, 0xFFFFFFFF.toInt(), 0.35f))
        canvas.drawCircle(x - 3 * d, y - 10 * d, 3 * d, fill)
    }

    /** Leaves fallen on the autumn hill. */
    private fun fallenLeaves(canvas: Canvas, w: Float, g: Float) {
        for (i in 0 until LEAVES.size / 3) {
            val fx = LEAVES[3 * i]
            fill.color = lit(AUTUMN_COLORS[i % AUTUMN_COLORS.size])
            canvas.save()
            canvas.translate(w * fx, frontAt(fx, g) + LEAVES[3 * i + 1] * d)
            canvas.rotate(LEAVES[3 * i + 2])
            oval.set(-4.5f * d, -2.4f * d, 4.5f * d, 2.4f * d)
            canvas.drawOval(oval, fill)
            canvas.restore()
        }
    }

    private fun parasol(canvas: Canvas, x: Float, y: Float) {
        groundShadow(canvas, x - 6 * d, y + 1 * d, 20 * d)
        stroke.color = lit(0xFF8A5A3C.toInt())
        stroke.strokeWidth = 2.5f * d
        canvas.drawLine(x, y, x - 4 * d, y - 44 * d, stroke)
        oval.set(x - 30 * d, y - 62 * d, x + 22 * d, y - 30 * d)
        for (i in 0 until 4) {
            fill.color = lit(if (i % 2 == 0) Palette.CORAL else 0xFFFFFFFF.toInt())
            canvas.drawArc(oval, 180f + i * 45f, 45f, true, fill)
        }
    }

    /** A starfish lying on the sand. */
    private fun starfish(canvas: Canvas, x: Float, y: Float) {
        fill.color = lit(0xFFFF9E7A.toInt())
        path.reset()
        for (i in 0 until 10) {
            val a = -PI / 2 + PI * i / 5
            val r = (if (i % 2 == 0) 7 else 3) * d
            val px = x + (cos(a) * r).toFloat()
            val py = y + (sin(a) * r * 0.75f).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, fill)
        fill.color = lit(0xFFFFC7AE.toInt())
        canvas.drawCircle(x - 0.8f * d, y - 0.8f * d, 1.5f * d, fill)
    }

    /** A shell on the sand, its ridges fanning out. */
    private fun shell(canvas: Canvas, x: Float, y: Float) {
        fill.color = lit(0xFFFFE3D3.toInt())
        oval.set(x - 6 * d, y - 6 * d, x + 6 * d, y + 6 * d)
        canvas.drawArc(oval, 180f, 180f, true, fill)
        stroke.color = lit(0xFFE7B9A3.toInt())
        stroke.strokeWidth = 1.1f * d
        for (k in -2..2) canvas.drawLine(x, y, x + k * 2.4f * d, y - 5.2f * d + abs(k) * 0.9f * d, stroke)
    }

    private fun butterfly(canvas: Canvas, w: Float, g: Float, alpha: Float) {
        if (night > 0.5f || alpha < 0.05f) return
        val x = w * (0.22f + 0.14f * sin(time * 0.31f) + 0.05f * sin(time * 0.83f))
        val y = g - 95 * d + 22 * d * sin(time * 0.57f)
        val flap = 0.25f + 0.75f * abs(sin(time * 11f))
        fill.color = Palette.LILAC
        fill.alpha = (255 * alpha).toInt()
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(flap, 1f)
        canvas.drawCircle(-5 * d, -2 * d, 5 * d, fill)
        canvas.drawCircle(5 * d, -2 * d, 5 * d, fill)
        fill.color = Palette.CORAL
        fill.alpha = (255 * alpha).toInt()
        canvas.drawCircle(-4 * d, 4 * d, 3.4f * d, fill)
        canvas.drawCircle(4 * d, 4 * d, 3.4f * d, fill)
        canvas.restore()
    }

    private fun gulls(canvas: Canvas, w: Float, g: Float, alpha: Float) {
        if (night > 0.5f || alpha < 0.05f) return
        stroke.color = 0xFF55606E.toInt()
        stroke.alpha = (220 * alpha).toInt()
        stroke.strokeWidth = 2 * d
        for (i in 0..1) {
            val x = ((time * (14 + i * 5) * d + i * w * 0.5f) % (w + 80 * d)) - 40 * d
            val y = g * (0.32f + i * 0.1f) + sin(time * 1.1f + i) * 6 * d
            val beat = 4 * d * sin(time * 6f + i)
            path.reset()
            path.moveTo(x - 9 * d, y - beat)
            path.quadTo(x - 4 * d, y - 5 * d, x, y)
            path.quadTo(x + 4 * d, y - 5 * d, x + 9 * d, y - beat)
            canvas.drawPath(path, stroke)
        }
        stroke.alpha = 255
    }

    private fun fireflies(canvas: Canvas, w: Float, g: Float, alpha: Float) {
        if (alpha < 0.05f) return
        for (i in 0 until 7) {
            val s = i * 1.7f
            val x = w * (0.06f + 0.88f * (((i * 0.37f + 0.11f * sin(time * 0.21f + s)) % 1f + 1f) % 1f))
            val y = g - 50 * d + 34 * d * sin(time * 0.43f + s)
            val glowAlpha = (0.5f + 0.5f * sin(time * 2.1f + s)) * alpha
            fill.color = 0xFFFFF0A0.toInt()
            fill.alpha = (70 * glowAlpha).toInt()
            canvas.drawCircle(x, y, 7 * d, fill)
            fill.alpha = (255 * glowAlpha).toInt()
            canvas.drawCircle(x, y, 2.2f * d, fill)
        }
    }

    private fun drawConfetti(canvas: Canvas, w: Float, h: Float) {
        for (i in confetti.indices) {
            val c = confetti[i]
            fill.color = c.color
            canvas.save()
            canvas.translate(c.x * w, c.y * h)
            canvas.rotate(c.spin)
            canvas.drawRect(-c.size * d, -c.size * 0.45f * d, c.size * d, c.size * 0.45f * d, fill)
            canvas.restore()
        }
    }

    /** While Peri warns: a soft coral light from the top, breathing slowly. Never red, never flashing. */
    private fun drawVeil(canvas: Canvas, w: Float, h: Float) {
        if (veil < 0.02f) return
        val breath = 0.85f + 0.15f * sin(time * 1.5f)
        fill.shader = veilShader ?: LinearGradient(0f, 0f, 0f, h * 0.75f, 0x99FF8A7A.toInt(), 0x00FF8A7A, Shader.TileMode.CLAMP).also { veilShader = it }
        fill.alpha = (255 * veil * breath).toInt()
        canvas.drawRect(0f, 0f, w, h, fill)
        fill.shader = null
        fill.alpha = 255
    }

    companion object {
        private const val FRAME_MS = 33L
        private const val SKY_MS = 1_200f
        private const val SHOOT_MS = 700f
        private const val CONFETTI_MS = 3_200L
        private const val SKY_UNIT = 1000f
        const val PREFS = "world"
        const val PLACE = "place"
        const val AUTO = "auto"
        const val RANDOM = "random"
        /** How far, in dp, the farthest things drift with the phone's lean. */
        private const val LEAN_DP = 18f
        private const val SPARKLES = 5
        private const val SNOW_STEP = 1f / 32

        /** Hills far away and snowy mountains: share of the width, dp above the ground. */
        private val ROLLING = floatArrayOf(-0.1f, 58f, 0.12f, 112f, 0.3f, 86f, 0.47f, 120f, 0.64f, 92f, 0.8f, 136f, 0.96f, 102f, 1.1f, 90f)
        private val MOUNTAINS = floatArrayOf(-0.1f, 64f, 0.1f, 150f, 0.25f, 98f, 0.42f, 166f, 0.57f, 106f, 0.74f, 178f, 0.9f, 112f, 1.1f, 84f)

        /** What's far away in each place, before the haze turns it towards the sky: cooler than what's near. */
        private val DISTANT = mapOf(
            Place.MEADOW to 0xFFA6D3B6.toInt(),
            Place.ORCHARD to 0xFFA3CDB4.toInt(),
            Place.SEASIDE to 0xFF8DC48C.toInt(),
            Place.AUTUMN to 0xFFD8B48E.toInt(),
            Place.SNOW to 0xFFB4C4DE.toInt(),
            Place.VILLAGE to 0xFFA6D3B6.toInt(),
        )

        /** Each place's shade, for the hills' lower slopes, the grass and the darker fingerprints in the clay. */
        private val SHADES = mapOf(
            Place.MEADOW to 0xFF3E7A35.toInt(),
            Place.ORCHARD to 0xFF3E7A35.toInt(),
            Place.SEASIDE to 0xFFC08F55.toInt(),
            Place.AUTUMN to 0xFF8C5A2B.toInt(),
            Place.SNOW to 0xFF9DB2D4.toInt(),
            Place.VILLAGE to 0xFF3E7A35.toInt(),
        )

        /** Clouds by the hour: their lit top, their body and their underside in shadow. */
        private val CLOUD_COLORS = mapOf(
            Phase.DAWN to intArrayOf(0xFFFFFBF6.toInt(), 0xFFFFEBDD.toInt(), 0xFFF2CDBE.toInt()),
            Phase.DAY to intArrayOf(0xFFFFFFFF.toInt(), 0xFFF0F6FD.toInt(), 0xFFD2E1F3.toInt()),
            Phase.DUSK to intArrayOf(0xFFFFF1E8.toInt(), 0xFFFFD9C9.toInt(), 0xFFE3AEAD.toInt()),
            Phase.NIGHT to intArrayOf(0xFFA9B1DE.toInt(), 0xFF8E97CF.toInt(), 0xFF6D76AE.toInt()),
        )

        /** The light all around the sun: pale by day, warm at dawn and dusk. */
        private val GLOWS = mapOf(
            Phase.DAWN to 0x80FFD9A8.toInt(),
            Phase.DAY to 0x66FFF8DC,
            Phase.DUSK to 0x80FFB48C.toInt(),
            Phase.NIGHT to 0,
        )

        private val AUTUMN_COLORS = intArrayOf(0xFFE8833A.toInt(), 0xFFD9532F.toInt(), 0xFFF2B33D.toInt(), 0xFFC0662B.toInt())
        private val FLOWER_X = floatArrayOf(0.06f, 0.15f, 0.24f, 0.77f, 0.86f, 0.94f)
        private val FLOWER_COLORS = intArrayOf(Palette.WHITE, Palette.CORAL, Palette.SUN, Palette.LILAC, Palette.WHITE, Palette.CORAL)
        private val BUSHES = floatArrayOf(0.07f, 0.2f, 0.83f, 0.95f)
        private val AUTUMN_BUSHES = floatArrayOf(0.08f, 0.86f)
        private val TUFTS = floatArrayOf(0.03f, 0.12f, 0.28f, 0.72f, 0.88f, 0.97f)
        /** Fallen leaves: share of the width, dp under the crest, turn. */
        private val LEAVES = floatArrayOf(0.16f, 10f, 20f, 0.23f, 24f, -35f, 0.31f, 14f, 60f, 0.7f, 20f, -15f, 0.77f, 9f, 40f)
        /** The pears on an orchard tree, from the foot of its trunk (dp). */
        private val PEARS = floatArrayOf(-9f, -30f, 8f, -38f, 12f, -26f)

        private val SKIES = mapOf(
            Phase.DAWN to intArrayOf(0xFFFFC9A0.toInt(), 0xFFFFE3C4.toInt(), 0xFFFFF4E2.toInt()),
            Phase.DAY to intArrayOf(0xFF8FD3FF.toInt(), 0xFFC9ECFF.toInt(), 0xFFF2FAFF.toInt()),
            Phase.DUSK to intArrayOf(0xFFFFA585.toInt(), 0xFFFFC59A.toInt(), 0xFFFFE6C4.toInt()),
            Phase.NIGHT to intArrayOf(0xFF141C44.toInt(), 0xFF2C3878.toInt(), 0xFF4B5596.toInt()),
        )

        /** How the hour colours the ground: towards which colour, and how much. */
        private val TINTS = mapOf(
            Phase.DAWN to (0xFFFF9F7A.toInt() to 0.10f),
            Phase.DAY to (0xFFFFFFFF.toInt() to 0f),
            Phase.DUSK to (0xFFB3546E.toInt() to 0.18f),
            Phase.NIGHT to (0xFF1E2656.toInt() to 0.55f),
        )

        /** Each place's far hills, front hill and the rim of light on it. */
        private val HILLS = mapOf(
            Place.MEADOW to intArrayOf(0xFFCDEBB2.toInt(), 0xFFA5D676.toInt(), 0xFFCBEBA4.toInt()),
            Place.ORCHARD to intArrayOf(0xFFC6E6C0.toInt(), 0xFF9CCB72.toInt(), 0xFFC3E39E.toInt()),
            Place.SEASIDE to intArrayOf(0xFF63C4EA.toInt(), 0xFFF6DFA8.toInt(), 0xFFFFF0C8.toInt()),
            Place.AUTUMN to intArrayOf(0xFFF2CF9A.toInt(), 0xFFD9A75A.toInt(), 0xFFEBC37F.toInt()),
            Place.SNOW to intArrayOf(0xFFE2EAF6.toInt(), 0xFFF7FAFF.toInt(), 0xFFFFFFFF.toInt()),
            Place.VILLAGE to intArrayOf(0xFFCFEAB4.toInt(), 0xFFAEDA7E.toInt(), 0xFFD2EFAD.toInt()),
        )

        fun phaseAt(hour: Int): Phase = when (hour) {
            in 6..8 -> Phase.DAWN
            in 9..17 -> Phase.DAY
            in 18..20 -> Phase.DUSK
            else -> Phase.NIGHT
        }

        /** The light sky of the hour for the screens around Peri's: always light, so dark text reads on it. */
        fun lightSky(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): IntArray = when (phaseAt(hour)) {
            Phase.DAWN -> intArrayOf(0xFFFFE6CF.toInt(), 0xFFFFF3E3.toInt(), Palette.CREAM)
            Phase.DAY -> intArrayOf(0xFFD5EEFF.toInt(), 0xFFEEF7FA.toInt(), Palette.CREAM)
            Phase.DUSK -> intArrayOf(0xFFFFDCC8.toInt(), 0xFFFFEEDD.toInt(), Palette.CREAM)
            Phase.NIGHT -> intArrayOf(0xFFE3E0F7.toInt(), 0xFFF1EEF4.toInt(), Palette.CREAM)
        }

        /**
         * Today's place: the one chosen in Settings, a surprise each time if that's what was chosen, or one of the
         * season's, a different one each day. Winter has snow, spring and summer the meadow and the sea, autumn its
         * hills (northern seasons).
         */
        fun chosenPlace(context: Context, today: Calendar = Calendar.getInstance()): Place {
            val chosen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PLACE, AUTO)
            Place.entries.firstOrNull { it.name == chosen }?.let { return it }
            if (chosen == RANDOM) return Place.entries.random()
            val season = when (today.get(Calendar.MONTH)) {
                Calendar.DECEMBER, Calendar.JANUARY, Calendar.FEBRUARY -> listOf(Place.SNOW, Place.ORCHARD, Place.SEASIDE)
                Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> listOf(Place.MEADOW, Place.ORCHARD, Place.SEASIDE)
                Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> listOf(Place.SEASIDE, Place.MEADOW, Place.ORCHARD)
                else -> listOf(Place.AUTUMN, Place.ORCHARD, Place.MEADOW)
            }
            return season[today.get(Calendar.DAY_OF_YEAR) % season.size]
        }

        private fun bez(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
            val u = 1 - t
            return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3
        }

        fun blend(a: Int, b: Int, t: Float): Int {
            val k = t.coerceIn(0f, 1f)
            fun ch(shift: Int) = (((a shr shift) and 0xFF) + (((b shr shift) and 0xFF) - ((a shr shift) and 0xFF)) * k).toInt()
            return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}
