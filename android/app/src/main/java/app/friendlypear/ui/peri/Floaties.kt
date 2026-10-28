package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import app.friendlypear.R
import kotlin.math.sin
import kotlin.random.Random

/**
 * Little hearts, clay stars and sparkles that drift up slowly around a section's Peri, in the section's colours, and
 * fade as they rise. At Peri's calm frame rate, and only while they can be seen; still with "remove animations".
 */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal class Floaties(context: Context, private val decor: Decor) : View(context) {
    enum class Shape { HEART, STAR, SPARKLE }

    /** What floats around each section's Peri, and in which colours. */
    enum class Decor(val shapes: List<Shape>, val colors: IntArray) {
        DIARY(listOf(Shape.HEART, Shape.HEART, Shape.SPARKLE), intArrayOf(Palette.CORAL, 0xFFFF9EB5.toInt(), Palette.SUN)),
        LEARN(listOf(Shape.STAR, Shape.SPARKLE, Shape.SPARKLE), intArrayOf(Palette.SUN, Palette.SKY, Palette.WHITE)),
        FAMILY(listOf(Shape.HEART, Shape.HEART, Shape.SPARKLE), intArrayOf(Palette.LILAC, Palette.CORAL, 0xFFFFD6E0.toInt())),
    }

    private class Bit(var x: Float, var y: Float, var rise: Float, var size: Float, var phase: Float, var shape: Shape, var color: Int, var spin: Float)

    private val moves = context.animationsOn()
    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val heart = Path()
    private val sparkle = Path()
    private val box = RectF()
    private val bits = ArrayList<Bit>()
    private var star: Bitmap? = null
    private var running = false
    private var lastSlot = -1L
    private var last = 0L

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        // A unit heart (1 wide), its point at the bottom.
        heart.moveTo(0f, 0.32f)
        heart.cubicTo(-0.55f, -0.05f, -0.42f, -0.52f, 0f, -0.26f)
        heart.cubicTo(0.42f, -0.52f, 0.55f, -0.05f, 0f, 0.32f)
        heart.close()
        // A four-pointed twinkle.
        sparkle.moveTo(0f, -0.5f)
        sparkle.quadTo(0.08f, -0.08f, 0.5f, 0f)
        sparkle.quadTo(0.08f, 0.08f, 0f, 0.5f)
        sparkle.quadTo(-0.08f, 0.08f, -0.5f, 0f)
        sparkle.quadTo(-0.08f, -0.08f, 0f, -0.5f)
        sparkle.close()
        if (Shape.STAR in decor.shapes) {
            star = runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, R.drawable.peri_fx_star)) { decoder, _, _ ->
                    decoder.setTargetSize((40 * d).toInt(), (40 * d).toInt())
                }
            }.getOrNull()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bits.clear()
        repeat(COUNT) { bits += newBit(Random.nextFloat()) }
    }

    private fun newBit(heightShare: Float): Bit {
        val shape = decor.shapes.random()
        return Bit(
            // Around Peri on the right of the card, never over its words.
            x = width * (0.6f + Random.nextFloat() * 0.38f),
            y = height * (0.25f + heightShare * 0.85f),
            rise = (9f + Random.nextFloat() * 10f) * d,
            size = (if (shape == Shape.STAR) 13f else 9f + Random.nextFloat() * 7f) * d,
            phase = Random.nextFloat() * 6.3f,
            shape = shape,
            color = decor.colors.random(),
            spin = (Random.nextFloat() - 0.5f) * 30f,
        )
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val now = SystemClock.uptimeMillis()
            val slot = Rest.slot(frameTimeNanos, Rest.calmFrameMs(now))
            if (slot != lastSlot) {
                lastSlot = slot
                val dt = ((now - last).coerceIn(1, 100)) / 1000f
                last = now
                for (i in bits.indices) {
                    val b = bits[i]
                    b.y -= b.rise * dt
                    if (b.y < -b.size) bits[i] = newBit(1f).also { it.y = height + it.size }
                }
                invalidate()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && moves && !running) {
            running = true
            last = SystemClock.uptimeMillis()
            Choreographer.getInstance().postFrameCallback(frame)
        } else if (!isVisible && running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(frame)
        }
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val t = SystemClock.uptimeMillis() / 1000f
        for (b in bits) {
            // Fading in at the bottom and out towards the top.
            val share = (b.y / height).coerceIn(0f, 1f)
            val fade = (minOf(share / 0.35f, (1f - share) / 0.2f)).coerceIn(0f, 1f)
            if (fade <= 0f) continue
            val sway = if (moves) sin(t * 1.3f + b.phase) * 6 * d else 0f
            canvas.save()
            canvas.translate(b.x + sway, b.y)
            canvas.rotate(b.spin + if (moves) sin(t + b.phase) * 8f else 0f)
            paint.alpha = 255
            when (b.shape) {
                Shape.HEART -> {
                    paint.color = b.color
                    paint.alpha = (230 * fade).toInt()
                    canvas.save()
                    canvas.scale(b.size, b.size)
                    canvas.drawPath(heart, paint)
                    canvas.restore()
                    // A little shine on its top left, like clay.
                    paint.color = Palette.WHITE
                    paint.alpha = (150 * fade).toInt()
                    box.set(-b.size * 0.3f, -b.size * 0.3f, -b.size * 0.1f, -b.size * 0.14f)
                    canvas.drawOval(box, paint)
                }
                Shape.STAR -> star?.let { bmp ->
                    paint.alpha = (255 * fade).toInt()
                    val s = b.size / bmp.width * 2f
                    canvas.scale(s, s)
                    canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, paint)
                }
                Shape.SPARKLE -> {
                    paint.color = b.color
                    paint.alpha = (220 * fade * (0.6f + 0.4f * sin(t * 3f + b.phase))).toInt().coerceIn(0, 255)
                    canvas.scale(b.size, b.size)
                    canvas.drawPath(sparkle, paint)
                }
            }
            canvas.restore()
        }
    }

    private companion object {
        const val COUNT = 8
    }
}
