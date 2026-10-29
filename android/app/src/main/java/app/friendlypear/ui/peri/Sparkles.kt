package app.friendlypear.ui.peri

import app.friendlypear.R
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Little effects around Peri: stars and dots that pop out and fall when she has an idea, gives a thumbs up, gets happy
 * or lands from her entrance; clay hearts that float up when she holds her heart; a sleepy "z z z" when she yawns or
 * dozes; and, when something goes well (a shield, a hello tap), a burst of chubby clay stars that spring out of her,
 * bounce in the air and twinkle away. The rest are clay too, made with Muse Image (lote 6): music notes while she hums,
 * a sweat drop when she worries, a question mark while she thinks, an exclamation mark when she warns, a light bulb for an
 * idea, confetti for a party, a tear when she's sad, and a puff of dust where she lands. Drawn over her, never
 * touchable; nothing runs between effects.
 */
internal class Sparkles(context: Context) : View(context) {
    enum class Effect { SPARKLE, HEARTS, SLEEPY, STARS, NOTES, SWEAT, QUESTION, ALARM, IDEA, CONFETTI, TEAR, HEARTBREAK, DUST }

    private enum class Shape { STAR, DOT, ZED, SPRITE, PUFF }

    private class Bit(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Int,
        val shape: Shape,
        var spin: Float,
        val spinSpeed: Float,
        /** Part of the effect's life to wait before showing up, so the z's come one after another. */
        val delay: Float = 0f,
        /** The clay picture it is, for [Shape.SPRITE]. */
        val picture: Int = 0,
        /** How far it rocks (°) and sways (px) as it goes: a question mark wobbles, a note dances. */
        val wobble: Float = 0f,
        val sway: Float = 0f,
    )

    private val bits = ArrayList<Bit>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, 800, false) }
    private val path = Path()
    private var start = 0L
    private var last = 0L
    private var effect = Effect.SPARKLE
    private var life = LIFE_MS
    private val d get() = resources.displayMetrics.density

    /** Where her feet are (px), for the dust she raises landing; 0 for near the bottom. */
    var feetY = 0f

    /** An effect still on show: a landing's dust waits rather than cut a mood's sign short. */
    val busy: Boolean get() = bits.isNotEmpty()

    init {
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** A burst from about her head: [count] bits flying up and out. */
    fun burst(count: Int = 16) = play(Effect.SPARKLE, count)

    fun play(effect: Effect, count: Int = 16) {
        if (width == 0 || height == 0) return
        bits.clear()
        this.effect = effect
        when (effect) {
            Effect.SPARKLE -> sparkle(count)
            Effect.HEARTS -> hearts()
            Effect.SLEEPY -> sleepy()
            Effect.STARS -> stars(count)
            Effect.NOTES -> notes()
            Effect.SWEAT -> one(R.drawable.peri_fx_sweat_drop, width * 0.67f, height * 0.33f, 17f, vy = 22f)
            Effect.QUESTION -> {
                one(R.drawable.peri_fx_question, width * 0.72f, height * 0.13f, 26f, wobble = 14f)
                one(R.drawable.peri_fx_question, width * 0.82f, height * 0.2f, 15f, wobble = 18f, delay = 0.18f)
            }
            Effect.ALARM -> one(R.drawable.peri_fx_exclamation, width * 0.7f, height * 0.12f, 28f, wobble = 10f)
            Effect.IDEA -> {
                one(R.drawable.peri_fx_bulb, width * 0.5f, height * 0.07f, 30f, vy = -8f, wobble = 6f)
                twinkles(width * 0.5f, height * 0.07f)
            }
            Effect.CONFETTI -> confetti(count.coerceAtLeast(20))
            Effect.TEAR -> one(R.drawable.peri_fx_tear, width * 0.42f, height * 0.49f, 11f, vy = 10f)
            Effect.HEARTBREAK -> one(R.drawable.peri_fx_heart_broken, width * 0.7f, height * 0.15f, 22f, vy = -10f, wobble = 8f)
            Effect.DUST -> dust()
        }
        life = when (effect) {
            Effect.SPARKLE -> LIFE_MS
            Effect.STARS -> STARS_LIFE_MS
            Effect.DUST -> DUST_LIFE_MS
            Effect.ALARM, Effect.SWEAT, Effect.TEAR -> SHORT_LIFE_MS
            else -> FLOAT_LIFE_MS
        }
        start = SystemClock.uptimeMillis()
        last = start
        postInvalidateOnAnimation()
    }

    /** One clay picture at [x], [y], [size] dp, popping in and then going its way ([vy] dp a second) with a wobble. */
    private fun one(picture: Int, x: Float, y: Float, size: Float, vy: Float = 0f, wobble: Float = 0f, delay: Float = 0f) {
        bits += Bit(x, y, 0f, vy * d, size * d, Palette.WHITE, Shape.SPRITE, 0f, 0f, delay, picture, wobble)
    }

    private fun sparkle(count: Int) {
        val cx = width / 2f
        val cy = height * 0.32f
        repeat(count) {
            val angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 1.3f).toFloat()
            val speed = (140 + Random.nextFloat() * 220) * d
            bits += Bit(
                x = cx + (Random.nextFloat() - 0.5f) * width * 0.25f,
                y = cy + (Random.nextFloat() - 0.5f) * height * 0.1f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = (4 + Random.nextFloat() * 6) * d,
                color = COLORS.random(),
                shape = if (Random.nextInt(3) != 0) Shape.STAR else Shape.DOT,
                spin = Random.nextFloat() * 360f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 540f,
            )
        }
    }

    /**
     * Clay stars springing out of her middle in a fan, most of them upwards, with a few white twinkles among them;
     * they fall back a little, turning, and fade.
     */
    private fun stars(count: Int) {
        val cx = width / 2f
        val cy = height * 0.48f
        repeat(count) { i ->
            val angle = (-PI / 2 + (i / (count - 1f) - 0.5f) * PI * 1.5f + (Random.nextFloat() - 0.5f) * 0.3f).toFloat()
            val speed = (230 + Random.nextFloat() * 170) * d
            bits += Bit(
                x = cx,
                y = cy,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = (15 + Random.nextFloat() * 9) * d,
                color = Palette.WHITE,
                shape = Shape.SPRITE,
                spin = (Random.nextFloat() - 0.5f) * 40f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 260f,
                delay = Random.nextFloat() * 0.12f,
                picture = if (i % 3 == 2) R.drawable.peri_fx_star_green else R.drawable.peri_fx_star,
            )
        }
        repeat(count / 2) {
            val angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 1.4f).toFloat()
            val speed = (120 + Random.nextFloat() * 160) * d
            bits += Bit(cx, cy, cos(angle) * speed, sin(angle) * speed, (4 + Random.nextFloat() * 4) * d, Palette.WHITE, Shape.STAR, 0f, 200f, 0.15f)
        }
    }

    /** Six or seven clay hearts, coral and pink, rising slowly from her chest and swaying a little as they go. */
    private fun hearts() {
        repeat(6 + Random.nextInt(2)) { i ->
            bits += Bit(
                x = width / 2f + (Random.nextFloat() - 0.5f) * width * 0.3f,
                y = height * 0.55f,
                vx = (Random.nextFloat() - 0.5f) * 50 * d,
                vy = -(70 + Random.nextFloat() * 60) * d,
                size = (10 + Random.nextFloat() * 7) * d,
                color = Palette.WHITE,
                shape = Shape.SPRITE,
                spin = (Random.nextFloat() - 0.5f) * 30f,
                spinSpeed = 0f,
                delay = i * 0.08f,
                picture = if (i % 2 == 0) R.drawable.peri_fx_heart_coral else R.drawable.peri_fx_heart_pink,
                wobble = 12f,
                sway = 8 * d,
            )
        }
    }

    /** Three z's drifting up from beside her head, each a little bigger than the one before. */
    private fun sleepy() {
        repeat(3) { i ->
            bits += Bit(
                x = width * 0.62f,
                y = height * 0.3f,
                vx = 22 * d,
                vy = -38 * d,
                size = (9 + i * 4) * d,
                color = Palette.INK_SOFT,
                shape = Shape.ZED,
                spin = 0f,
                spinSpeed = 0f,
                delay = i * 0.22f,
            )
        }
    }

    /** Three music notes leaving her mouth one after another, dancing upwards to the side. */
    private fun notes() {
        repeat(3) { i ->
            bits += Bit(
                x = width * 0.6f,
                y = height * 0.5f,
                vx = (34 + i * 6) * d,
                vy = -(58 + i * 8) * d,
                size = (13 + (i % 2) * 5) * d,
                color = Palette.WHITE,
                shape = Shape.SPRITE,
                spin = -10f + i * 10f,
                spinSpeed = 0f,
                delay = i * 0.2f,
                picture = R.drawable.peri_fx_music_note,
                wobble = 16f,
                sway = 7 * d,
            )
        }
    }

    /** Clay confetti, green strips and yellow dots with a few clay hearts, bursting up from her and fluttering down. */
    private fun confetti(count: Int) {
        val cx = width / 2f
        val cy = height * 0.42f
        repeat(count) { i ->
            val angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 1.25f).toFloat()
            val speed = (240 + Random.nextFloat() * 200) * d
            bits += Bit(
                x = cx + (Random.nextFloat() - 0.5f) * width * 0.15f,
                y = cy,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = (7 + Random.nextFloat() * 6) * d,
                color = Palette.WHITE,
                shape = Shape.SPRITE,
                spin = Random.nextFloat() * 360f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 720f,
                delay = Random.nextFloat() * 0.1f,
                picture = CONFETTI[i % CONFETTI.size],
                sway = 10 * d,
            )
        }
    }

    /** A few white twinkles around a light bulb as it lights up. */
    private fun twinkles(x: Float, y: Float) {
        repeat(5) { i ->
            val angle = (i / 5f * 2 * PI).toFloat()
            bits += Bit(x, y, cos(angle) * 60 * d, sin(angle) * 60 * d, (4 + Random.nextFloat() * 3) * d, 0xFFFFF4C2.toInt(), Shape.STAR, 0f, 180f, 0.12f)
        }
    }

    /** Soft puffs of dust from under her feet, rolling outwards as they swell and fade: she just landed. */
    private fun dust() {
        val y = if (feetY > 0f) feetY else height * 0.9f
        repeat(4) { i ->
            val side = if (i % 2 == 0) -1f else 1f
            bits += Bit(
                x = width / 2f + side * width * (0.06f + i / 2 * 0.06f),
                y = y,
                vx = side * (70 + Random.nextFloat() * 50) * d,
                vy = -(8 + Random.nextFloat() * 10) * d,
                size = (7 + Random.nextFloat() * 4) * d,
                color = DUST,
                shape = Shape.PUFF,
                spin = 0f,
                spinSpeed = 0f,
                delay = (i / 2) * 0.08f,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (bits.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val dt = ((now - last).coerceIn(1, 50)) / 1000f
        last = now
        val age = (now - start) / life
        if (age >= 1f) {
            bits.clear()
            return
        }
        // Sparkles, stars, confetti and tears fall; hearts, notes and z's float; marks and bulbs hang in the air.
        val gravity = when (effect) {
            Effect.SPARKLE -> 380f
            Effect.STARS -> 520f
            Effect.CONFETTI -> 300f
            // A tear rolls down her cheek before it falls.
            Effect.TEAR -> 160f
            Effect.SWEAT -> 60f
            else -> 0f
        } * d
        for (b in bits) {
            b.vy += gravity * dt
            b.vx *= if (effect == Effect.DUST) 0.93f else 0.985f
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.spin += b.spinSpeed * dt
            val own = ((age - b.delay) / (1f - b.delay)).coerceAtMost(1f)
            if (own <= 0f) continue
            paint.color = b.color
            paint.alpha = (255 * (1f - own * own)).toInt()
            when (b.shape) {
                Shape.STAR -> drawStar(canvas, b.x, b.y, b.size * (own * 6f).coerceAtMost(1f), b.spin)
                Shape.DOT -> canvas.drawCircle(b.x, b.y, b.size * (own * 6f).coerceAtMost(1f) * 0.55f, paint)
                Shape.ZED -> {
                    paint.textSize = b.size * (own * 6f).coerceAtMost(1f) * 2f
                    canvas.drawText("z", b.x, b.y, paint)
                }
                // A puff swells as it rolls away and thins out.
                Shape.PUFF -> {
                    paint.alpha = (150 * (1f - own)).toInt()
                    canvas.drawCircle(b.x, b.y, b.size * (0.6f + own * 1.1f), paint)
                }
                // A clay picture pops past its size and springs back, then wobbles, sways and fades as it goes.
                Shape.SPRITE -> picture(b.picture, b.size)?.let { bmp ->
                    val scale = pop(own) * (1f + 0.06f * sin(own * 24f))
                    canvas.save()
                    canvas.translate(b.x + b.sway * sin(own * 9f), b.y)
                    canvas.rotate(b.spin + b.wobble * sin(own * 18f))
                    canvas.scale(scale, scale)
                    canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, paint)
                    canvas.restore()
                }
            }
        }
        postInvalidateOnAnimation()
    }

    /** 0 to a little past 1 and back, in the first fifth of its life: the pop of a clay star. */
    private fun pop(t: Float): Float {
        val k = (t / 0.2f).coerceIn(0f, 1f)
        return k * (1f + 0.35f * sin(k * PI.toFloat()))
    }

    /** The clay pictures, decoded once at the size they're drawn and shared by every Peri on screen. */
    private fun picture(res: Int, size: Float): Bitmap? {
        val side = (size * 2).toInt().coerceAtLeast(8)
        val key = res * 31 + side
        PICTURES[key]?.let { return it }
        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, res)) { decoder, info, _ ->
                // The longer side at [side]: a tall exclamation mark is as tall as a round heart is wide.
                val longer = maxOf(info.size.width, info.size.height).coerceAtLeast(1)
                decoder.setTargetSize(maxOf(1, side * info.size.width / longer), maxOf(1, side * info.size.height / longer))
            }
        }.getOrNull() ?: return null
        if (PICTURES.size > MAX_PICTURES) PICTURES.clear()
        PICTURES[key] = bitmap
        return bitmap
    }

    /** A soft four-point star, like a twinkle. */
    private fun drawStar(canvas: Canvas, x: Float, y: Float, r: Float, angle: Float) {
        path.reset()
        val inner = r * 0.32f
        for (i in 0 until 8) {
            val rr = if (i % 2 == 0) r else inner
            val a = Math.toRadians((angle + i * 45).toDouble())
            val px = x + (rr * cos(a)).toFloat()
            val py = y + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private companion object {
        const val LIFE_MS = 1_300f
        const val FLOAT_LIFE_MS = 2_200f
        const val STARS_LIFE_MS = 1_700f
        const val SHORT_LIFE_MS = 1_500f
        const val DUST_LIFE_MS = 650f
        const val MAX_PICTURES = 40
        val PICTURES = HashMap<Int, Bitmap>()
        val COLORS = listOf(Palette.GREEN, 0xFFFFC94D.toInt(), 0xFFFF8A80.toInt(), 0xFFFFFFFF.toInt(), 0xFFB7E07A.toInt())
        val CONFETTI = listOf(R.drawable.peri_fx_confetti_strip, R.drawable.peri_fx_confetti_dot, R.drawable.peri_fx_confetti_strip,
            R.drawable.peri_fx_confetti_dot, R.drawable.peri_fx_heart_pink, R.drawable.peri_fx_star)
        const val DUST = 0xFFF4EBDD.toInt()
    }
}
