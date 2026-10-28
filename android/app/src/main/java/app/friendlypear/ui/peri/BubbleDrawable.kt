package app.friendlypear.ui.peri

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * A speech bubble with a small tail pointing at Peri: up to her in the app, down to her in Mini Peri, and to the
 * left where she stands beside it (the game "Scam or not?").
 * Its outline follows the shape, so where the bubble floats (Mini Peri, over other apps) its shadow matches.
 */
class BubbleDrawable(
    private val fill: Int,
    private val stroke: Int,
    private val strokeWidth: Float,
    private val radius: Float,
    private val tailSize: Float,
    private val tail: Tail,
    /** For the bottom tails: how far the tail's tip is from that side, so it points at Peri's middle. */
    private val tailInset: Float = 0f,
) : Drawable() {
    enum class Tail { TOP, BOTTOM_START, BOTTOM_END, LEFT }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = stroke
        style = Paint.Style.STROKE
        strokeWidth = this@BubbleDrawable.strokeWidth
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val half = strokeWidth / 2
        val body = RectF(
            bounds.left + half + if (tail == Tail.LEFT) tailSize else 0f,
            bounds.top + half + if (tail == Tail.TOP) tailSize else 0f,
            bounds.right - half,
            bounds.bottom - half - if (tail == Tail.BOTTOM_START || tail == Tail.BOTTOM_END) tailSize else 0f,
        )
        path.reset()
        path.addRoundRect(body, radius, radius, Path.Direction.CW)
        val tip = Path()
        when (tail) {
            Tail.TOP -> {
                val x = body.centerX()
                tip.moveTo(x - tailSize, body.top + 1)
                tip.lineTo(x, body.top - tailSize)
                tip.lineTo(x + tailSize, body.top + 1)
            }
            Tail.BOTTOM_START, Tail.BOTTOM_END -> {
                val x = if (tail == Tail.BOTTOM_START) body.left + tailInset else body.right - tailInset
                tip.moveTo(x - tailSize, body.bottom - 1)
                tip.lineTo(x, body.bottom + tailSize)
                tip.lineTo(x + tailSize, body.bottom - 1)
            }
            Tail.LEFT -> {
                val y = body.centerY()
                tip.moveTo(body.left + 1, y - tailSize)
                tip.lineTo(body.left - tailSize, y)
                tip.lineTo(body.left + 1, y + tailSize)
            }
        }
        tip.close()
        path.op(tip, Path.Op.UNION)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, fillPaint)
        if (strokeWidth > 0) canvas.drawPath(path, strokePaint)
    }

    override fun getOutline(outline: Outline) {
        outline.setPath(path)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
