package app.friendlypear.ui.peri

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import app.friendlypear.R
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Peri by layers, shared by every Peri on screen: where her face goes on each body (the eyes of the picture the body was
 * made from, [PeriAnchors]), the pieces decoded once at the size she's drawn (a body or an eye piece in use by several
 * frames, or several Peris, is decoded once), and how a body, her eyes, her mouth and what she holds in front of her face
 * are drawn, in the 1024 px space of her pictures.
 */
internal object PeriLayers {
    /** Where her face goes on a body: the middle of its eyes, its turn (°) and its size against the calm body's. */
    class Place(val x: Float, val y: Float, val turn: Float, val scale: Float) {
        fun towards(other: Place, f: Float) =
            Place(x + (other.x - x) * f, y + (other.y - y) * f, turn + (other.turn - turn) * f, scale + (other.scale - scale) * f)
    }

    private val CALM = PeriAnchors.EYES.getValue(R.drawable.peri_calm)
    val CALM_X = (CALM[0] + CALM[2]) / 2
    val CALM_Y = (CALM[1] + CALM[3]) / 2
    val WHOLE = floatArrayOf(0f, 0f, PeriLayerData.SIZE, PeriLayerData.SIZE)

    /** How far her face turns and her eyes move looking all the way to a side (px of the 1024 px picture). */
    const val FACE_RUN_X = 6f
    const val FACE_RUN_Y = 4f
    const val EYES_RUN_X = 7f
    const val EYES_RUN_Y = 3f

    private val places: Map<String, Place> = PeriLayerData.BODIES.mapValues { (_, ids) ->
        val e = PeriAnchors.EYES[ids.second] ?: CALM
        val turn = atan2(e[3] - e[1], e[2] - e[0]) - atan2(CALM[3] - CALM[1], CALM[2] - CALM[0])
        Place((e[0] + e[2]) / 2, (e[1] + e[3]) / 2, Math.toDegrees(turn.toDouble()).toFloat(), hypot(e[2] - e[0], e[3] - e[1]) / hypot(CALM[2] - CALM[0], CALM[3] - CALM[1]))
    }

    fun place(body: String): Place = places[body] ?: places.getValue("calm")

    private val decoded = HashMap<String, WeakReference<Bitmap>>()

    /**
     * A piece decoded for a Peri [side] px wide (its 1024 px picture scaled alike), shared while anyone draws it. Safe on
     * the decoding thread: PeriFace decodes the next mood's frames in the background.
     */
    fun bitmap(resources: Resources, id: Int, side: Int): Bitmap {
        val key = "$id@$side"
        synchronized(decoded) { decoded[key]?.get()?.let { return it } }
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, id)) { decoder, info, _ ->
            val k = side / PeriLayerData.SIZE
            if (k < 1f) decoder.setTargetSize(maxOf(1, (info.size.width * k).toInt()), maxOf(1, (info.size.height * k).toInt()))
        }
        synchronized(decoded) {
            decoded.values.removeAll { it.get() == null }
            decoded[key] = WeakReference(bitmap)
        }
        return bitmap
    }

    /** Draws [bitmap] over [box] (left, top, right, bottom, in the 1024 px space). */
    fun draw(canvas: Canvas, bitmap: Bitmap, box: FloatArray, paint: Paint, rect: RectF) {
        rect.set(box[0], box[1], box[2], box[3])
        canvas.drawBitmap(bitmap, null, rect, paint)
    }

    /**
     * Her face on a body at [place]: turned [faceX], [faceY] (-1 to 1) towards what she looks at, her eyes moving a
     * little more ([eyesX], [eyesY]); the mouth goes over the eyes (its edge gives way to them, see layers.py).
     */
    fun drawFace(canvas: Canvas, place: Place, eyes: Bitmap?, mouth: Bitmap?, faceX: Float, faceY: Float, eyesX: Float, eyesY: Float, paint: Paint, rect: RectF) {
        canvas.save()
        canvas.translate(place.x, place.y)
        canvas.rotate(place.turn)
        canvas.scale(place.scale, place.scale)
        canvas.translate(-CALM_X + faceX * FACE_RUN_X, -CALM_Y + faceY * FACE_RUN_Y)
        if (eyes != null) {
            canvas.save()
            canvas.translate(eyesX * EYES_RUN_X, eyesY * EYES_RUN_Y)
            draw(canvas, eyes, PeriLayerData.EYES_BOX, paint, rect)
            canvas.restore()
        }
        if (mouth != null) draw(canvas, mouth, PeriLayerData.MOUTH_BOX, paint, rect)
        canvas.restore()
    }
}

/**
 * One of PeriFace's frames drawn by layers ([PeriLayerData.FRAMES]): the body, her eyes and mouth where that body's
 * picture had its eyes, and what she holds in front of her face (a cup, a heart with her hands, the umbrella's handle)
 * over them, so a mouth never rubs half of it out. It takes the place of the frame's whole picture anywhere: the same
 * 1024 px square, the same feet, the same eyes for her outfit. Its pieces are decoded for a Peri [side] px wide.
 */
internal class LayeredFrame(resources: Resources, frame: PeriLayerData.Frame, side: Int) : Drawable() {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()
    private val body = PeriLayers.bitmap(resources, PeriLayerData.BODIES.getValue(frame.body).first, side)
    private val eyes = (PeriLayerData.EYES[frame.eyes] ?: PeriLayerData.EYES.getValue("open")).let { PeriLayers.bitmap(resources, it, side) }
    private val mouth = (PeriLayerData.MOUTHS[frame.mouth] ?: PeriLayerData.MOUTHS.getValue("smile")).let { PeriLayers.bitmap(resources, it, side) }
    private val front = PeriLayerData.FRONTS[frame.body]?.let { (id, box) -> PeriLayers.bitmap(resources, id, side) to box }
    private val place = PeriLayers.place(frame.body)
    private val look = frame.look

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(b.width() / PeriLayerData.SIZE, b.height() / PeriLayerData.SIZE)
        PeriLayers.draw(canvas, body, PeriLayers.WHOLE, paint, rect)
        PeriLayers.drawFace(canvas, place, eyes, mouth, look, 0f, look, 0f, paint, rect)
        front?.let { (bitmap, box) -> PeriLayers.draw(canvas, bitmap, box, paint, rect) }
        canvas.restore()
    }

    override fun getIntrinsicWidth() = PeriLayerData.SIZE.toInt()

    override fun getIntrinsicHeight() = PeriLayerData.SIZE.toInt()

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
