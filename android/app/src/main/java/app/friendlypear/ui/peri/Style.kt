package app.friendlypear.ui.peri

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.StateListAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.SystemClock
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.provider.Settings
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * FriendlyPear's look, shared by every screen: Peri's world. Pear green and forest for what you press, sun, sky,
 * lilac and coral for the sections and for what needs care, white cards and cream. Buttons and cards are soft
 * rubber, with a lip underneath that they sink onto when pressed. Every text colour here has at least 7:1
 * contrast on its background (docs/DISENO.md).
 */
internal object Palette {
    const val CREAM = 0xFFFFF8EC.toInt()
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
    /** The edge of a white card on the cream: just enough to see where it ends. */
    const val LINE = 0xFFEDE4D0.toInt()
    /** The edge of a white button, one step stronger than a card's. */
    const val LINE_STRONG = 0xFFD8CCB2.toInt()
    const val SHADOW = 0x33000000
    const val RIPPLE_LIGHT = 0x33FFFFFF
    const val RIPPLE_DARK = 0x1F25541A

    /** The lips under the rubber buttons and cards: what they sink onto. */
    const val FOREST_LIP = 0xFF173A10.toInt()
    const val SAND_LIP = 0xFFE3D7BD.toInt()
    const val CARD_LIP = 0xFFEADFC8.toInt()

    /** The sections' colours, each with a tint for backgrounds and an ink for text on that tint (7:1 or more). */
    const val SUN = 0xFFFFC83D.toInt()
    const val SUN_TINT = 0xFFFFF1C9.toInt()
    const val SUN_LIP = 0xFFDDA52A.toInt()
    const val SKY = 0xFF6EC6FF.toInt()
    const val SKY_TINT = 0xFFDDF1FF.toInt()
    const val SKY_INK = 0xFF1E4A7A.toInt()
    const val LILAC = 0xFFA98BFF.toInt()
    const val LILAC_TINT = 0xFFECE5FF.toInt()
    const val LILAC_INK = 0xFF4A2F99.toInt()
    const val CORAL = 0xFFFF8A7A.toInt()
    const val CORAL_TINT = 0xFFFFE2DC.toInt()
    const val CORAL_INK = 0xFF8A2B1F.toInt()
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

/** Three sizes for every screen: titles, everything you read or press, and the small print. */
internal object TextSize {
    const val TITLE = 26f
    const val BODY = 20f
    const val SMALL = 16f
}

/**
 * The two typefaces, both OFL and inside the app (res/font, licences in assets/licenses): Fredoka, round and
 * friendly like Peri, for titles, buttons and chips; Atkinson Hyperlegible, made by the Braille Institute for people
 * with low vision, for everything you read (it's also the theme's default). Fredoka is one variable file: each
 * weight is built from it once.
 */
internal object Fonts {
    private val display = HashMap<Int, Typeface>()
    private val reading = HashMap<Boolean, Typeface>()

    /** Builds every weight once, at app start; until then (never, in practice) the phone's own font stands in. */
    @Synchronized
    fun init(context: Context) {
        val res = context.applicationContext.resources
        listOf(500, 600, 700).forEach { weight ->
            runCatching {
                val font = Font.Builder(res, R.font.fredoka)
                    .setFontVariationSettings("'wght' $weight, 'wdth' 100")
                    .setWeight(weight)
                    .build()
                Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).setSystemFallback("sans-serif").build()
            }.onSuccess { display[weight] = it }
        }
        listOf(false, true).forEach { bold ->
            runCatching { Typeface.create(res.getFont(R.font.atkinson), if (bold) 700 else 400, false) }.onSuccess { reading[bold] = it }
        }
    }

    /** Fredoka at 500, 600 or 700, whichever is closest to [w]. */
    @Synchronized
    fun display(w: Int): Typeface {
        val weight = when {
            w <= 500 -> 500
            w < 700 -> 600
            else -> 700
        }
        return display[weight] ?: Typeface.create(Typeface.DEFAULT, weight, false)
    }

    @Synchronized
    fun reading(bold: Boolean = false): Typeface = reading[bold] ?: Typeface.create(Typeface.DEFAULT, if (bold) 700 else 400, false)
}

/**
 * The typeface for a weight, as the screens ask for it: what you read (up to 500) in Atkinson Hyperlegible, what
 * you press and titles (600 and up) in Fredoka.
 */
internal fun weight(w: Int): Typeface = if (w >= 600) Fonts.display(w) else Fonts.reading()

/** The ground of the screens around Peri's: the light sky of the hour, fading into cream. */
internal fun ground(): Drawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, PeriWorld.lightSky())

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

internal fun rounded(color: Int, radius: Float, strokeColor: Int = 0, strokeWidth: Int = 0) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = radius
    if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
}

internal fun pressable(background: Drawable, ripple: Int): Drawable = RippleDrawable(ColorStateList.valueOf(ripple), background, null)

internal fun Context.animationsOn(): Boolean = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

/**
 * A soft rubber shape: a rounded face resting on a darker lip [lipPx] tall. [press] (0 to 1) sinks the face onto
 * the lip, which is how buttons and tiles show a touch. Its colours can change, and a [ring] inside the face's edge
 * marks what's chosen.
 */
internal class RubberDrawable(face: Int, lip: Int, private val lipPx: Float, private val radius: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private var alphaValue = 255

    var face = face
        set(value) {
            field = value
            invalidateSelf()
        }
    var lip = lip
        set(value) {
            field = value
            invalidateSelf()
        }
    /** 0 for none. */
    var ring = 0
        set(value) {
            field = value
            invalidateSelf()
        }
    var ringWidth = 0f

    var press = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val r = minOf(radius, (b.height() - lipPx) / 2f)
        paint.color = lip
        paint.alpha = alphaValue
        rect.set(b.left.toFloat(), b.top + lipPx, b.right.toFloat(), b.bottom.toFloat())
        canvas.drawRoundRect(rect, r, r, paint)
        val down = lipPx * press
        paint.color = face
        paint.alpha = alphaValue
        rect.set(b.left.toFloat(), b.top + down, b.right.toFloat(), b.bottom - lipPx + down)
        canvas.drawRoundRect(rect, r, r, paint)
        if (ring != 0 && ringWidth > 0f) {
            ringPaint.color = ring
            ringPaint.alpha = alphaValue
            ringPaint.strokeWidth = ringWidth
            rect.inset(ringWidth / 2f, ringWidth / 2f)
            canvas.drawRoundRect(rect, r - ringWidth / 2f, r - ringWidth / 2f, ringPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        alphaValue = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** The rubber colours a button can have: its face, its lip and the ink of its label (7:1 or more on the face). */
internal enum class Rubber(val face: Int, val lip: Int, val ink: Int) {
    PRIMARY(Palette.GREEN_DARK, Palette.FOREST_LIP, Palette.WHITE),
    PLAIN(Palette.WHITE, Palette.SAND_LIP, Palette.GREEN_DARK),
    GREEN(Palette.GREEN_TINT, 0xFFB7D798.toInt(), Palette.GREEN_DARK),
    CORAL(Palette.CORAL_TINT, 0xFFEFB4A8.toInt(), Palette.CORAL_INK),
    SUN(Palette.SUN, Palette.SUN_LIP, Palette.INK),
    SKY(Palette.SKY_TINT, 0xFFA9D5F2.toInt(), Palette.SKY_INK),
    LILAC(Palette.LILAC_TINT, 0xFFCDBEF5.toInt(), Palette.LILAC_INK),
}

/**
 * A big pill button with its icon right beside the label, both centred: soft rubber that sinks onto its lip when
 * pressed and springs back with a little bounce. [primary] is the one main action of a screen, filled forest
 * green; the others are white ([rubber] picks another colour). A [trailing] icon goes after the label ("Next >").
 * It reads as a button to TalkBack, and its label can change (e.g. while the microphone is open).
 */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal class Pill(
    context: Context,
    label: CharSequence,
    icon: Int,
    primary: Boolean,
    size: Float = TextSize.BODY,
    trailing: Boolean = false,
    rubber: Rubber = if (primary) Rubber.PRIMARY else Rubber.PLAIN,
    /** The same icon in Rive, made for this button's colours; [icon] stands in with animations off. */
    private val art: RiveArt? = null,
) : LinearLayout(context) {
    private val artView = art?.let { context.rive(it, 0f) }
    private val text = TextView(context)
    private val lip = context.dp(LIP_DP).toFloat()
    private val shape = RubberDrawable(rubber.face, rubber.lip, lip, PILL_RADIUS)
    private var down = false
    private var sink: ValueAnimator? = null

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
        val fg = rubber.ink
        background = shape
        // The label sits on the face, above the lip.
        setPadding(context.dp(18), 0, context.dp(18), lip.toInt())
        val s = context.dp(if (primary) 28 else 24)
        val image = artView ?: ImageView(context).apply {
            setImageResource(icon)
            setColorFilter(fg)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        if (!trailing) addView(image, LayoutParams(s, s).apply { marginEnd = context.dp(12) })
        addView(text.apply {
            sizeSp(size)
            typeface = Fonts.display(600)
            setTextColor(fg)
            isSingleLine = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        })
        if (trailing) addView(image, LayoutParams(s, s).apply { marginStart = context.dp(10) })
        this.label = label
    }

    /** Moves the Rive icon on to [value], if it has one. */
    fun art(value: Float) {
        art?.let { artView?.show(it, value) }
    }

    private val baseSp = size
    private val iconRoom = context.dp(if (primary) 28 else 24) + context.dp(12)

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

    /** Pressed, the face sinks onto its lip with the label; let go, it comes back up with a small bounce. */
    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val pressed = isPressed && isEnabled
        if (pressed == down) return
        down = pressed
        sink?.cancel()
        if (!context.animationsOn()) return setSink(if (pressed) 1f else 0f)
        sink = ValueAnimator.ofFloat(shape.press, if (pressed) 1f else 0f).apply {
            duration = if (pressed) 60 else 160
            interpolator = DecelerateInterpolator()
            addUpdateListener { setSink(it.animatedValue as Float) }
            start()
        }
        if (!pressed) {
            scaleX = 0.97f
            scaleY = 0.97f
            animate().scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(OvershootInterpolator(3f)).start()
        }
    }

    private fun setSink(value: Float) {
        shape.press = value
        for (i in 0 until childCount) getChildAt(i).translationY = lip * value
    }

    override fun performClick(): Boolean {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        return super.performClick()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
    }

    override fun getAccessibilityClassName(): CharSequence = Button::class.java.name

    private companion object {
        const val LIP_DP = 5
    }
}

/** Every button is fully round: decided once, for the whole app. */
private const val PILL_RADIUS = 999f

internal fun Context.heading(value: CharSequence, size: Float = TextSize.TITLE) = TextView(this).apply {
    text = value
    sizeSp(size)
    typeface = Fonts.display(600)
    setTextColor(Palette.INK)
    // TalkBack can jump from title to title.
    isAccessibilityHeading = true
}

internal fun Context.body(value: CharSequence, size: Float = TextSize.BODY) = TextView(this).apply {
    text = value
    sizeSp(size)
    typeface = Fonts.reading()
    setTextColor(Palette.INK_SOFT)
    setLineSpacing(0f, 1.15f)
}

/**
 * Android's dialogs in Peri's look, called right after `show()`: cream with soft corners, and buttons in sentence
 * case at the body size (never small capitals). A [risky] button (a "yes" to something dangerous) is in the
 * warning colour, so the safe answer is the one that stands out.
 */
internal fun android.app.AlertDialog.friendly(risky: Int? = null, riskyColor: Int = Palette.DANGER): android.app.AlertDialog {
    window?.setBackgroundDrawable(rounded(Palette.CREAM, context.dp(24).toFloat()))
    listOf(android.app.AlertDialog.BUTTON_POSITIVE, android.app.AlertDialog.BUTTON_NEGATIVE, android.app.AlertDialog.BUTTON_NEUTRAL).forEach { which ->
        getButton(which)?.apply {
            isAllCaps = false
            sizeSp(TextSize.BODY)
            typeface = Fonts.display(if (which == risky) 500 else 600)
            setTextColor(if (which == risky) riskyColor else Palette.GREEN_DARK)
        }
    }
    return this
}

/** A white rubber card with soft corners, resting on a thin sand lip. */
internal fun Context.cardBackground(): Drawable = RubberDrawable(Palette.WHITE, Palette.CARD_LIP, dp(4).toFloat(), dp(26).toFloat())

/** A tinted rubber card, for a section's own colour (the diary's sun, the quiz's sky). */
internal fun Context.tintedCard(face: Int, lip: Int): Drawable = RubberDrawable(face, lip, dp(4).toFloat(), dp(26).toFloat())

/** Tiles and cards you can press sink a little and spring back. */
internal fun pressScale(): StateListAnimator = StateListAnimator().apply {
    // The state list animator sets each animator's target (the view) itself.
    fun scale(to: Float, ms: Long) = ObjectAnimator().apply {
        setValues(PropertyValuesHolder.ofFloat(View.SCALE_X, to), PropertyValuesHolder.ofFloat(View.SCALE_Y, to))
        duration = ms
    }
    addState(intArrayOf(android.R.attr.state_pressed), scale(0.96f, 90))
    addState(intArrayOf(), scale(1f, 260).apply { interpolator = OvershootInterpolator(2.5f) })
}

/**
 * A gentle entrance: the view rises into place as it fades in. With animations off it just appears.
 * Stagger [delay] so a screen's pieces arrive one after another, like Peri setting the table.
 */
internal fun View.floatIn(delay: Long = 0L) {
    if (!context.animationsOn()) return
    alpha = 0f
    translationY = context.dp(14).toFloat()
    animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(340)
        .setInterpolator(DecelerateInterpolator(1.5f)).start()
}

/**
 * Shows or hides a card. Coming in, it rises a little and settles with a soft spring, so the eye follows it; going,
 * it simply leaves. Nothing moves with "remove animations".
 */
internal fun View.reveal(show: Boolean) {
    val was = visibility == View.VISIBLE
    if (show == was) return
    animate().cancel()
    if (!show) {
        visibility = View.GONE
        return
    }
    visibility = View.VISIBLE
    if (!context.animationsOn()) return
    alpha = 0f
    translationY = context.dp(22).toFloat()
    scaleX = 0.97f
    scaleY = 0.97f
    animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(380)
        .setInterpolator(android.view.animation.OvershootInterpolator(1.1f)).start()
}

/**
 * A sticker of Peri that's alive: while it's on screen she bobs and sways a little from her feet, slowly, and a tap
 * makes her hop (then [onTap] runs: another line of hers, say). With "remove animations" she stays still, and the
 * tap still works.
 */
internal fun View.alive(onTap: (() -> Unit)? = null) {
    // A sticker, not a field: no grey focus square around her when the phone is driven by keys.
    defaultFocusHighlightEnabled = false
    val lift = context.dp(4).toFloat()
    val jump = context.dp(20).toFloat()
    var hopAt = -HOP_MS
    if (context.animationsOn()) {
        val loop = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3_400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val a = (it.animatedValue as Float) * 2f * PI.toFloat()
                val since = SystemClock.uptimeMillis() - hopAt
                val hop = if (since in 0 until HOP_MS) sin(PI * since / HOP_MS).toFloat() * jump else 0f
                pivotX = width / 2f
                pivotY = height * 0.92f
                translationY = -abs(sin(a)) * lift - hop
                rotation = sin(a) * 2.2f
            }
        }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = loop.start()
            override fun onViewDetachedFromWindow(v: View) = loop.cancel()
        })
        if (isAttachedToWindow) loop.start()
    }
    setOnClickListener {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        hopAt = SystemClock.uptimeMillis()
        onTap?.invoke()
    }
}

private const val HOP_MS = 460L

/** A playful entrance for small things (a sticker, a chip, a medal): it pops up from small with a bounce. */
internal fun View.popIn(delay: Long = 0L) {
    if (!context.animationsOn()) return
    scaleX = 0.4f
    scaleY = 0.4f
    alpha = 0f
    animate().scaleX(1f).scaleY(1f).alpha(1f).setStartDelay(delay).setDuration(420)
        .setInterpolator(OvershootInterpolator(2.6f)).start()
}
