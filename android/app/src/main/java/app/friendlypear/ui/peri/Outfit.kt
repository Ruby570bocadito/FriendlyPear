package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.util.LruCache
import android.view.View
import app.friendlypear.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * What Peri wears: one thing on her head, one on her eyes and one below her smile. Each is a Muse Image accessory
 * (assets/peri/generate.ts `wear`) laid over her in every pose and every frame, placed from where her eyes are in
 * that very picture ([PeriAnchors]) and turned with her head. Chosen in her wardrobe, kept on this phone.
 */
internal data class Outfit(val head: Item? = null, val eyes: Item? = null, val neck: Item? = null) {
    enum class Slot { HEAD, EYES, NECK }

    /**
     * An accessory: its picture and name, how wide it is in eye distances, which point of the picture ([ax], [ay],
     * from 0 to 1) goes where, and where that is ([dx], [dy] eye distances from between her eyes, turned with her).
     */
    enum class Item(val slot: Slot, val picture: Int, val label: Int, val width: Float, val ax: Float, val ay: Float, val dx: Float, val dy: Float) {
        STRAW(Slot.HEAD, R.drawable.peri_wear_straw, R.string.wear_straw, 1.95f, 0.5f, 0.8f, 0f, -1.02f),
        BEANIE(Slot.HEAD, R.drawable.peri_wear_beanie, R.string.wear_beanie, 1.45f, 0.5f, 0.9f, 0f, -0.92f),
        CROWN(Slot.HEAD, R.drawable.peri_wear_crown, R.string.wear_crown, 1.25f, 0.5f, 0.92f, 0f, -1.08f),
        FLOWERS(Slot.HEAD, R.drawable.peri_wear_flowers, R.string.wear_flowers, 1.55f, 0.5f, 0.75f, 0f, -0.98f),
        BOW(Slot.HEAD, R.drawable.peri_wear_bow, R.string.wear_bow, 0.72f, 0.5f, 0.5f, 0.06f, -1.22f),
        SUNGLASSES(Slot.EYES, R.drawable.peri_wear_sunglasses, R.string.wear_sunglasses, 1.85f, 0.5f, 0.5f, 0f, 0f),
        HEARTS(Slot.EYES, R.drawable.peri_wear_hearts, R.string.wear_hearts, 1.93f, 0.5f, 0.5f, 0f, 0f),
        GLASSES(Slot.EYES, R.drawable.peri_wear_glasses, R.string.wear_glasses, 1.91f, 0.5f, 0.5f, 0f, 0f),
        BOWTIE(Slot.NECK, R.drawable.peri_wear_bowtie, R.string.wear_bowtie, 0.9f, 0.5f, 0.5f, 0f, 0.78f),
        TIE(Slot.NECK, R.drawable.peri_wear_tie, R.string.wear_tie, 0.46f, 0.5f, 0.04f, 0f, 0.6f),
        BANDANA(Slot.NECK, R.drawable.peri_wear_bandana, R.string.wear_bandana, 0.85f, 0.5f, 0.05f, 0f, 0.66f),
    }

    val items: List<Item> get() = listOfNotNull(head, eyes, neck)

    fun with(slot: Slot, item: Item?): Outfit = when (slot) {
        Slot.HEAD -> copy(head = item)
        Slot.EYES -> copy(eyes = item)
        Slot.NECK -> copy(neck = item)
    }

    fun on(slot: Slot): Item? = when (slot) {
        Slot.HEAD -> head
        Slot.EYES -> eyes
        Slot.NECK -> neck
    }

    /** As it travels and is kept: "STRAW,,BOWTIE". */
    fun code(): String = listOf(head, eyes, neck).joinToString(",") { it?.name.orEmpty() }

    companion object {
        private const val PREFS = "outfit"
        private const val KEY = "wearing"

        fun of(code: String?): Outfit {
            val parts = code.orEmpty().split(",")
            fun item(i: Int, slot: Slot) = Item.entries.firstOrNull { it.name == parts.getOrNull(i) && it.slot == slot }
            return Outfit(item(0, Slot.HEAD), item(1, Slot.EYES), item(2, Slot.NECK))
        }

        fun load(context: Context): Outfit = of(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, ""))

        fun save(context: Context, outfit: Outfit) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, outfit.code()).apply()
        }

        fun random(): Outfit = Outfit(
            Item.entries.filter { it.slot == Slot.HEAD }.random(),
            Item.entries.filter { it.slot == Slot.EYES }.plus(listOf(null)).random(),
            Item.entries.filter { it.slot == Slot.NECK }.plus(listOf(null)).random(),
        )

        /**
         * Moods and poses that already have something there, so that slot stays bare: a nightcap or a party hat, a
         * magnifying glass or her hands on her eyes, or something she holds against her chest (her hands, a watch, a
         * heart, a letter, a medal, a book, a scarf). Asleep, her glasses come off too.
         */
        val BUSY: Map<Int, Set<Slot>> = mapOf(
            R.drawable.peri_listening to setOf(Slot.NECK),
            R.drawable.peri_thinking to setOf(Slot.EYES, Slot.NECK),
            R.drawable.peri_waiting to setOf(Slot.NECK),
            R.drawable.peri_sleeping to setOf(Slot.HEAD, Slot.EYES),
            R.drawable.peri_calm_love to setOf(Slot.NECK),
            R.drawable.peri_calm_letter to setOf(Slot.NECK),
            R.drawable.peri_calm_medal to setOf(Slot.NECK),
            R.drawable.peri_calm_clap to setOf(Slot.NECK),
            R.drawable.peri_calm_detective to setOf(Slot.HEAD, Slot.EYES),
            R.drawable.peri_calm_party to setOf(Slot.HEAD),
            R.drawable.peri_calm_peekaboo to setOf(Slot.EYES),
            R.drawable.peri_calm_glasses to setOf(Slot.EYES, Slot.NECK),
            R.drawable.peri_calm_scarf to setOf(Slot.NECK),
        )
    }
}

/**
 * Draws Peri's outfit over her picture, inside her rig so it tilts, bobs and squashes with her. [anchor] is the frame
 * on show, whose eyes place everything (a step moves her up to 35 px); [base] is its mood's still image or the pose,
 * which says what's bare.
 */
@SuppressLint("ViewConstructor")
internal class OutfitLayer(context: Context) : View(context) {
    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    var outfit = Outfit()
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var anchor = R.drawable.peri_calm
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var base = R.drawable.peri_calm
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        if (outfit.items.isEmpty() || width == 0 || height == 0) return
        val eyes = PeriAnchors.EYES[anchor] ?: PeriAnchors.EYES[base] ?: PeriAnchors.EYES.getValue(R.drawable.peri_calm)
        val busy = Outfit.BUSY[base].orEmpty()
        // Her picture is a square drawn centred (fit) in this view, at 1024 px.
        val side = minOf(width, height).toFloat()
        val left = (width - side) / 2
        val top = (height - side) / 2
        val k = side / 1024f
        val lx = left + eyes[0] * k
        val ly = top + eyes[1] * k
        val rx = left + eyes[2] * k
        val ry = top + eyes[3] * k
        val d = hypot(rx - lx, ry - ly)
        val angle = atan2(ry - ly, rx - lx)
        val cx = (lx + rx) / 2
        val cy = (ly + ry) / 2
        for (item in outfit.items) {
            if (item.slot in busy) continue
            val w = item.width * d
            val picture = Pictures.get(context, item.picture, w) ?: continue
            val h = w * picture.height / picture.width.coerceAtLeast(1)
            val px = cx + cos(angle) * item.dx * d - sin(angle) * item.dy * d
            val py = cy + sin(angle) * item.dx * d + cos(angle) * item.dy * d
            canvas.save()
            canvas.translate(px, py)
            canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
            rect.set(-item.ax * w, -item.ay * h, (1 - item.ax) * w, (1 - item.ay) * h)
            canvas.drawBitmap(picture, null, rect, paint)
            canvas.restore()
        }
    }

    /**
     * The accessories' pictures, decoded at the width they're drawn (in steps of 64 px) and shared by every Peri on
     * screen: a pear's card in the family section doesn't hold a 512 px hat. A few megabytes at most.
     */
    private object Pictures {
        private val cache = object : LruCache<String, Bitmap>(CACHE_KB) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount / 1024
        }

        fun get(context: Context, res: Int, width: Float): Bitmap? {
            val step = ((width / STEP).toInt() + 1) * STEP
            val key = "$res:$step"
            cache.get(key)?.let { return it }
            val bitmap = runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.resources, res)) { decoder, info, _ ->
                    if (info.size.width > step) decoder.setTargetSize(step, step * info.size.height / info.size.width)
                }
            }.getOrNull() ?: return null
            cache.put(key, bitmap)
            return bitmap
        }

        private const val STEP = 64
        private const val CACHE_KB = 6 * 1024
    }
}
