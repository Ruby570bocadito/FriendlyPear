package app.friendlypear.ui.peri

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Little bursts of joy around Peri: stars and dots that pop out, float up and fade, when she has an idea, gives
 * a thumbs up, gets happy or lands from her entrance. Drawn over her, never touchable; nothing runs between bursts.
 */
internal class Sparkles(context: Context) : View(context) {
    private class Bit(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Int,
        val star: Boolean,
        var spin: Float,
        val spinSpeed: Float,
    )

    private val bits = ArrayList<Bit>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var start = 0L
    private var last = 0L

    init {
        isClickable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** A burst from about her head: [count] bits flying up and out. */
    fun burst(count: Int = 16) {
        if (width == 0 || height == 0) return
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
                star = Random.nextInt(3) != 0,
                spin = Random.nextFloat() * 360f,
                spinSpeed = (Random.nextFloat() - 0.5f) * 540f,
            )
        }
        start = SystemClock.uptimeMillis()
        last = start
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        if (bits.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val dt = ((now - last).coerceIn(1, 50)) / 1000f
        last = now
        val age = (now - start) / LIFE_MS
        if (age >= 1f) {
            bits.clear()
            return
        }
        val gravity = 380f * resources.displayMetrics.density
        paint.alpha = 255
        for (b in bits) {
            b.vy += gravity * dt
            b.vx *= 0.985f
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.spin += b.spinSpeed * dt
            paint.color = b.color
            // Grow in quickly, fade out slowly.
            val scale = (age * 6f).coerceAtMost(1f)
            paint.alpha = (255 * (1f - age * age)).toInt()
            val r = b.size * scale
            if (b.star) drawStar(canvas, b.x, b.y, r, b.spin) else canvas.drawCircle(b.x, b.y, r * 0.55f, paint)
        }
        postInvalidateOnAnimation()
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
        val COLORS = listOf(Palette.GREEN, 0xFFFFC94D.toInt(), 0xFFFF8A80.toInt(), 0xFFFFFFFF.toInt(), 0xFFB7E07A.toInt())
    }
}
