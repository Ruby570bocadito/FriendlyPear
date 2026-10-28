package app.friendlypear.ui.peri

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewOutlineProvider
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import app.friendlypear.R

/*
 * Choosing in Peri's look instead of Android's grey radio buttons and checkboxes: rubber tiles that sink when
 * pressed, the chosen one pear green with a forest ring and a tick (colour, shape and TalkBack's "selected" all say
 * it), and a big rubber switch.
 */

/**
 * One option: its [label] and, above it, a picture of what it is ([preview]: a little world for a place); without
 * one it's a row, with an [icon] before the label. Chosen, it turns pear green with a forest ring and a tick.
 */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal class ChoiceTile(context: Context, label: CharSequence, preview: View? = null, icon: Int = 0) : FrameLayout(context) {
    private val lip = context.dp(4)
    private val shape = RubberDrawable(Palette.WHITE, Palette.SAND_LIP, lip.toFloat(), context.dp(22).toFloat()).apply {
        ringWidth = context.dp(3).toFloat()
    }
    private val iconView = if (icon != 0 && preview == null) ImageView(context) else null
    private val tick = ImageView(context)

    var chosen = false
        set(value) {
            field = value
            shape.face = if (value) Palette.GREEN_TINT else Palette.WHITE
            shape.lip = if (value) GREEN_LIP else Palette.SAND_LIP
            shape.ring = if (value) Palette.GREEN_DARK else 0
            tick.visibility = if (value) View.VISIBLE else View.INVISIBLE
            iconView?.setColorFilter(if (value) Palette.GREEN_DARK else Palette.INK_SOFT)
            isSelected = value
        }

    init {
        background = shape
        isClickable = true
        isFocusable = true
        stateListAnimator = pressScale()
        contentDescription = label
        val tile = preview != null
        val pad = context.dp(10)
        val tickSize = context.dp(30)
        val content = LinearLayout(context).apply {
            orientation = if (tile) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = if (tile) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            // Everything sits on the face, above the lip; a row leaves room for its tick.
            setPadding(if (tile) pad else context.dp(18), pad, if (tile) pad else context.dp(18) + tickSize, pad + lip)
        }
        if (preview != null) {
            val radius = context.dp(16).toFloat()
            preview.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) = outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
            preview.clipToOutline = true
            content.addView(preview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, context.dp(PREVIEW_DP)))
        }
        iconView?.let {
            it.setImageResource(icon)
            it.setColorFilter(Palette.INK_SOFT)
            content.addView(it, LinearLayout.LayoutParams(context.dp(26), context.dp(26)).apply { marginEnd = context.dp(12) })
        }
        content.addView(TextView(context).apply {
            text = label
            sizeSp(if (tile) 17f else TextSize.BODY)
            typeface = Fonts.display(600)
            setTextColor(Palette.INK)
            gravity = if (tile) Gravity.CENTER else Gravity.START or Gravity.CENTER_VERTICAL
            if (tile) setPadding(0, context.dp(8), 0, context.dp(2))
        }, if (tile) LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        else LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, if (tile) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT))
        // The tick: a forest circle with a white check, in a tile's corner or at the end of a row.
        tick.apply {
            setImageResource(R.drawable.ic_check)
            setColorFilter(Palette.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Palette.GREEN_DARK)
            }
            val p = context.dp(5)
            setPadding(p, p, p, p)
            visibility = View.INVISIBLE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(tick, LayoutParams(tickSize, tickSize, if (tile) Gravity.TOP or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.END).apply {
            if (tile) {
                topMargin = context.dp(4)
                marginEnd = context.dp(4)
            } else {
                marginEnd = context.dp(14)
                bottomMargin = lip
            }
        })
        if (!tile) minimumHeight = context.dp(64)
    }

    override fun performClick(): Boolean {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        return super.performClick()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
    }

    /** TalkBack hears a radio button: the label, and whether it's the chosen one. */
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = RadioButton::class.java.name
        info.isCheckable = true
        info.isChecked = chosen
    }

    companion object {
        const val PREVIEW_DP = 96
        private const val GREEN_LIP = 0xFFB7D798.toInt()
    }
}

/**
 * One choice among [ChoiceTile]s, [columns] to a row.
 * A tap chooses and calls [onChoose], also on the chosen one again (a voice plays its sample once more).
 */
@SuppressLint("ViewConstructor")
internal class ChoiceGroup(context: Context, private val columns: Int, private val onChoose: (String) -> Unit) : LinearLayout(context) {
    private val tiles = LinkedHashMap<String, ChoiceTile>()
    private var row: LinearLayout? = null

    init {
        orientation = VERTICAL
        clipChildren = false
        clipToPadding = false
    }

    fun add(key: String, tile: ChoiceTile) {
        var current = row
        if (current == null || current.childCount >= columns) {
            val first = childCount == 0
            current = LinearLayout(context).apply {
                clipChildren = false
                clipToPadding = false
            }
            addView(current, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { if (!first) topMargin = context.dp(10) })
            row = current
        }
        val beside = current.childCount > 0
        current.addView(tile, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            if (beside) marginStart = context.dp(10)
        })
        tiles[key] = tile
        tile.setOnClickListener {
            choose(key)
            onChoose(key)
        }
    }

    /** Marks [key] as the chosen one without calling [onChoose]: how things stand when the screen opens. */
    fun choose(key: String) = tiles.forEach { (k, tile) -> tile.chosen = k == key }

    /** A short last row keeps its tiles as wide as the others'. */
    fun done() {
        val last = row ?: return
        repeat(columns - last.childCount) {
            last.addView(View(context), LayoutParams(0, 1, 1f).apply { marginStart = context.dp(10) })
        }
    }
}

/**
 * A big rubber switch with its label: on, the track turns forest green and the knob slides across with a little
 * bounce. TalkBack hears a switch, on or off.
 */
@SuppressLint("ViewConstructor")
internal class Toggle(context: Context, label: CharSequence, on: Boolean, private val onChange: (Boolean) -> Unit) : LinearLayout(context) {
    var on = on
        private set
    private val knob = Knob(context)
    private var slide: ValueAnimator? = null

    init {
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        minimumHeight = context.dp(64)
        contentDescription = label
        addView(TextView(context).apply {
            text = label
            sizeSp(TextSize.BODY)
            typeface = Fonts.display(600)
            setTextColor(Palette.INK)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = context.dp(14) })
        addView(knob, LayoutParams(context.dp(66), context.dp(40)))
        knob.position = if (on) 1f else 0f
        setOnClickListener { flip() }
    }

    /** Sets it without calling onChange: when the answer comes from elsewhere (the server). */
    fun set(value: Boolean) {
        if (value == on) return
        on = value
        slide?.cancel()
        knob.position = if (value) 1f else 0f
    }

    private fun flip() {
        on = !on
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        slide?.cancel()
        val target = if (on) 1f else 0f
        if (!context.animationsOn()) knob.position = target
        else slide = ValueAnimator.ofFloat(knob.position, target).apply {
            duration = 260
            interpolator = OvershootInterpolator(1.6f)
            addUpdateListener { knob.position = it.animatedValue as Float }
            start()
        }
        onChange(on)
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        alpha = if (enabled) 1f else 0.5f
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Switch::class.java.name
        info.isCheckable = true
        info.isChecked = on
    }

    /** The track and its knob: [position] 0 is off (sand), 1 on (forest); past 1 or below 0 while it bounces. */
    private class Knob(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private val colours = ArgbEvaluator()
        private val inset = context.dp(4).toFloat()
        private val lip = context.dp(3).toFloat()

        var position = 0f
            set(value) {
                field = value
                invalidate()
            }

        init {
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val p = position.coerceIn(0f, 1f)
            // The track, with its own lip, like every piece of rubber here.
            val r = (h - lip) / 2f
            paint.color = colours.evaluate(p, Palette.SAND_LIP, Palette.FOREST_LIP) as Int
            rect.set(0f, lip, w, h)
            canvas.drawRoundRect(rect, r, r, paint)
            paint.color = colours.evaluate(p, Palette.LINE_STRONG, Palette.GREEN_DARK) as Int
            rect.set(0f, 0f, w, h - lip)
            canvas.drawRoundRect(rect, r, r, paint)
            // The knob, a white bead that slides (and overshoots a hair when it bounces).
            val k = r - inset
            val x = inset + k + (w - 2 * (inset + k)) * position
            paint.color = 0x22000000
            canvas.drawCircle(x, r + context.dp(1), k, paint)
            paint.color = Palette.WHITE
            canvas.drawCircle(x, r, k, paint)
        }
    }
}
