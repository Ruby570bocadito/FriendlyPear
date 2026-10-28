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
 * or lands from her entrance; hearts that float up when she holds her heart; a sleepy "z z z" when she yawns or dozes;
 * and, when something goes well (a shield, a hello tap), a burst of chubby clay stars made with Muse Image that spring
 * out of her, bounce in the air and twinkle away. Drawn over her, never touchable; nothing runs between effects.
 */
internal class Sparkles(context: Context) : View(context) {
    enum class Effect { SPARKLE, HEARTS, SLEEPY, STARS }

    private enum class Shape { STAR, DOT, HEART, ZED, GOLD, LIME }

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
    )

    private val bits = ArrayList<Bit>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, 800, false) }
    private val path = Path()
    private var start = 0L
    private var last = 0L
    private var effect = Effect.SPARKLE
    private var life = LIFE_MS

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
        }
        life = when (effect) {
            Effect.SPARKLE -> LIFE_MS
            Effect.STARS -> STARS_LIFE_MS
            else -> FLOAT_LIFE_MS
        }
        start = SystemClock.uptimeMillis()
        last = start
        postInvalidateOnAnimation()
    }

    private fun sparkle(count: Int) {
        val d = resources.displayMetrics.density
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
        val d = resources.displayMetrics.density
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
                shape = if (i % 3 == 2) Shape.LIME else Shape.GOLD,
                spin = (Random.nextFloat() - 0.5f) * 40f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 260f,
                delay = Random.nextFloat() * 0.12f,
            )
        }
        repeat(count / 2) {
            val angle = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 1.4f).toFloat()
            val speed = (120 + Random.nextFloat() * 160) * d
            bits += Bit(cx, cy, cos(angle) * speed, sin(angle) * speed, (4 + Random.nextFloat() * 4) * d, Palette.WHITE, Shape.STAR, 0f, 200f, 0.15f)
        }
    }

    /** Six or seven hearts rising slowly from her chest, swaying a little as they go. */
    private fun hearts() {
        val d = resources.displayMetrics.density
        repeat(6 + Random.nextInt(2)) { i ->
            bits += Bit(
                x = width / 2f + (Random.nextFloat() - 0.5f) * width * 0.3f,
                y = height * 0.55f,
                vx = (Random.nextFloat() - 0.5f) * 50 * d,
                vy = -(70 + Random.nextFloat() * 60) * d,
                size = (7 + Random.nextFloat() * 6) * d,
                color = HEARTS.random(),
                shape = Shape.HEART,
                spin = Random.nextFloat() * 6.3f,
                spinSpeed = 2.5f + Random.nextFloat() * 2f,
                delay = i * 0.08f,
            )
        }
    }

    /** Three z's drifting up from beside her head, each a little bigger than the one before. */
    private fun sleepy() {
        val d = resources.displayMetrics.density
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
        // Sparkles fall; hearts and z's float up gently.
        val gravity = when (effect) {
            Effect.SPARKLE -> 380f
            Effect.STARS -> 520f
            else -> 0f
        } * resources.displayMetrics.density
        for (b in bits) {
            b.vy += gravity * dt
            b.vx *= 0.985f
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.spin += b.spinSpeed * dt
            val own = ((age - b.delay) / (1f - b.delay)).coerceAtMost(1f)
            if (own <= 0f) continue
            paint.color = b.color
            // Grow in quickly, fade out slowly; a clay star pops past its size and springs back, then twinkles.
            val sprite = b.shape == Shape.GOLD || b.shape == Shape.LIME
            val scale = if (sprite) pop(own) * (1f + 0.08f * sin(own * 28f)) else (own * 6f).coerceAtMost(1f)
            paint.alpha = (255 * (1f - own * own)).toInt()
            val r = b.size * scale
            when (b.shape) {
                Shape.STAR -> drawStar(canvas, b.x, b.y, r, b.spin)
                Shape.DOT -> canvas.drawCircle(b.x, b.y, r * 0.55f, paint)
                Shape.HEART -> drawHeart(canvas, b.x + sin(b.spin) * r * 0.6f, b.y, r)
                Shape.ZED -> {
                    paint.textSize = r * 2f
                    canvas.drawText("z", b.x, b.y, paint)
                }
                Shape.GOLD, Shape.LIME -> picture(if (b.shape == Shape.GOLD) R.drawable.peri_fx_star else R.drawable.peri_fx_star_green, b.size)?.let { bmp ->
                    canvas.save()
                    canvas.translate(b.x, b.y)
                    canvas.rotate(b.spin)
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

    /** The star sprites, decoded once at the size they're drawn and shared by every Peri on screen. */
    private fun picture(res: Int, size: Float): Bitmap? {
        val side = (size * 2).toInt().coerceAtLeast(8)
        val key = res * 31 + side
        PICTURES[key]?.let { return it }
        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, res)) { decoder, info, _ ->
                decoder.setTargetSize(side, side * info.size.height / info.size.width.coerceAtLeast(1))
            }
        }.getOrNull() ?: return null
        if (PICTURES.size > MAX_PICTURES) PICTURES.clear()
        PICTURES[key] = bitmap
        return bitmap
    }

    /** A plump little heart, its point down. */
    private fun drawHeart(canvas: Canvas, x: Float, y: Float, r: Float) {
        path.reset()
        path.moveTo(x, y + r * 0.9f)
        path.cubicTo(x - r * 1.6f, y - r * 0.1f, x - r * 0.7f, y - r * 1.3f, x, y - r * 0.45f)
        path.cubicTo(x + r * 0.7f, y - r * 1.3f, x + r * 1.6f, y - r * 0.1f, x, y + r * 0.9f)
        path.close()
        canvas.drawPath(path, paint)
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
        const val MAX_PICTURES = 24
        val PICTURES = HashMap<Int, Bitmap>()
        val COLORS = listOf(Palette.GREEN, 0xFFFFC94D.toInt(), 0xFFFF8A80.toInt(), 0xFFFFFFFF.toInt(), 0xFFB7E07A.toInt())
        val HEARTS = listOf(0xFFFF8A80.toInt(), 0xFFF26B6B.toInt(), 0xFFFFB3A7.toInt())
    }
}
