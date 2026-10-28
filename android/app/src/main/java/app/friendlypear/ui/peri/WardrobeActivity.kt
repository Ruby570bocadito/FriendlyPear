package app.friendlypear.ui.peri

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.services.PeriMoment
import app.friendlypear.services.PeriView

/**
 * Peri's wardrobe: dress her with something on her head, on her eyes and below her smile. Every choice shows on her
 * at once (she likes it, and says so) and is kept straight away for every Peri: the app, Mini Peri and the game.
 * "Surprise me" picks a whole outfit.
 */
class WardrobeActivity : Activity() {
    private val hub by lazy { PeriApp.hub(this) }
    private lateinit var face: PeriFace
    private lateinit var world: PeriWorld
    private val tiles = HashMap<Outfit.Slot, List<Pair<Outfit.Item?, View>>>()
    private var outfit = Outfit()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        outfit = Outfit.load(this)
        world = PeriWorld(this)
        face = PeriFace(this).apply {
            outfit = this@WardrobeActivity.outfit
            isClickable = true
            setOnClickListener { play(PeriFace.Trick.HOP) }
        }

        val back = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Palette.GREEN_DARK)
            contentDescription = getString(R.string.back)
            background = pressable(rounded(Palette.WHITE, dp(24).toFloat()), Palette.RIPPLE_DARK)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { finish() }
        }
        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(back, LinearLayout.LayoutParams(dp(48), dp(48)))
            // On a white pill, so the title reads on any sky.
            addView(heading(getString(R.string.wardrobe_title)).apply {
                background = rounded(Palette.WHITE, dp(24).toFloat())
                setPadding(dp(18), dp(6), dp(18), dp(8))
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        }

        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground()
            setPadding(dp(16), dp(14), dp(16), dp(18))
        }
        listOf(
            Outfit.Slot.HEAD to R.string.wardrobe_head,
            Outfit.Slot.EYES to R.string.wardrobe_eyes,
            Outfit.Slot.NECK to R.string.wardrobe_neck,
        ).forEach { (slot, title) ->
            sheet.addView(heading(getString(title), 18f).apply { setPadding(dp(4), if (slot == Outfit.Slot.HEAD) 0 else dp(10), 0, dp(6)) })
            sheet.addView(HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                clipToPadding = false
                addView(LinearLayout(this@WardrobeActivity).apply {
                    val row = (listOf<Outfit.Item?>(null) + Outfit.Item.entries.filter { it.slot == slot }).map { item -> item to tile(slot, item) }
                    tiles[slot] = row
                    row.forEach { (_, view) -> addView(view, LinearLayout.LayoutParams(dp(TILE_DP), dp(TILE_DP)).apply { marginEnd = dp(10) }) }
                })
            })
        }
        sheet.addView(LinearLayout(this).apply {
            addView(Pill(this@WardrobeActivity, getString(R.string.wardrobe_surprise), R.drawable.ic_star, primary = false, rubber = Rubber.SUN).apply {
                setOnClickListener { surprise() }
            }, LinearLayout.LayoutParams(0, dp(62), 1f).apply { marginEnd = dp(6) })
            addView(Pill(this@WardrobeActivity, getString(R.string.wardrobe_done), R.drawable.ic_check, primary = true).apply {
                setOnClickListener { finish() }
            }, LinearLayout.LayoutParams(0, dp(62), 1f).apply { marginStart = dp(6) })
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            addView(top)
            addView(face, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(8) })
            addView(sheet, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        }
        val root = FrameLayout(this).apply {
            addView(world, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }
        val pad = dp(16)
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            column.setPadding(pad + bars.left, dp(8) + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        // Peri stands on her hill, wherever the sheet leaves her.
        column.viewTreeObserver.addOnGlobalLayoutListener {
            if (face.height == 0 || world.height == 0) return@addOnGlobalLayoutListener
            val at = IntArray(2).also { face.getLocationInWindow(it) }
            val origin = IntArray(2).also { world.getLocationInWindow(it) }
            world.setGround(at[1] - origin[1] + face.feetY + dp(3))
        }
        setContentView(root)
        face.show(Mood.CALM)
        showChosen()
        sheet.floatIn(120)
        if (savedInstanceState == null) face.post { face.enter() }
    }

    /** A round tile: the accessory on cream, or a cross for "nothing"; a green ring around the one she wears. */
    private fun tile(slot: Outfit.Slot, item: Outfit.Item?): View = FrameLayout(this).apply {
        isClickable = true
        isFocusable = true
        stateListAnimator = pressScale()
        contentDescription = getString(item?.label ?: R.string.wardrobe_none)
        setOnClickListener { choose(slot, item) }
        addView(ImageView(this@WardrobeActivity).apply {
            if (item == null) {
                setImageResource(R.drawable.ic_close)
                setColorFilter(Palette.INK_SOFT)
                setPadding(dp(20), dp(20), dp(20), dp(20))
            } else {
                setImageDrawable(sized(item.picture, dp(TILE_DP)))
                setPadding(dp(9), dp(9), dp(9), dp(9))
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun showChosen() {
        tiles.forEach { (slot, row) ->
            row.forEach { (item, view) ->
                val on = outfit.on(slot) == item
                view.isSelected = on
                view.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (on) Palette.GREEN_TINT else Palette.CREAM)
                    if (on) setStroke(dp(3), Palette.GREEN_DARK) else setStroke(dp(1), Palette.LINE_STRONG)
                }
            }
        }
    }

    /** She puts it on at once, hops and says what she thinks of it; it's kept for every Peri. */
    private fun choose(slot: Outfit.Slot, item: Outfit.Item?) {
        if (outfit.on(slot) == item) return
        wear(outfit.with(slot, item))
        face.play(PeriFace.Trick.HOP)
        if (!hub.speaking) say(Lines.next(this, if (item == null) R.array.peri_outfit_off else R.array.peri_outfit))
    }

    private fun surprise() {
        wear(Outfit.random())
        face.play(PeriFace.Trick.SPIN)
        face.postDelayed({ face.moment(PeriMoment.SURPRISED) }, SPIN_MS)
        if (!hub.speaking) say(Lines.next(this, R.array.peri_outfit))
    }

    private fun wear(next: Outfit) {
        outfit = next
        Outfit.save(this, next)
        face.outfit = next
        showChosen()
        // The family sees her new look on this phone's pear.
        hub.publishPear()
    }

    private fun say(text: String) = hub.say(text)

    private val view = object : PeriView {
        override fun speaking(on: Boolean) = face.speaking(on)
    }

    override fun onResume() {
        super.onResume()
        hub.attach(view)
    }

    override fun onPause() {
        hub.detach(view)
        face.speaking(false)
        super.onPause()
    }

    private companion object {
        const val TILE_DP = 72
        const val SPIN_MS = 800L
    }
}
