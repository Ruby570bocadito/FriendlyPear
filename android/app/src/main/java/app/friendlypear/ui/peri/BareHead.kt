package app.friendlypear.ui.peri

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

/**
 * One of Peri's pictures with her head bare, for under a hat: the picture less the part with her leaf and stem, and over
 * it that part redrawn without them by Muse Image ([PeriTops]), its sides and bottom soft so no seam shows.
 */
internal class BareHead(private val picture: Drawable, private val top: Drawable, private val spec: PeriTops.Top) : Drawable() {
    override fun draw(canvas: Canvas) {
        val b = bounds
        val k = b.width() / SIDE
        canvas.save()
        canvas.clipOutRect(b.left + spec.clip[0] * k, b.top + spec.clip[1] * k, b.left + spec.clip[2] * k, b.top + spec.clip[3] * k)
        picture.draw(canvas)
        canvas.restore()
        // Placed here, not once: frames of one mood share the piece, each moved as far as her head moved.
        val box = spec.box
        top.setBounds(
            (b.left + box[0] * k).toInt(), (b.top + box[1] * k).toInt(),
            (b.left + (box[0] + box[2]) * k).toInt(), (b.top + (box[1] + box[3]) * k).toInt(),
        )
        top.draw(canvas)
    }

    override fun onBoundsChange(bounds: Rect) {
        picture.bounds = bounds
    }

    override fun getIntrinsicWidth() = picture.intrinsicWidth

    override fun getIntrinsicHeight() = picture.intrinsicHeight

    override fun setAlpha(alpha: Int) {
        picture.alpha = alpha
        top.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        picture.colorFilter = colorFilter
        top.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    private companion object {
        /** The pictures' square, in px: [PeriTops] measures in it. */
        const val SIDE = 1024f
    }
}
