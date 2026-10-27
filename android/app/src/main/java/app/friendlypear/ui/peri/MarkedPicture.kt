package app.friendlypear.ui.peri

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import app.friendlypear.net.PictureMark

/**
 * The picture the person showed Peri, fitted to the view, with a soft red ring around each risky thing SAM 3.1
 * found (a QR code to scan, for example) and a small label above it.
 */
class MarkedPicture(context: Context) : View(context) {
    private var bitmap: Bitmap? = null
    private var marks: List<Pair<PictureMark, String>> = emptyList()
    private val shown = RectF()
    private val around = RectF()
    private val chipBox = RectF()
    private val density = resources.displayMetrics.density
    private val photo = Paint(Paint.FILTER_BITMAP_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10 * density
        color = 0x66E5402B
        maskFilter = BlurMaskFilter(8 * density, BlurMaskFilter.Blur.NORMAL)
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4 * density
        color = 0xFFD7351F.toInt()
    }
    private val chip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD7351F.toInt() }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 16 * density
        typeface = weight(700)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun show(picture: Bitmap?, found: List<Pair<PictureMark, String>> = emptyList()) {
        bitmap = picture
        marks = found
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val b = bitmap ?: return
        val scale = minOf(width.toFloat() / b.width, height.toFloat() / b.height)
        val w = b.width * scale
        val h = b.height * scale
        shown.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2)
        canvas.drawBitmap(b, null, shown, photo)
        val pad = 8 * density
        marks.forEach { (m, name) ->
            val r = around.apply {
                set(shown.left + m.x1 * w - pad, shown.top + m.y1 * h - pad, shown.left + m.x2 * w + pad, shown.top + m.y2 * h + pad)
            }
            val corner = 18 * density
            canvas.drawRoundRect(r, corner, corner, glow)
            canvas.drawRoundRect(r, corner, corner, ring)
            val tw = label.measureText(name)
            chipBox.set(r.left, r.top - 30 * density, r.left + tw + 20 * density, r.top - 4 * density)
            if (chipBox.top < 0) chipBox.offset(0f, r.height() + 34 * density)
            canvas.drawRoundRect(chipBox, 12 * density, 12 * density, chip)
            canvas.drawText(name, chipBox.left + 10 * density, chipBox.bottom - 8 * density, label)
        }
    }
}
