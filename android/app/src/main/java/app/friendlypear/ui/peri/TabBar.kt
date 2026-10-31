package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import app.rive.runtime.kotlin.RiveAnimationView

/**
 * The sections of Peri's screen, along the bottom: Peri, Diary, Learn and Family, each with its own colour. The one
 * on show sits in a pill of its colour and hops when it's picked. Every tab is a big target with its icon and its
 * word, and TalkBack hears which one is selected.
 */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal class TabBar(context: Context, private val tabs: List<Tab>, private val onPick: (Int) -> Unit) : LinearLayout(context) {
    /** [art]: the same icon in Rive, with its own moment when picked; [icon] still stands in with animations off. */
    class Tab(val icon: Int, val label: String, val tint: Int, val ink: Int, val art: RiveArt? = null)

    private class Item(val root: LinearLayout, val pill: FrameLayout, val icon: View, val label: TextView)

    private val items = ArrayList<Item>()

    var selected = -1
        private set

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = RubberDrawable(Palette.WHITE, Palette.CARD_LIP, context.dp(4).toFloat(), context.dp(30).toFloat())
        setPadding(context.dp(6), context.dp(6), context.dp(6), context.dp(10))
        tabs.forEachIndexed { i, tab ->
            val item = item(i, tab)
            items += item
            addView(item.root, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun item(i: Int, tab: Tab): Item {
        val art = tab.art?.let { context.rive(it, 0f) }
        val icon = art ?: ImageView(context).apply {
            setImageResource(tab.icon)
            setColorFilter(Palette.INK_SOFT)
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val pill = FrameLayout(context).apply {
            // The Rive icon's artboard has a margin around the drawing: a little larger, the same size on screen.
            val size = context.dp(if (art != null) 30 else 26)
            addView(icon, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        }
        val label = TextView(context).apply {
            text = tab.label
            sizeSp(14f)
            typeface = Fonts.display(600)
            setTextColor(Palette.INK_SOFT)
            gravity = Gravity.CENTER
            isSingleLine = true
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val root = object : LinearLayout(context) {
            override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(info)
                info.className = android.widget.Button::class.java.name
                info.isSelected = selected == i
            }
        }.apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = tab.label
            background = pressable(rounded(0, context.dp(22).toFloat()), Palette.RIPPLE_DARK)
            addView(pill, LayoutParams(context.dp(58), context.dp(32)))
            addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(2) })
            setOnClickListener {
                if (selected != i) {
                    performHapticFeedbackSoft(this)
                    select(i)
                    onPick(i)
                }
            }
        }
        return Item(root, pill, icon, label)
    }

    /** Shows [index] as the section on screen, with its colour; [animate] makes its icon hop. */
    fun select(index: Int, animate: Boolean = true) {
        if (index == selected) return
        selected = index
        items.forEachIndexed { i, item ->
            val on = i == index
            val tab = tabs[i]
            item.pill.background = if (on) rounded(tab.tint, context.dp(16).toFloat()) else null
            (item.icon as? ImageView)?.setColorFilter(if (on) tab.ink else Palette.INK_SOFT)
            // The Rive icon takes its colour itself, and plays its moment.
            if (item.icon is RiveAnimationView) tab.art?.let { item.icon.show(it, if (on) 1f else 0f) }
            item.label.setTextColor(if (on) tab.ink else Palette.INK_SOFT)
            item.label.typeface = Fonts.display(if (on) 700 else 600)
            item.root.isSelected = on
            if (on && animate && context.animationsOn()) {
                item.pill.scaleX = 0.6f
                item.pill.scaleY = 0.6f
                item.pill.animate().scaleX(1f).scaleY(1f).setDuration(380).setInterpolator(OvershootInterpolator(3f)).start()
                item.icon.translationY = 0f
                item.icon.animate().translationY(-context.dp(4).toFloat()).setDuration(120).withEndAction {
                    item.icon.animate().translationY(0f).setDuration(260).setInterpolator(OvershootInterpolator(3f)).start()
                }.start()
            }
        }
    }

    private fun performHapticFeedbackSoft(view: View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }
}
