package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * FriendlyPear's look, shared by every screen: cream and pear green like Peri's illustrations, big rounded
 * shapes and ink text. Every text colour here has at least 7:1 contrast on its background (docs/DISENO.md).
 */
internal object Palette {
    const val CREAM = 0xFFFFF8EC.toInt()
    /** Bottom of the screen's background: the cream with a hint of green. */
    const val MEADOW = 0xFFEAF4DC.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val INK = 0xFF1F2A1A.toInt()
    const val INK_SOFT = 0xFF3F4D39.toInt()
    const val GREEN = 0xFF7CC242.toInt()
    /** Buttons: white text on it is 8.9:1, and it is 7.4:1 on [GREEN_TINT]. */
    const val GREEN_DARK = 0xFF25541A.toInt()
    const val GREEN_TINT = 0xFFE1F0CF.toInt()
    const val AMBER_TINT = 0xFFFFEBC7.toInt()
    const val AMBER_INK = 0xFF6B3E00.toInt()
    /** "Put away" target when Peri is over it: white on it is 7.6:1. */
    const val DANGER = 0xFF9C1F17.toInt()
    const val SHADOW = 0x33000000
    const val RIPPLE_LIGHT = 0x33FFFFFF
    const val RIPPLE_DARK = 0x1F25541A
}

internal fun Context.dp(value: Number): Int = (value.toFloat() * resources.displayMetrics.density).toInt()

/**
 * Text that grows with the phone's font size, up to [MAX_FONT_SCALE]: the sizes here already start large, and with
 * the phone's own "large text" on top, the words would push Peri and the buttons off the screen.
 */
internal fun TextView.sizeSp(sp: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp * minOf(resources.configuration.fontScale, MAX_FONT_SCALE))
}

internal const val MAX_FONT_SCALE = 1.15f

/** One-line text that shrinks (down to [minSp]) until it fits its width. */
internal fun TextView.fitWidth(baseSp: Float, minSp: Float) {
    val room = width - paddingLeft - paddingRight
    if (room <= 0) return
    var sp = baseSp
    sizeSp(sp)
    while (paint.measureText(text.toString()) > room && sp > minSp) {
        sp -= 1f
        sizeSp(sp)
    }
}

/**
 * Shrinks the text, from [baseSp] down to [minSp], until it fits in [maxHeightPx]: a long sentence of Peri's
 * gets a little smaller instead of squeezing Peri herself. Call it once the view has its width.
 */
internal fun TextView.fitInto(maxHeightPx: Int, baseSp: Float, minSp: Float = 15f) {
    val w = width.takeIf { it > 0 } ?: return
    val widthSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
    val free = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    var sp = baseSp
    sizeSp(sp)
    measure(widthSpec, free)
    while (measuredHeight > maxHeightPx && sp > minSp) {
        sp -= 1f
        sizeSp(sp)
        measure(widthSpec, free)
    }
}

/** Roboto at a given weight: 400 body, 600 buttons, 800 the wordmark. */
internal fun weight(w: Int): Typeface = Typeface.create(Typeface.DEFAULT, w, false)

internal fun rounded(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = radius
    if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
}

internal fun pressable(background: Drawable, ripple: Int): Drawable = RippleDrawable(ColorStateList.valueOf(ripple), background, null)

/**
 * A big pill button with its icon right beside the label, both centred; [primary] is the one main action of a
 * screen. It reads as a button to TalkBack, and its label can change (e.g. while the microphone is open).
 */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal class Pill(context: Context, label: CharSequence, icon: Int, primary: Boolean, size: Float = 20f) : LinearLayout(context) {
    private val text = TextView(context)

    var label: CharSequence
        get() = text.text
        set(value) {
            text.text = value
            contentDescription = value
        }

    init {
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        val fg = if (primary) Palette.WHITE else Palette.GREEN_DARK
        background = pressable(
            rounded(if (primary) Palette.GREEN_DARK else Palette.GREEN_TINT, context.dp(40).toFloat()),
            if (primary) Palette.RIPPLE_LIGHT else Palette.RIPPLE_DARK,
        )
        elevation = if (primary) context.dp(3).toFloat() else 0f
        setPadding(context.dp(18), 0, context.dp(18), 0)
        val s = context.dp(if (primary) 30 else 24)
        addView(ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(fg)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(s, s).apply { marginEnd = context.dp(12) })
        addView(text.apply {
            sizeSp(size)
            typeface = weight(600)
            setTextColor(fg)
            isSingleLine = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        this.label = label
    }

    private val baseSp = size
    private val iconRoom = context.dp(if (primary) 30 else 24) + context.dp(12)

    /** The label shrinks to fit beside its icon (large text on the phone) instead of being cut off. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val total = MeasureSpec.getSize(widthMeasureSpec)
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED && total > 0) {
            val room = total - paddingLeft - paddingRight - iconRoom
            var sp = baseSp
            text.sizeSp(sp)
            while (sp > 12f && text.paint.measureText(text.text.toString()) > room) {
                sp -= 1f
                text.sizeSp(sp)
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun getAccessibilityClassName(): CharSequence = Button::class.java.name
}

internal fun Context.heading(value: CharSequence, size: Float = 24f) = TextView(this).apply {
    text = value
    sizeSp(size)
    typeface = weight(700)
    setTextColor(Palette.INK)
}

internal fun Context.body(value: CharSequence, size: Float = 18f) = TextView(this).apply {
    text = value
    sizeSp(size)
    setTextColor(Palette.INK_SOFT)
    setLineSpacing(0f, 1.15f)
}

/** A white card with soft corners and a gentle shadow. */
internal fun Context.cardBackground(): Drawable = rounded(Palette.WHITE, dp(24).toFloat())
