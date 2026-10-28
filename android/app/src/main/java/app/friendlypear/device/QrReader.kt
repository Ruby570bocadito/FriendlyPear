package app.friendlypear.device

import android.graphics.Bitmap
import app.friendlypear.core.shield.QrTraps
import app.friendlypear.net.PictureMark
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import com.google.zxing.qrcode.QRCodeReader

/**
 * The QR codes in a picture, read on the phone (ZXing, offline): what each holds and where it is, never opened or
 * followed. What they hold is judged by [QrTraps]; only the picture goes to be looked at, as before. Every code found
 * is judged, so a trap can't hide behind a few harmless ones.
 */
object QrReader {
    /** What a code holds (cut to what a QR code can hold) and its box on the picture, from 0 to 1. */
    data class Found(val text: String, val mark: PictureMark)

    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8",
    )

    fun read(bitmap: Bitmap): List<Found> = runCatching {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        // A code printed light on dark too: phone cameras read those, so a trap could hide that way.
        decode(source).ifEmpty { decode(source.invert()) }.map { r ->
            Found(r.text.orEmpty().take(QrTraps.MAX_PAYLOAD + 1), box(r, w, h))
        }
    }.getOrDefault(emptyList())

    /** Several codes if there are; one, read harder, if the multi-reader finds none. */
    private fun decode(source: LuminanceSource): List<Result> {
        val image = BinaryBitmap(HybridBinarizer(source))
        return runCatching { QRCodeMultiReader().decodeMultiple(image, hints).toList() }.getOrNull()
            ?: runCatching { listOf(QRCodeReader().decode(image, hints)) }.getOrDefault(emptyList())
    }

    /** The finder points sit inside the code's corners: a quarter more on each side rings all of it. */
    private fun box(r: Result, w: Int, h: Int): PictureMark {
        val points = r.resultPoints.orEmpty().filterNotNull()
        if (points.isEmpty()) return PictureMark("qr", 0f, 0f, 1f, 1f)
        val xs = points.map { it.x }
        val ys = points.map { it.y }
        val pad = (xs.max() - xs.min()).coerceAtLeast(ys.max() - ys.min()) * 0.25f
        return PictureMark(
            "qr",
            ((xs.min() - pad) / w).coerceIn(0f, 1f),
            ((ys.min() - pad) / h).coerceIn(0f, 1f),
            ((xs.max() + pad) / w).coerceIn(0f, 1f),
            ((ys.max() + pad) / h).coerceIn(0f, 1f),
        )
    }
}
