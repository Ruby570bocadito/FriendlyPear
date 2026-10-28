package app.friendlypear.ui.peri

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
 * The sun or the moon floats in it, clouds drift by, stars twinkle at night. On the ground, a place that changes
 * every day with the season (a meadow, an orchard, the seaside, autumn hills, snow), each with its own little life:
 * petals, a butterfly, a boat, falling leaves, snowflakes, fireflies at night. On Peri's section the hills rise to
 * her feet ([setGround]); elsewhere they sink to make room. Decorative: TalkBack skips it, and with "remove
 * animations" it's drawn once, still. It draws at most 30 times a second and stops when it can't be seen.
 */
class PeriWorld(context: Context) : View(context) {
    enum class Phase { DAWN, DAY, DUSK, NIGHT }

    enum class Place { MEADOW, ORCHARD, SEASIDE, AUTUMN, SNOW }

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
    private val rim = Path()
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
            Place.MEADOW -> 7
            Place.ORCHARD -> 5
            Place.SEASIDE -> 0
        }
        repeat(count) { bits += newBit(Random.nextFloat()) }
    }

    private fun newBit(heightShare: Float): Bit {
        val colors = when (place) {
            Place.SNOW -> listOf(0xFFFFFFFF.toInt())
            Place.AUTUMN -> listOf(0xFFE8833A.toInt(), 0xFFD9532F.toInt(), 0xFFF2B33D.toInt(), 0xFFC0662B.toInt())
            Place.MEADOW -> listOf(0xFFFFFFFF.toInt(), 0xFFFFC2CF.toInt(), 0xFFFFE9A8.toInt())
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

    // Drawing, from the back to the front.

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val g = if (ground < 0f) h * 0.55f else ground
        // Each part sets its own alpha; nothing carries over from the last frame (the sky would fade).
        fill.alpha = 255
        stroke.alpha = 255
        drawSky(canvas, w, h, g)
        drawStars(canvas, w, g)
        drawSunAndMoon(canvas, w, g)
        clouds.forEach { drawCloud(canvas, it, w, g) }
        when (place) {
            Place.SEASIDE -> drawSea(canvas, w, h, g)
            else -> drawFarHills(canvas, w, h, g)
        }
        drawFrontHill(canvas, w, h, g)
        drawPlaceLife(canvas, w, h, g)
        drawConfetti(canvas, w, h)
        drawVeil(canvas, w, h)
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
        for (s in stars) {
            val twinkle = 0.55f + 0.45f * sin(time * s[4] + s[3])
            fill.color = 0xFFFFF8E1.toInt()
            fill.alpha = (255 * night * twinkle).toInt().coerceIn(0, 255)
            canvas.drawCircle(s[0] * w, s[1] * g * 0.9f, s[2] * d, fill)
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
        val x = w * 0.80f
        val bob = if (moves) sin(time * 0.9f) * 4 * d else 0f
        val p = shownPhase()
        // High in the sky, behind the sections' first cards; lower at dawn and dusk.
        val y = (if (p == Phase.DAY || p == Phase.NIGHT) 118 else 150) * d + bob
        val r = 26 * d
        val halo = glow ?: RadialGradient(0f, 0f, r * 2.8f, 0x99FFE27A.toInt(), 0x00FFE27A, Shader.TileMode.CLAMP).also { glow = it }
        val sun = 1f - night
        canvas.save()
        canvas.translate(x, y)
        if (sun > 0.02f) {
            val pulse = 1f + 0.06f * sin(time * 1.4f)
            canvas.save()
            canvas.scale(pulse, pulse)
            fill.shader = halo
            fill.alpha = (255 * sun).toInt()
            canvas.drawCircle(0f, 0f, r * 2.8f, fill)
            canvas.restore()
            fill.shader = null
            fill.color = if (p == Phase.DUSK) 0xFFFF9A4D.toInt() else if (p == Phase.DAWN) 0xFFFFB347.toInt() else Palette.SUN
            fill.alpha = (255 * sun).toInt()
            canvas.drawCircle(0f, 0f, r, fill)
            fill.color = 0xFFFFF0B3.toInt()
            fill.alpha = (200 * sun).toInt()
            canvas.drawCircle(-r * 0.32f, -r * 0.32f, r * 0.42f, fill)
        }
        if (night > 0.02f) {
            fill.shader = moonGlow ?: RadialGradient(0f, 0f, r * 2.4f, 0x66FFF6D8, 0x00FFF6D8, Shader.TileMode.CLAMP).also { moonGlow = it }
            fill.alpha = (255 * night).toInt()
            canvas.drawCircle(0f, 0f, r * 2.4f, fill)
            fill.shader = null
            fill.color = 0xFFFFF6D8.toInt()
            fill.alpha = (255 * night).toInt()
            canvas.drawCircle(0f, 0f, r * 0.85f, fill)
            fill.color = 0xFFE9DDB4.toInt()
            fill.alpha = (255 * night).toInt()
            canvas.drawCircle(r * 0.25f, -r * 0.2f, r * 0.18f, fill)
            canvas.drawCircle(-r * 0.3f, r * 0.25f, r * 0.13f, fill)
            canvas.drawCircle(r * 0.05f, r * 0.42f, r * 0.08f, fill)
        }
        canvas.restore()
    }

    private fun drawCloud(canvas: Canvas, c: Cloud, w: Float, g: Float) {
        if (cloud.isEmpty) {
            // A puffy cloud on a flat base, drawn once at 1 dp and scaled.
            cloud.addCircle(-22f, 0f, 15f, Path.Direction.CW)
            cloud.addCircle(-4f, -10f, 19f, Path.Direction.CW)
            cloud.addCircle(15f, -3f, 15f, Path.Direction.CW)
            cloud.addCircle(29f, 5f, 10f, Path.Direction.CW)
            cloud.addRoundRect(RectF(-36f, 0f, 38f, 15f), 7.5f, 7.5f, Path.Direction.CW)
        }
        val tint = when (shownPhase()) {
            Phase.DAY -> 0xFFFFFFFF.toInt()
            Phase.DAWN -> 0xFFFFF1E6.toInt()
            Phase.DUSK -> 0xFFFFE0D2.toInt()
            Phase.NIGHT -> 0xFF8E97CF.toInt()
        }
        fill.color = tint
        fill.alpha = (235 - 170 * night).toInt()
        canvas.save()
        canvas.translate(c.x * w, g * c.y * 0.82f + 40 * d)
        canvas.scale(c.scale * d * 1.3f, c.scale * d * 1.3f)
        canvas.drawPath(cloud, fill)
        canvas.restore()
    }

    /** A colour of the place, darkened or warmed by the hour. */
    private fun lit(color: Int): Int {
        val to = blend(color, tintTo.first, tintTo.second)
        if (tintMix >= 1f) return to
        return blend(blend(color, tintFrom.first, tintFrom.second), to, tintMix)
    }

    private fun drawFarHills(canvas: Canvas, w: Float, h: Float, g: Float) {
        val colors = HILLS.getValue(place)
        path.reset()
        path.moveTo(0f, g - 26 * d)
        path.cubicTo(w * 0.10f, g - 60 * d, w * 0.26f, g - 78 * d, w * 0.38f, g - 52 * d)
        path.cubicTo(w * 0.48f, g - 34 * d, w * 0.58f, g - 40 * d, w * 0.68f, g - 58 * d)
        path.cubicTo(w * 0.80f, g - 82 * d, w * 0.93f, g - 62 * d, w, g - 40 * d)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        fill.color = lit(colors[0])
        canvas.drawPath(path, fill)
        when (place) {
            Place.ORCHARD -> {
                tree(canvas, w * 0.11f, g - 50 * d, 1f, listOf(0xFF6FAE4C.toInt(), 0xFF5E9E40.toInt()), pears = true)
                tree(canvas, w * 0.26f, g - 70 * d, 0.7f, listOf(0xFF7DBA55.toInt(), 0xFF6FAE4C.toInt()), pears = true)
                tree(canvas, w * 0.88f, g - 62 * d, 1.1f, listOf(0xFF6FAE4C.toInt(), 0xFF5E9E40.toInt()), pears = true)
            }
            Place.AUTUMN -> {
                tree(canvas, w * 0.12f, g - 52 * d, 1f, listOf(0xFFE8833A.toInt(), 0xFFD9532F.toInt()), pears = false)
                tree(canvas, w * 0.27f, g - 70 * d, 0.72f, listOf(0xFFF2B33D.toInt(), 0xFFE8833A.toInt()), pears = false)
                tree(canvas, w * 0.87f, g - 64 * d, 1.1f, listOf(0xFFD9532F.toInt(), 0xFFF2B33D.toInt()), pears = false)
            }
            Place.SNOW -> {
                pine(canvas, w * 0.08f, g - 40 * d, 1f)
                pine(canvas, w * 0.18f, g - 58 * d, 0.8f)
                pine(canvas, w * 0.84f, g - 66 * d, 1.1f)
                pine(canvas, w * 0.94f, g - 50 * d, 0.85f)
            }
            else -> Unit
        }
    }

    private fun drawSea(canvas: Canvas, w: Float, h: Float, g: Float) {
        val horizon = g - 52 * d
        fill.color = lit(0xFF63C4EA.toInt())
        canvas.drawRect(0f, horizon, w, h, fill)
        fill.color = lit(0xFF49ADDA.toInt())
        canvas.drawRect(0f, horizon + 26 * d, w, h, fill)
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

    private fun drawFrontHill(canvas: Canvas, w: Float, h: Float, g: Float) {
        val colors = HILLS.getValue(place)
        path.reset()
        path.moveTo(0f, g + 40 * d)
        path.cubicTo(w * 0.18f, g + 8 * d, w * 0.32f, g, w * 0.5f, g)
        path.cubicTo(w * 0.68f, g, w * 0.82f, g + 6 * d, w, g + 34 * d)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        fill.color = lit(colors[1])
        canvas.drawPath(path, fill)
        // A lighter rim along the top, like light on plasticine.
        rim.reset()
        rim.moveTo(0f, g + 40 * d)
        rim.cubicTo(w * 0.18f, g + 8 * d, w * 0.32f, g, w * 0.5f, g)
        rim.cubicTo(w * 0.68f, g, w * 0.82f, g + 6 * d, w, g + 34 * d)
        stroke.color = lit(colors[2])
        stroke.strokeWidth = 4 * d
        canvas.drawPath(rim, stroke)
    }

    private fun drawPlaceLife(canvas: Canvas, w: Float, h: Float, g: Float) {
        val hidden = 1f - veil
        when (place) {
            Place.MEADOW -> {
                listOf(0.06f to 0xFFFFFFFF.toInt(), 0.15f to Palette.CORAL, 0.24f to Palette.SUN, 0.77f to Palette.LILAC, 0.86f to 0xFFFFFFFF.toInt(), 0.94f to Palette.CORAL)
                    .forEachIndexed { i, (fx, c) -> flower(canvas, w * fx, frontAt(fx, g) + (14 + (i % 3) * 7) * d, c) }
                butterfly(canvas, w, g, hidden)
            }
            Place.ORCHARD -> {
                listOf(0.07f, 0.2f, 0.83f, 0.95f).forEach { fx -> bush(canvas, w * fx, frontAt(fx, g) + 16 * d, 0xFF6FAE4C.toInt()) }
                butterfly(canvas, w, g, hidden)
            }
            Place.SEASIDE -> {
                parasol(canvas, w * 0.87f, frontAt(0.87f, g) + 10 * d)
                gulls(canvas, w, g, hidden)
            }
            Place.AUTUMN -> listOf(0.08f, 0.86f).forEach { fx -> bush(canvas, w * fx, frontAt(fx, g) + 16 * d, 0xFFC9792F.toInt()) }
            Place.SNOW -> Unit
        }
        // Petals, leaves and snowflakes, everywhere in the sky; fireflies by the ground at night.
        for (b in bits) {
            fill.color = b.color
            fill.alpha = (255 * hidden * if (place == Place.SNOW) 0.9f else 1f).toInt()
            val x = b.x * w
            val y = b.y * h
            if (place == Place.SNOW) {
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
        if (night > 0.4f && place != Place.SNOW) fireflies(canvas, w, g, hidden * (night - 0.4f) / 0.6f)
    }

    private fun tree(canvas: Canvas, x: Float, base: Float, s: Float, canopy: List<Int>, pears: Boolean) {
        val u = d * s
        fill.color = lit(0xFF8A5A3C.toInt())
        canvas.drawRoundRect(x - 3.5f * u, base - 26 * u, x + 3.5f * u, base + 4 * u, 3 * u, 3 * u, fill)
        fill.color = lit(canopy[1])
        canvas.drawCircle(x - 11 * u, base - 30 * u, 13 * u, fill)
        canvas.drawCircle(x + 11 * u, base - 31 * u, 13 * u, fill)
        fill.color = lit(canopy[0])
        canvas.drawCircle(x, base - 42 * u, 17 * u, fill)
        if (pears) {
            fill.color = lit(0xFFD8E163.toInt())
            listOf(-9f to -30f, 8f to -38f, 12f to -26f).forEach { (dx, dy) ->
                oval.set(x + (dx - 3.2f) * u, base + (dy - 4.2f) * u, x + (dx + 3.2f) * u, base + (dy + 4.2f) * u)
                canvas.drawOval(oval, fill)
            }
        }
    }

    private fun pine(canvas: Canvas, x: Float, base: Float, s: Float) {
        val u = d * s
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
            fill.color = lit(0xFFFFFFFF.toInt())
            path.reset()
            path.moveTo(x, top - 14 * u)
            path.lineTo(x + half * 0.42f, top - 6 * u)
            path.lineTo(x - half * 0.42f, top - 6 * u)
            path.close()
            canvas.drawPath(path, fill)
        }
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
        fill.color = lit(color)
        canvas.drawCircle(x - 9 * d, y, 8 * d, fill)
        canvas.drawCircle(x + 9 * d, y + 1 * d, 7 * d, fill)
        canvas.drawCircle(x, y - 5 * d, 10 * d, fill)
        fill.color = lit(blend(color, 0xFFFFFFFF.toInt(), 0.35f))
        canvas.drawCircle(x - 3 * d, y - 10 * d, 3 * d, fill)
    }

    private fun parasol(canvas: Canvas, x: Float, y: Float) {
        stroke.color = lit(0xFF8A5A3C.toInt())
        stroke.strokeWidth = 2.5f * d
        canvas.drawLine(x, y, x - 4 * d, y - 44 * d, stroke)
        oval.set(x - 30 * d, y - 62 * d, x + 22 * d, y - 30 * d)
        for (i in 0 until 4) {
            fill.color = lit(if (i % 2 == 0) Palette.CORAL else 0xFFFFFFFF.toInt())
            canvas.drawArc(oval, 180f + i * 45f, 45f, true, fill)
        }
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
        for (c in confetti) {
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
         * Today's place: the one chosen in Settings, or one of the season's, a different one each day. Winter has
         * snow, spring and summer the meadow and the sea, autumn its hills (northern seasons).
         */
        fun chosenPlace(context: Context, today: Calendar = Calendar.getInstance()): Place {
            val chosen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PLACE, AUTO)
            Place.entries.firstOrNull { it.name == chosen }?.let { return it }
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
