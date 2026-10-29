package app.friendlypear.ui.peri

import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Choreographer
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import java.util.concurrent.Executors

/**
 * Peri on screen, cut out of her Muse Image illustrations (assets/peri/) so she stands on any background.
 * The motion is smooth and never blends two poses: she tilts, shifts her weight and bobs as one image, with a
 * soft shadow that shrinks as she rises, squashes a little when she lands and nods as she speaks; each mood
 * moves its own way (a happy Peri runs on the spot, a steady protective one, slow breathing asleep). Now and
 * then she does something by herself: a hop, steps or a little dance, a spin, a wink, a look around, a stretch,
 * a wave. She drops in when the screen opens, hops when her mood changes, squishes like clay when pressed and
 * talks with her hands. Only quick changes swap frames: blinks, glances, steps, gestures and mouth shapes.
 * Two pictures never show at once (blended, her legs and arms looked doubled, like a ghost): a new frame takes the
 * old one's place at once with a small clay squash, and a new mood pops in. With "remove animations" on she changes
 * mood without moving. Most frames are drawn by layers ([LayeredFrame]): blinks, words and glances change only her
 * eyes and mouth on the same body, and what she holds stays in front of her face.
 */
class PeriFace(context: Context) : FrameLayout(context) {
    private data class Frames(
        val still: Int,
        val half: Int,
        val closed: Int,
        val talk: Int,
        val wide: Int,
        val round: Int,
        val left: Int,
        val right: Int,
        /** One foot lifted (left or right side of the picture), for steps; none while asleep. */
        val stepLeft: Int? = null,
        val stepRight: Int? = null,
        /** Talking with her hands: one hand raised, then both arms open; and a wink. */
        val gestureA: Int? = null,
        val gestureB: Int? = null,
        val wink: Int? = null,
        /** The foot only just leaving the ground: a step in four phases rather than two. */
        val stepLeftHalf: Int? = null,
        val stepRightHalf: Int? = null,
    ) {
        val all = setOfNotNull(still, half, closed, talk, wide, round, left, right, stepLeft, stepRight, stepLeftHalf, stepRightHalf, gestureA, gestureB, wink)
    }

    /** Little tricks the screens can ask for, e.g. when she's tapped. */
    enum class Trick { HOP, SPIN, DANCE, WINK, STRETCH }

    /** What she does by herself now and then, when nothing else is going on; DROP is her entrance. */
    private enum class Idle { HOP, STEPS, LOOK, WAVE, STRETCH, WINK, SPIN, DANCE, DROP, POSE, BOW }

    /** One moment of an action: how high (dp), squash (−) or stretch (+), extra tilt (°) and width (−1 is turned round). */
    private data class Shape(val lift: Float = 0f, val squash: Float = 0f, val tilt: Float = 0f, val width: Float = 1f)

    /** A moment of a movement drawn frame by frame: when, the picture that comes on (or none), how high she is then. */
    private data class Key(val at: Long, val res: Int?, val lift: Float, val fall: Boolean = false)

    /** A movement drawn frame by frame: its keys, and when her feet touch the ground (a puff of dust). */
    private class Drawn(val keys: List<Key>, val lands: Long) {
        val pictures = keys.mapNotNull { it.res }.distinct()
        val length = keys.last().at
    }

    /** How each mood moves: tilt in degrees and its period, bob in dp and its period, breathing depth. */
    private data class Motion(
        val tilt: Float,
        val tiltMs: Long,
        val bob: Float,
        val bobMs: Long,
        val bounce: Boolean = false,
        val breath: Float = 0.012f,
        val breathMs: Long = 3_200,
        val blinks: Boolean = true,
        /** Side-to-side weight shift in dp, in time with the tilt. */
        val sway: Float = 1f,
        val idles: List<Idle> = emptyList(),
        /** Runs on the spot: a foot up on every bounce. */
        val runs: Boolean = false,
    )

    private val moves = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    private val main = Handler(Looper.getMainLooper())
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
    /** Where her feet are, in this view's pixels: the ground of Peri's world goes there. */
    var feetY = 0f
        private set
    private var shadowWidth = 0f
    private var rise = 0f
    private val rig = FrameLayout(context)
    private val sparkles = Sparkles(context)
    /** What she wears, drawn over her picture inside the rig, so it moves with her. */
    private val outfitLayer = OutfitLayer(context)
    private val front = layer()
    private var shown: Mood? = null
    private var frames = FRAMES.getValue(Mood.CALM)
    private val loaded = HashMap<Int, Drawable>()
    /** Her bare head for under a hat ([PeriTops]), decoded at her size like the frames; and the picture on show. */
    private val tops = HashMap<Int, Drawable>()
    private var onShow = 0
    private var talking = false
    private var popStart = 0L
    private val pop = OvershootInterpolator(2.2f)
    private var running = false
    /** While a pose (an idea, "sorry", a thumbs up) is on, blinks and glances wait. */
    private var posing = false
    /** The idle action going on (a hop, steps, a stretch) and when it started. */
    private var action: Idle? = null
    /** The keys of the movement playing frame by frame, if it is one; her height comes from them. */
    private var path: List<Key>? = null
    /** The side of her square picture, in px: the frames' heights are shares of it. */
    private var pictureSide = 0f
    /** Out of sight, her entrance waits for its frames. */
    private var entering = false
    /** The jump and the entrance about to play: each picked once, and kept while its frames decode. */
    private val variants = mutableMapOf<Idle, Drawn>()
    private var actionStart = 0L
    /** Until then a blink or glance owns the frame, so running steps don't cut it short. */
    private var busyUntil = 0L
    /** A turn with her own pictures, not a mirror trick. */
    private var turning = false
    /** Until then one of her movements owns the picture. */
    private var movingUntil = 0L
    private var lastRun = -1L
    /** The start of the little nod that goes with each word, and of the squash as her body changes frame. */
    private var nodStart = 0L
    private var snapStart = 0L
    /** Pressed, she squishes; let go, she springs back past straight and settles, like clay. */
    private var pressTarget = 0f
    private var pressX = 0f
    private var pressV = 0f
    private var lastFrame = 0L
    private var lastSlot = -1L
    /** Dragged by a finger: where it wants her (px from her place), where she is, and how fast she's going. */
    private var holding = false
    private var holdX = 0f
    private var holdY = 0f
    private var offX = 0f
    private var offY = 0f
    private var velX = 0f
    private var velY = 0f

    init {
        rig.addView(front, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        rig.addView(outfitLayer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        outfitLayer.outfit = Outfit.load(context)
        addView(rig, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(sparkles, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        clipChildren = false
        clipToPadding = false
    }

    private fun layer() = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Her room changed (a card came or went below her): she grows or shrinks to it smoothly, her feet staying on the
     * ground, instead of jumping to the new size. Only her own view moves; the rig keeps bobbing as always.
     */
    private fun glide(oldSide: Float, side: Float, oldFeet: Float, w: Int) {
        if (oldSide <= 0f || side <= 0f || !moves || !isAttachedToWindow) return
        val ratio = oldSide / side
        if (kotlin.math.abs(ratio - 1f) < 0.01f) return
        animate().cancel()
        pivotX = w / 2f
        pivotY = feetY
        scaleX = ratio
        scaleY = ratio
        translationY = oldFeet - feetY
        animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(GLIDE_MS)
            .setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start()
    }

    /** Mini Peri handles touches itself (tap, drag, long press) and reports a tap here, for accessibility. */
    override fun performClick(): Boolean = super.performClick()

    /** The illustrations are square; her feet are near the bottom of that square, wherever it lands. */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val side = min(w, h).toFloat()
        pictureSide = side
        val wasFeet = feetY
        feetY = (h - side) / 2 + side * FEET
        sparkles.feetY = feetY
        glide(min(oldw, oldh).toFloat(), side, wasFeet, w)
        shadowWidth = side * 0.44f
        rig.pivotX = w / 2f
        rig.pivotY = feetY
        // Frames are decoded for her size: a new size (Mini Peri, a turned phone) decodes them again.
        // Only for a clearly different size: the bubble growing a line resizes her a little, and that's fine.
        val target = targetSide()
        if (target > decodedSide || target < decodedSide * 3 / 4) {
            decodedSide = target
            loaded.clear()
            tops.clear()
            if (shown != null) preload()
        }
        // A view squeezed to nothing (a crowded screen) has no room for a shadow: a zero radius would crash.
        shadow.shader = if (shadowWidth > 1f) RadialGradient(0f, 0f, shadowWidth / 2, Palette.SHADOW, 0x00000000, Shader.TileMode.CLAMP) else null
    }

    /** Her shadow on the ground, under the picture: smaller and lighter as she rises. */
    override fun dispatchDraw(canvas: Canvas) {
        if (shadow.shader != null) {
            val scale = 1 - rise * 0.22f
            canvas.save()
            canvas.translate(width / 2f + offX, feetY)
            canvas.scale(scale, scale * SHADOW_FLATNESS)
            shadow.alpha = (255 * (1 - rise * 0.35f)).toInt()
            canvas.drawCircle(0f, 0f, shadowWidth / 2, shadow)
            canvas.restore()
        }
        super.dispatchDraw(canvas)
    }

    private fun drawable(res: Int): Drawable = loaded.getOrPut(res) { decode(res) }

    /** Under a hat her leaf would poke out: the picture with her head bare instead, where there is one. */
    private fun dressed(res: Int, picture: Drawable): Drawable {
        onShow = res
        if (!bare(outfitLayer.outfit)) return picture
        val spec = PeriTops.TOPS[res] ?: return picture
        return BareHead(picture, tops.getOrPut(spec.picture) { decodeTop(spec) }, spec)
    }

    private fun bare(outfit: Outfit) = outfit.head in Outfit.HATS && Outfit.Slot.HEAD !in Outfit.BUSY[outfitLayer.base].orEmpty()

    private fun decodeTop(spec: PeriTops.Top): Drawable {
        val side = decodedSide
        if (side <= 0) return context.getDrawable(spec.picture)!!
        val wide = maxOf(1, (spec.box[2] * side / 1024f).toInt())
        return ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, spec.picture)) { decoder, info, _ ->
            if (info.size.width > wide) decoder.setTargetSize(wide, maxOf(1, wide * info.size.height / info.size.width))
        }
    }

    private var decodedSide = 0

    /** The square she's drawn in, with room for her pop and squish, in steps of 128 px; 0 before the first layout. */
    private fun targetSide(): Int {
        val raw = (min(width, height) * 1.12f).toInt()
        return if (raw <= 0) 0 else min(1024, (raw + 127) / 128 * 128)
    }

    /**
     * An illustration decoded at the size she's drawn at, not its full 1024 px: Mini Peri needs a fraction of
     * the memory, and the big Peri a half, with no loss of sharpness. Where the frame has layers, it's drawn by
     * them ([LayeredFrame]): one body for the whole mood, with the eyes and mouth of the frame and what she holds on top.
     */
    private fun decode(res: Int, side: Int = decodedSide): Drawable {
        PeriLayerData.FRAMES[res]?.let { return LayeredFrame(context.resources, it, if (side > 0) side else PeriLayerData.SIZE.toInt()) }
        if (side <= 0) return context.getDrawable(res)!!
        return ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.resources, res)) { decoder, info, _ ->
            if (info.size.width > side) decoder.setTargetSize(side, side * info.size.height / info.size.width)
        }
    }

    /** The mood's other frames decode in the background, so a change of mood never stutters. */
    private fun preload() {
        val wanted = frames.all.filter { it !in loaded }
        if (wanted.isEmpty()) return
        val side = decodedSide
        DECODER.execute {
            val ready = wanted.mapNotNull { res -> runCatching { res to decode(res, side) }.getOrNull() }
            main.post {
                // Only if she's still that size, and the frames still belong to her mood.
                if (side == decodedSide) ready.forEach { (res, d) -> if (res in frames.all) loaded.putIfAbsent(res, d) }
            }
        }
    }

    fun show(mood: Mood) {
        if (mood == shown) return
        val first = shown == null
        shown = mood
        action = null
        frames = FRAMES.getValue(mood)
        outfitLayer.base = frames.still
        outfitLayer.anchor = frames.still
        contentDescription = context.getString(DESCRIPTIONS.getValue(mood))
        // Only this mood's frames stay in memory: each is a full-quality illustration.
        loaded.keys.retainAll(frames.all)
        tops.keys.retainAll(setOfNotNull(PeriTops.TOPS[frames.still]?.picture))
        // The new mood at once, popping in with a hop: fading over the old one showed both, one through the other.
        front.setImageDrawable(dressed(frames.still, drawable(frames.still)))
        if (moves && !first) {
            popStart = SystemClock.uptimeMillis()
            if (mood != Mood.SLEEPING) act(Idle.HOP)
            // Her mood's sign, in clay: a drop when she worries, a question mark while she thinks, a "!" when she warns.
            when (mood) {
                Mood.HAPPY -> sparkles.burst(22)
                Mood.WORRIED -> sparkles.play(Sparkles.Effect.SWEAT)
                Mood.THINKING -> sparkles.play(Sparkles.Effect.QUESTION)
                Mood.PROTECTIVE -> sparkles.play(Sparkles.Effect.ALARM)
                else -> Unit
            }
        }
        // The other frames decode in the background, so the change itself is instant.
        preload()
    }

    /** What she wears, saved (every Peri on screen puts it on) or being tried on in her wardrobe. */
    internal var outfit: Outfit
        get() = outfitLayer.outfit
        set(value) {
            val wasBare = bare(outfitLayer.outfit)
            outfitLayer.outfit = value
            // A hat on or off: her leaf goes or comes back on the picture on show.
            if (bare(value) != wasBare && onShow != 0) front.setImageDrawable(dressed(onShow, loaded[onShow] ?: decode(onShow)))
        }

    /** The mouth moves while Peri's voice plays. */
    fun speaking(on: Boolean) {
        if (on == talking) return
        talking = on
        main.removeCallbacks(mouth)
        if (on && moves) {
            // A pose holds her face still: she strikes it a moment, then talks with her mouth moving.
            if (posing) {
                main.removeCallbacks(endPose)
                main.postDelayed(endPose, maxOf(0L, POSE_TALK_MS - (SystemClock.uptimeMillis() - popStart)))
            }
            main.post(mouth)
        } else if (!on && shown != null) {
            put(frames.still)
        }
    }

    /** One of the poses for a couple of seconds, with a little pop, then back to her mood. */
    fun moment(moment: app.friendlypear.services.PeriMoment) {
        if (shown == null) return
        // A moment that comes during her entrance (her hello's yawn, cape or scarf) waits until she has landed.
        if (entering || action == Idle.DROP) {
            main.postDelayed({ moment(moment) }, MOMENT_WAIT_MS)
            return
        }
        val res = when (moment) {
            app.friendlypear.services.PeriMoment.IDEA -> R.drawable.peri_calm_idea
            app.friendlypear.services.PeriMoment.SORRY -> R.drawable.peri_calm_sorry
            app.friendlypear.services.PeriMoment.THUMBS_UP -> R.drawable.peri_calm_thumbsup
            app.friendlypear.services.PeriMoment.LAUGH -> R.drawable.peri_calm_laugh
            app.friendlypear.services.PeriMoment.LOVE -> R.drawable.peri_calm_love
            app.friendlypear.services.PeriMoment.DIZZY -> R.drawable.peri_calm_dizzy
            app.friendlypear.services.PeriMoment.YAWN -> R.drawable.peri_calm_yawn
            app.friendlypear.services.PeriMoment.LETTER -> R.drawable.peri_calm_letter
            app.friendlypear.services.PeriMoment.HERO -> R.drawable.peri_calm_hero
            app.friendlypear.services.PeriMoment.MEDAL -> R.drawable.peri_calm_medal
            app.friendlypear.services.PeriMoment.PEEKABOO -> R.drawable.peri_calm_peekaboo
            app.friendlypear.services.PeriMoment.CLAP -> R.drawable.peri_calm_clap
            app.friendlypear.services.PeriMoment.DETECTIVE -> R.drawable.peri_calm_detective
            app.friendlypear.services.PeriMoment.SURPRISED -> R.drawable.peri_calm_surprised
            app.friendlypear.services.PeriMoment.PARTY -> R.drawable.peri_calm_party
            app.friendlypear.services.PeriMoment.GLASSES -> R.drawable.peri_calm_glasses
            app.friendlypear.services.PeriMoment.SCARF -> R.drawable.peri_calm_scarf
            app.friendlypear.services.PeriMoment.BYE -> R.drawable.peri_calm_bye
        }
        hold(res, POSE_MILLIS)
        if (moves) when (moment) {
            app.friendlypear.services.PeriMoment.LETTER, app.friendlypear.services.PeriMoment.GLASSES, app.friendlypear.services.PeriMoment.SCARF -> Unit
            app.friendlypear.services.PeriMoment.DETECTIVE -> sparkles.play(Sparkles.Effect.QUESTION)
            app.friendlypear.services.PeriMoment.SORRY -> sparkles.play(Sparkles.Effect.SWEAT)
            app.friendlypear.services.PeriMoment.LOVE, app.friendlypear.services.PeriMoment.PEEKABOO, app.friendlypear.services.PeriMoment.BYE -> sparkles.play(Sparkles.Effect.HEARTS)
            app.friendlypear.services.PeriMoment.PARTY, app.friendlypear.services.PeriMoment.MEDAL,
            app.friendlypear.services.PeriMoment.CLAP -> sparkles.play(Sparkles.Effect.CONFETTI, 26)
            app.friendlypear.services.PeriMoment.HERO -> sparkles.burst(26)
            app.friendlypear.services.PeriMoment.SURPRISED -> sparkles.play(Sparkles.Effect.ALARM)
            app.friendlypear.services.PeriMoment.IDEA -> sparkles.play(Sparkles.Effect.IDEA)
            app.friendlypear.services.PeriMoment.YAWN -> sparkles.play(Sparkles.Effect.SLEEPY)
            app.friendlypear.services.PeriMoment.DIZZY -> sparkles.burst(8)
            else -> sparkles.burst()
        }
    }

    /** [res] on her for [millis], dressed as she is, with a little pop; then back to her mood. */
    private fun hold(res: Int, millis: Long) {
        posing = true
        action = null
        outfitLayer.base = res
        outfitLayer.anchor = res
        front.setImageDrawable(dressed(res, decode(res)))
        popStart = SystemClock.uptimeMillis()
        main.removeCallbacks(endPose)
        main.postDelayed(endPose, millis)
    }

    private val endPose = Runnable {
        posing = false
        outfitLayer.base = frames.still
        outfitLayer.anchor = frames.still
        put(frames.still)
    }

    /** Something went well (a shield): clay stars spring out of her. */
    fun stars(count: Int = 12) {
        if (moves && shown != Mood.WORRIED && shown != Mood.PROTECTIVE) sparkles.play(Sparkles.Effect.STARS, count)
    }

    /** Happy for a moment (a shield earned, or she was tapped): her jump for joy, then back to her mood. */
    fun cheer() {
        val mood = shown ?: return
        if (!moves || mood == Mood.HAPPY) return
        show(Mood.HAPPY)
        main.postDelayed({ if (shown == Mood.HAPPY) show(mood) }, CHEER_MILLIS)
    }

    /** A little wave hello, when the app opens and she's calm. */
    fun greet() {
        if (!moves || shown != Mood.CALM || talking) return
        val steps = listOf(R.drawable.peri_calm_wave1, R.drawable.peri_calm_wave2, R.drawable.peri_calm_wave1, R.drawable.peri_calm_wave2, R.drawable.peri_calm_wave1)
        steps.forEachIndexed { i, res -> main.postDelayed({ if (shown == Mood.CALM && !talking) put(res, snap = true) }, WAVE_START + i * WAVE_STEP) }
        main.postDelayed({ if (shown == Mood.CALM && !talking) put(frames.still, snap = true) }, WAVE_START + steps.size * WAVE_STEP)
    }

    /**
     * Hidden (the app in the background, Mini Peri out instead), she lets go of her frames, all but the one on
     * show; back on screen, they decode again in the background.
     */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            if (shown != null) preload()
        } else {
            loaded.clear()
            tops.clear()
        }
    }

    /**
     * [res] on her now, in place of the frame before (never over it: two at once looked like a ghost). With [snap],
     * her body changes (a step, a gesture, a wave) and she squashes a touch as she moves, like clay; blinks and her
     * mouth just change, as they do in life.
     */
    private fun put(res: Int, snap: Boolean = false) {
        if (windowVisibility != VISIBLE) return
        // A blink or wave scheduled before a mood change never shows a frame of the old mood.
        val waving = shown == Mood.CALM && (res == R.drawable.peri_calm_wave1 || res == R.drawable.peri_calm_wave2)
        val moving = res in MOVE_FRAMES && canMove()
        // During one of her movements, only its pictures: a word or a blink would cut it short (it has its own face).
        if (SystemClock.uptimeMillis() < movingUntil && res !in MOVE_FRAMES) return
        if (posing || (res !in frames.all && !waving && !moving)) return
        outfitLayer.visibility = if (res in BARE) INVISIBLE else VISIBLE
        front.setImageDrawable(dressed(res, drawable(res)))
        if (snap && moves) snapStart = SystemClock.uptimeMillis()
        // Each frame places her outfit from its own eyes: a step or a gesture moves her a little.
        outfitLayer.anchor = res
    }

    private val mouth = object : Runnable {
        private var last = 0
        override fun run() {
            if (!talking || shown == null) return
            // Now and then a hand gesture (her mouth open in it too), held a little longer, like people do.
            val gesture = listOfNotNull(frames.gestureA, frames.gestureB).filter { it != last }
            if (gesture.isNotEmpty() && last !in gesture && Random.nextInt(9) == 0) {
                val g = gesture.random()
                last = g
                put(g, snap = true)
                nodStart = SystemClock.uptimeMillis()
                main.postDelayed(this, Random.nextLong(700, 1_050))
                return
            }
            val next = listOf(frames.talk, frames.wide, frames.round, frames.still).filter { it != last }.random()
            // Out of a gesture her arms come down softly; between mouth shapes it's instant.
            val fromGesture = last == frames.gestureA || last == frames.gestureB
            last = next
            put(next, snap = fromGesture)
            if (next != frames.still) nodStart = SystemClock.uptimeMillis()
            main.postDelayed(this, if (next == frames.still) Random.nextLong(110, 180) else Random.nextLong(140, 230))
        }
    }

    private val blink = object : Runnable {
        override fun run() {
            if (shown != null && !talking && MOTIONS.getValue(shown!!).blinks) {
                busyUntil = SystemClock.uptimeMillis() + 480
                val steps = listOf(frames.half to 0L, frames.closed to 45L, frames.half to 125L, frames.still to 170L)
                steps.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res) }, at) }
                // Now and then a double blink, as people do.
                if (Random.nextInt(7) == 0) steps.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res) }, 260 + at) }
            }
            main.postDelayed(this, Random.nextLong(3_500, 6_500))
        }
    }

    /** Now and then she glances to one side and back, like someone keeping you company. */
    private val glance = object : Runnable {
        override fun run() {
            if (shown != null && !talking && MOTIONS.getValue(shown!!).blinks) {
                val side = if (Random.nextBoolean()) frames.left else frames.right
                val back = Random.nextLong(1_000, 1_600)
                busyUntil = SystemClock.uptimeMillis() + back
                put(side)
                main.postDelayed({ if (!talking) put(frames.still) }, back)
            }
            main.postDelayed(this, Random.nextLong(8_000, 14_000))
        }
    }

    /** Every several seconds, if she's free, one of her mood's idle actions. */
    private val idle = object : Runnable {
        override fun run() {
            val mood = shown
            if (mood != null && !talking && !posing && action == null) {
                MOTIONS.getValue(mood).idles.randomOrNull()?.let { act(it) }
                // Asleep, she breathes out a little "z z z" now and then.
                if (mood == Mood.SLEEPING && moves) sparkles.play(Sparkles.Effect.SLEEPY)
            }
            main.postDelayed(this, Random.nextLong(IDLE_MIN, IDLE_MAX))
        }
    }

    /** Her movements (lote 4) only calm or happy: their pictures have her calm, smiling face. */
    private fun canMove(): Boolean = moves && (shown == Mood.CALM || shown == Mood.HAPPY)

    /** [res] decoded in the background, then [then] on the main thread; at once if they're ready. */
    private fun prepared(res: List<Int>, then: () -> Unit) {
        val missing = res.filter { it !in loaded }
        if (missing.isEmpty()) return then()
        val side = decodedSide
        // On a thread of their own: a movement never waits behind a whole mood's frames being decoded.
        MOVES_DECODER.execute {
            val ready = missing.mapNotNull { r -> runCatching { r to decode(r, side) }.getOrNull() }
            main.post {
                if (side == decodedSide) ready.forEach { (r, d) -> loaded.putIfAbsent(r, d) }
                then()
            }
        }
    }

    /** Once a movement is over, she lets go of its pictures: each is a full illustration. */
    private fun forgetMoves(after: Long) = main.postDelayed({ if (action == null) loaded.keys.removeAll(MOVE_FRAMES) }, after)

    /** What [idle] needs decoded; a jump or an entrance picks which of hers it will be, once. */
    private fun pictures(idle: Idle): List<Int> = when (idle) {
        Idle.HOP, Idle.DROP -> variants.getOrPut(idle) { pick(idle) }.pictures
        else -> MOVES[idle].orEmpty()
    }

    /** Her entrances and her jumps at random (the big jump most often); happy, she jumps for joy. */
    private fun pick(idle: Idle): Drawn = when {
        idle == Idle.DROP -> ENTRANCES.random()
        shown == Mood.HAPPY -> CHEER
        else -> HOPS.random()
    }

    /** [idle] now; with [drawn] false, without its own pictures even if she has them (an entrance that couldn't wait). */
    private fun act(idle: Idle, drawn: Boolean = true) {
        // Her entrance comes first: nothing cuts it short, nor starts while she's still out of sight.
        if ((entering || action == Idle.DROP) && idle != Idle.DROP) return
        val now = SystemClock.uptimeMillis()
        // A movement with pictures of its own waits for them, decoded in the background, so it never stutters.
        val pictures = if (drawn && canMove()) pictures(idle) else emptyList()
        if (pictures.any { it !in loaded }) return prepared(pictures) { if (!posing && action == null && canMove()) act(idle) }
        val real = pictures.isNotEmpty()
        // The jump or entrance picked, drawn frame by frame; the next one picks again.
        val move = variants.remove(idle)?.takeIf { real }
        // Any new movement takes over from one drawn frame by frame.
        path = null
        if (real) {
            forgetMoves(MOVE_HOLD_MS)
            movingUntil = now + (move?.let { it.length - 60 } ?: MOVE_MS[idle] ?: 0L)
        }
        when (idle) {
            Idle.HOP, Idle.STRETCH -> {
                action = idle
                actionStart = now
                if (idle == Idle.HOP) main.postDelayed({ if (!sparkles.busy) sparkles.play(Sparkles.Effect.DUST) }, move?.lands ?: HOP_LAND_MS)
                // Her own jump, frame by frame, at the heights each was drawn for.
                if (idle == Idle.HOP && move != null) play(move.keys, idle)
                // A stretch, her arms up, as if it felt good; without the picture, her eyes softly half closed.
                if (idle == Idle.STRETCH) {
                    put(if (real) R.drawable.peri_move_stretch_up else frames.half)
                    main.postDelayed({ if (!talking) put(frames.still) }, STRETCH_MS)
                }
            }
            Idle.STEPS -> {
                val l = frames.stepLeft ?: return
                val r = frames.stepRight ?: return
                action = idle
                actionStart = now
                busyUntil = now + STEPS * STEP_MS
                val lh = frames.stepLeftHalf
                val rh = frames.stepRightHalf
                // A foot lifts, is up, comes down, rests; then the other one. Two such strides, in STEPS beats.
                val stride = if (lh != null && rh != null) listOf(lh, l, lh, frames.still, rh, r, rh, frames.still) else listOf(l, frames.still, r, frames.still)
                val beat = STEP_MS * 4 / stride.size
                val steps = stride + stride
                steps.forEachIndexed { i, res -> main.postDelayed({ if (!talking && action == Idle.STEPS) put(res, snap = true) }, i * beat) }
                main.postDelayed({ if (!talking) put(frames.still, snap = true) }, steps.size * beat)
            }
            Idle.LOOK -> {
                busyUntil = now + 2_100
                put(frames.left)
                main.postDelayed({ if (!talking) put(frames.right) }, 950)
                main.postDelayed({ if (!talking) put(frames.still) }, 1_950)
            }
            Idle.WAVE -> greet()
            // A moment of her own while all is calm: humming, a butterfly on her finger, a cup of tea...
            Idle.POSE -> if (moves) {
                val pose = CALM_MOMENTS.random()
                hold(pose, CALM_MOMENT_MILLIS)
                // Humming, notes leave her mouth.
                if (pose == R.drawable.peri_calm_hum) sparkles.play(Sparkles.Effect.NOTES)
            }
            Idle.WINK -> {
                val w = frames.wink ?: return
                busyUntil = now + 850
                put(w)
                main.postDelayed({ if (!talking) put(frames.still) }, 750)
            }
            Idle.SPIN, Idle.DROP -> {
                action = idle
                actionStart = now
                turning = idle == Idle.SPIN && real
                if (idle == Idle.DROP) {
                    rig.alpha = 1f
                    // Out of the ground, the soil puffs as she comes up.
                    if (move === SPROUT) sparkles.play(Sparkles.Effect.DUST)
                    main.postDelayed({ sparkles.play(Sparkles.Effect.DUST) }, move?.lands ?: DROP_MS.toLong())
                    if (move != null) play(move.keys, idle)
                }
                if (turning) {
                    // Round she goes, really, in eight views: three quarters, her side, three quarters from behind, her
                    // back, and round the other side till she faces you again.
                    val step = TURN_SHARE / TURN_STEPS.size
                    TURN_STEPS.forEachIndexed { i, res -> main.postDelayed({ if (action == Idle.SPIN) put(res) }, (SPIN_MS * (TURN_START + i * step)).toLong()) }
                    main.postDelayed({ if (!talking) put(frames.still) }, (SPIN_MS * (TURN_START + TURN_SHARE)).toLong())
                }
            }
            Idle.BOW -> {
                if (!real) return
                busyUntil = now + BOW_STEPS.last().second + 120
                BOW_STEPS.forEach { (res, at) -> main.postDelayed({ if (!talking) put(res ?: frames.still, snap = true) }, at) }
            }
            Idle.DANCE -> {
                val l = frames.stepLeft ?: return
                val r = frames.stepRight ?: return
                action = idle
                actionStart = now
                busyUntil = now + DANCE_STEPS * DANCE_MS
                (0 until DANCE_STEPS).forEach { i ->
                    val step = if (real) DANCE_FRAMES[i % DANCE_FRAMES.size] else if (i % 2 == 0) l else r
                    main.postDelayed({ if (!talking && action == Idle.DANCE) put(step, snap = true) }, i * DANCE_MS)
                }
                main.postDelayed({ if (!talking) put(frames.still, snap = true) }, DANCE_STEPS * DANCE_MS)
            }
        }
    }

    /**
     * Her entrance: she drops in from the sky and lands with a squash. Calm, frame by frame: stretched by the fall, a
     * splat, a bounce, and her arms open. Her frames take a moment to decode: till then she isn't there yet, rather
     * than standing and then flying off.
     */
    fun enter() {
        if (!moves) {
            rig.alpha = 1f
            return
        }
        val pictures = pictures(Idle.DROP)
        if (!canMove() || pictures.all { it in loaded }) return act(Idle.DROP)
        rig.alpha = 0f
        entering = true
        prepared(pictures) { if (entering) start(drawn = true) }
        // If they're slow (or her size changed while they decoded), she falls in as she used to, in one picture.
        main.postDelayed({ if (entering || rig.alpha == 0f) start(drawn = false) }, ENTER_WAIT_MS)
    }

    /** Out of sight from the very first picture until her entrance: she never stands there and then vanishes. */
    fun awaitEntrance() {
        if (moves) rig.alpha = 0f
    }

    private fun start(drawn: Boolean) {
        entering = false
        act(Idle.DROP, drawn)
    }

    /** A movement drawn frame by frame: each picture at its time, and her height between them ([along]). */
    private fun play(keys: List<Key>, idle: Idle) {
        path = keys
        // The first picture at once: one frame of her plain self up in the air would show.
        keys.forEach { key ->
            val res = key.res ?: return@forEach
            if (key.at == 0L) put(res) else main.postDelayed({ if (action == idle) put(res) }, key.at)
        }
        main.postDelayed({ if (!talking) put(frames.still) }, keys.last().at - 40)
    }

    /** Her height [e] ms into a movement drawn frame by frame: between its keys, faster and faster from a [Key.fall]. */
    private fun along(keys: List<Key>, e: Float): Shape {
        val i = keys.indexOfLast { it.at <= e }
        if (i >= keys.lastIndex) {
            path = null
            action = null
            return Shape()
        }
        val a = keys[i]
        val b = keys[i + 1]
        val f = ((e - a.at) / (b.at - a.at)).coerceIn(0f, 1f)
        val share = a.lift + (b.lift - a.lift) * (if (a.fall) f * f else f)
        return Shape(lift = share * pictureSide / resources.displayMetrics.density)
    }

    /**
     * Someone touched the screen to one side of her ([fromCenter] px, negative is left): she glances there for a
     * moment, as if to see what you're doing. Only when she's free.
     */
    fun lookToward(fromCenter: Float) {
        val now = SystemClock.uptimeMillis()
        if (!moves || shown == null || talking || posing || action != null || now < busyUntil) return
        if (!MOTIONS.getValue(shown!!).blinks) return
        busyUntil = now + 1_000
        put(if (fromCenter < 0) frames.left else frames.right)
        main.postDelayed({ if (!talking) put(frames.still) }, 900)
    }

    /** Pressed and let go, for the squish. */
    fun press(on: Boolean) {
        pressTarget = if (on) 1f else 0f
    }

    /**
     * A finger drags her [dx], [dy] px from her place: she follows, leaning into the movement and stretching with
     * the speed, a little less the further she goes, like pulling on a rubber band.
     */
    fun drag(dx: Float, dy: Float) {
        if (!moves) return
        holding = true
        val reachX = width * 0.38f
        val reachUp = height * 0.45f
        val reachDown = height * 0.08f
        holdX = band(dx, reachX)
        holdY = if (dy < 0) band(dy, reachUp) else band(dy, reachDown)
    }

    /** Let go: she springs back to her place, overshoots a little and settles. */
    fun release() {
        holding = false
        holdX = 0f
        holdY = 0f
    }

    private fun band(d: Float, reach: Float): Float = if (reach <= 0f) 0f else Math.signum(d) * reach * (1 - kotlin.math.exp(-abs(d) / reach))

    /** Where the last touch landed on her, from 0 (top of the leaf) to 1 (her feet). */
    var lastTouch = 0.5f
        private set

    /** A trick right now, if she's free to move. */
    fun play(trick: Trick) {
        if (!moves || shown == null || posing) return
        action = null
        act(
            when (trick) {
                Trick.HOP -> Idle.HOP
                Trick.SPIN -> Idle.SPIN
                Trick.DANCE -> Idle.DANCE
                Trick.WINK -> Idle.WINK
                Trick.STRETCH -> Idle.STRETCH
            },
        )
    }

    /** A burst of sparkles, for a happy moment the screen knows about. */
    fun sparkle() {
        if (moves) sparkles.burst()
    }

    /** Every touch on her squishes her, whoever handles it next (the app's tap, Mini Peri's drag). */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                press(true)
                val side = min(width, height).toFloat()
                if (side > 0f) lastTouch = ((ev.y - (height - side) / 2) / side).coerceIn(0f, 1f)
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> press(false)
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * The current action at time [t]. A hop crouches first, springs up a little stretched and lands with a squash;
     * a spin turns her round with a small jump; a dance rocks her side to side on each step.
     */
    private fun actionShape(t: Long): Shape {
        val e = (t - actionStart).toFloat()
        fun done(): Shape = Shape().also { action = null }
        // Drawn frame by frame, the pictures have their own squash and stretch: only her height moves her.
        path?.let { keys -> if (action == Idle.HOP || action == Idle.DROP) return along(keys, e) }
        return when (action) {
            Idle.HOP -> when {
                e < 130 -> Shape(squash = -0.08f * sin(PI / 2 * e / 130).toFloat())
                e < 480 -> {
                    val a = (e - 130) / 350
                    Shape(lift = HOP_DP * 4 * a * (1 - a), squash = 0.05f * (1 - 2 * a).coerceAtLeast(0f))
                }
                e < HOP_MS -> Shape(squash = -0.08f * sin(PI * (e - 480) / (HOP_MS - 480)).toFloat())
                else -> done()
            }
            Idle.STEPS -> if (e < STEPS * STEP_MS) {
                // A small bounce on each step.
                val ph = (e % STEP_MS) / STEP_MS
                Shape(lift = 3f * sin(PI * ph).toFloat())
            } else done()
            Idle.STRETCH -> if (e < STRETCH_MS) Shape(squash = 0.06f * sin(PI * e / STRETCH_MS).toFloat()) else done()
            Idle.SPIN -> if (e < SPIN_MS) {
                val a = e / SPIN_MS
                Shape(lift = 10f * 4 * a * (1 - a), width = if (turning) 1f else kotlin.math.cos(2 * PI * a).toFloat())
            } else done()
            Idle.DANCE -> if (e < DANCE_STEPS * DANCE_MS) {
                val ph = (e % DANCE_MS) / DANCE_MS
                val side = if ((e / DANCE_MS).toInt() % 2 == 0) 1f else -1f
                Shape(lift = 5f * sin(PI * ph).toFloat(), tilt = side * 7f * sin(PI * ph).toFloat())
            } else done()
            Idle.DROP -> when {
                e < DROP_MS -> {
                    // Falling under gravity from high above, a touch stretched by the speed.
                    val a = e / DROP_MS
                    Shape(lift = DROP_DP * (1 - a * a), squash = 0.06f * a)
                }
                e < DROP_MS + 220 -> Shape(squash = -0.12f * sin(PI * (e - DROP_MS) / 220).toFloat())
                else -> done()
            }
            else -> Shape()
        }
    }

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            val t = SystemClock.uptimeMillis()
            val m = MOTIONS.getValue(shown ?: Mood.CALM)
            // Only her calm breathing and sway on show: Rest's calm rate is plenty, on the same vsyncs as her world, and
            // spares an old phone's battery. A finger, a word, a pose or an action brings every frame back.
            if (!lively(t, m)) {
                val slot = Rest.slot(frameTimeNanos, Rest.calmFrameMs(t))
                if (slot == lastSlot) {
                    Choreographer.getInstance().postFrameCallback(this)
                    return
                }
                lastSlot = slot
            }
            fun wave(period: Long) = sin(2 * PI * (t % period) / period).toFloat()
            val density = resources.displayMetrics.density
            rise = if (m.bounce) abs(sin(PI * (t % m.bobMs) / m.bobMs).toFloat()) else (1 - wave(m.bobMs)) / 2
            val breath = wave(m.breathMs) * m.breath
            val p = ((t - popStart) / POP_MS).coerceIn(0f, 1f)
            val popScale = 0.94f + 0.06f * pop.getInterpolation(p)
            // Bouncing, she squashes a little each time she touches the ground.
            val landing = if (m.bounce) -LAND_SQUASH * (1 - rise).let { it * it * it * it * it * it } else 0f
            // A little nod with each word.
            val n = ((t - nodStart) / NOD_MS).coerceIn(0f, 1f)
            val nod = if (talking) sin(PI * n).toFloat() * NOD else 0f
            val shape = actionShape(t)
            val lift = shape.lift
            // The squish: a spring towards pressed (1) or free (0), so letting go overshoots and wobbles.
            val dt = ((t - lastFrame).coerceIn(1, 50)) / 1000f
            lastFrame = t
            pressV += (SPRING_K * (pressTarget - pressX) - SPRING_C * pressV) * dt
            pressX += pressV * dt
            // Dragged, she follows the finger closely; let go, she bounces home.
            val k = if (holding) DRAG_K else HOME_K
            val c = if (holding) DRAG_C else HOME_C
            velX += (k * (holdX - offX) - c * velX) * dt
            velY += (k * (holdY - offY) - c * velY) * dt
            offX += velX * dt
            offY += velY * dt
            val speed = kotlin.math.sqrt(velX * velX + velY * velY) / density
            val pull = (speed * 0.00012f).coerceAtMost(0.09f)
            // A new body frame: a quick squash and back, so the change reads as clay moving.
            val s = ((t - snapStart) / SNAP_MS).coerceIn(0f, 1f)
            val snap = if (s < 1f) -SNAP * sin(PI * s).toFloat() else 0f
            val squash = breath + landing + shape.squash + nod + snap - PRESS_SQUASH * pressX + pull
            // Two waves at unrelated speeds: her sway never looks like a metronome.
            val sway = (wave(m.tiltMs) + 0.4f * wave((m.tiltMs * 0.61f).toLong())) / 1.4f
            rig.rotation = sway * m.tilt + shape.tilt + (velX / density * 0.012f).coerceIn(-14f, 14f)
            rig.translationX = sway * m.sway * density + offX
            rig.translationY = -(rise * m.bob + lift) * density + offY
            // Lifted off the ground, her shadow stays behind on it, smaller.
            if (offY < 0f) rise = maxOf(rise, (-offY / (HOP_DP * 4 * density)).coerceAtMost(1f))
            // Squash and stretch keeps her volume: taller is a touch thinner.
            rig.scaleY = popScale * (1 + squash)
            rig.scaleX = popScale * (1 - squash / 2) * shape.width
            if (lift > 0f) rise = maxOf(rise, (lift / HOP_DP).coerceAtMost(1f))
            // Happy, she runs on the spot: the other foot up on every bounce.
            if (m.runs && !talking && !posing && action == null && t > busyUntil) {
                val step = t / m.bobMs
                if (step != lastRun) {
                    lastRun = step
                    (if (step % 2 == 0L) frames.stepLeft else frames.stepRight)?.let { put(it, snap = true) }
                }
            }
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /** Something more than her calm breathing is moving: a finger on her, a spring settling, a word, a pose, an action. */
    private fun lively(t: Long, m: Motion): Boolean =
        holding || talking || posing || action != null || m.bounce || m.runs || t < busyUntil || t - popStart < POP_MS || t - snapStart < SNAP_MS ||
            abs(pressV) > 0.01f || abs(pressTarget - pressX) > 0.01f || abs(velX) + abs(velY) > 1f || abs(offX) + abs(offY) > 0.5f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!moves) return
        running = true
        Choreographer.getInstance().postFrameCallback(frame)
        main.postDelayed(blink, 1_800)
        main.postDelayed(glance, 5_000)
        main.postDelayed(idle, Random.nextLong(IDLE_FIRST, IDLE_MIN))
        if (talking) main.post(mouth)
    }

    /** Out of sight (another section on screen, a card scrolled away), she rests until she's back. */
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        // Out of sight (another section, a family card scrolled away), she keeps only the picture on show; back in
        // sight, the rest of her mood decodes again in the background.
        if (!isVisible) {
            loaded.keys.retainAll(setOf(onShow))
            tops.clear()
        } else if (shown != null && isAttachedToWindow) {
            preload()
        }
        if (!moves || !isAttachedToWindow) return
        if (isVisible && !running) {
            running = true
            Choreographer.getInstance().postFrameCallback(frame)
            main.postDelayed(blink, 1_800)
            main.postDelayed(glance, 5_000)
            main.postDelayed(idle, Random.nextLong(IDLE_FIRST, IDLE_MIN))
        } else if (!isVisible && running) {
            running = false
            Choreographer.getInstance().removeFrameCallback(frame)
            listOf(blink, glance, idle).forEach { main.removeCallbacks(it) }
        }
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(frame)
        main.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    private companion object {
        /** The squash as her body changes frame: shorter than a beat of her steps, and small. */
        const val SNAP_MS = 140f
        const val SNAP = 0.025f
        const val CHEER_MILLIS = 2_200L
        const val POSE_MILLIS = 2_800L
        /** How long a pose holds once she starts talking, before her talking face takes over. */
        const val POSE_TALK_MS = 1_000L
        /** One background thread for every Peri on screen: decoding is quick, and never on the main thread. */
        val DECODER = Executors.newSingleThreadExecutor { r -> Thread(r, "peri-frames").apply { isDaemon = true; priority = Thread.NORM_PRIORITY - 1 } }
        val MOVES_DECODER = Executors.newSingleThreadExecutor { r -> Thread(r, "peri-moves").apply { isDaemon = true } }
        const val POP_MS = 420f
        const val IDLE_FIRST = 3_500L
        /** Growing or shrinking to new room: about as long as her hill takes to follow. */
        const val GLIDE_MS = 420L
        /** Her calm moments (Muse Image, like her other poses), a little longer than a reaction. */
        val CALM_MOMENTS = listOf(R.drawable.peri_calm_hum, R.drawable.peri_calm_butterfly, R.drawable.peri_calm_tea, R.drawable.peri_calm_flower, R.drawable.peri_calm_phone)
        const val CALM_MOMENT_MILLIS = 3_200L
        const val IDLE_MIN = 7_000L
        const val IDLE_MAX = 12_000L
        const val SPIN_MS = 900f
        /** Her movements' pictures are let go this long after one starts. */
        const val MOVE_HOLD_MS = 4_000L
        /**
         * Her jump frame by frame (lote 16): when each picture comes on, and how high she is then, as a share of her
         * picture (sprites/anim_offsets.json: Muse keeps her in the middle, so the frames in the air are drawn higher).
         * She crouches, springs, rises stretched, tucks, opens out at the top, falls, lands squashed and stands up.
         */
        val JUMP = Drawn(listOf(
            Key(0, R.drawable.peri_anim_jump_01, 0f),
            Key(110, R.drawable.peri_anim_jump_02, 0f),
            Key(170, R.drawable.peri_anim_jump_03, 0.073f),
            Key(240, R.drawable.peri_anim_jump_04, 0.153f),
            Key(320, R.drawable.peri_anim_jump_05, 0.198f),
            Key(410, R.drawable.peri_anim_jump_06, 0.177f),
            Key(490, R.drawable.peri_anim_jump_07, 0f),
            Key(590, R.drawable.peri_anim_jump_08, 0f),
            Key(670, R.drawable.peri_anim_jump_09, 0f),
            Key(750, null, 0f),
        ), lands = 490)
        /** A star jump: up from her crouch, arms and legs wide at the top, and the big jump's landing. */
        val STAR = Drawn(listOf(
            Key(0, R.drawable.peri_anim_jump_01, 0f),
            Key(110, R.drawable.peri_anim_jump_02, 0f),
            Key(170, R.drawable.peri_anim_star_01, 0.06f),
            Key(290, null, 0.17f),
            Key(390, null, 0.08f),
            Key(440, R.drawable.peri_anim_jump_07, 0f),
            Key(540, R.drawable.peri_anim_jump_08, 0f),
            Key(620, R.drawable.peri_anim_jump_09, 0f),
            Key(700, null, 0f),
        ), lands = 440)
        /** A jump with a turn: she crouches turning, is seen from the side and from behind in the air, and lands facing you. */
        val SPIN_JUMP = Drawn(listOf(
            Key(0, R.drawable.peri_anim_spin_01, 0f),
            Key(120, R.drawable.peri_anim_spin_02, 0.07f),
            Key(260, R.drawable.peri_anim_spin_03, 0.16f),
            Key(390, R.drawable.peri_anim_spin_04, 0f),
            Key(640, null, 0f),
        ), lands = 390)
        /** Her jump for joy: fists at her chest, up with both fists high, one fist up at the top, and landed still cheering. */
        val CHEER = Drawn(listOf(
            Key(0, R.drawable.peri_anim_cheer_01, 0f),
            Key(130, R.drawable.peri_anim_cheer_02, 0f),
            Key(200, null, 0.168f),
            Key(290, R.drawable.peri_anim_cheer_03, 0.218f),
            Key(400, null, 0.15f),
            Key(470, R.drawable.peri_anim_cheer_04, 0f),
            Key(760, null, 0f),
        ), lands = 470)
        /** Her jumps, at random: the big one most often. */
        val HOPS = listOf(JUMP, JUMP, STAR, SPIN_JUMP)
        /** Where her entrance starts: this far above the ground, as a share of her picture. */
        const val ENTER_FROM = 0.8f
        /** Her entrance frame by frame: she falls stretched, splats, bounces up a little and stands with her arms open. */
        val DROP = Drawn(listOf(
            Key(0, R.drawable.peri_anim_drop_01, ENTER_FROM, fall = true),
            Key(300, R.drawable.peri_anim_drop_02, 0.11f),
            Key(360, R.drawable.peri_anim_drop_03, 0f),
            Key(480, R.drawable.peri_anim_drop_04, 0f),
            Key(545, null, 0.065f),
            Key(610, R.drawable.peri_anim_drop_05, 0f),
            Key(700, R.drawable.peri_anim_drop_06, 0f),
            Key(1_050, null, 0f),
        ), lands = 360)
        /**
         * Out of the ground: her leaf and the top of her head, then half of her (both pictures end where the soil is,
         * so they're drawn up to the ground line), a jump out with her arms up and the big jump's landing.
         */
        val SPROUT = Drawn(listOf(
            Key(0, R.drawable.peri_anim_sprout_01, 1 - FEET),
            Key(320, R.drawable.peri_anim_sprout_02, 1 - FEET),
            Key(560, R.drawable.peri_anim_sprout_03, 0.08f),
            Key(660, null, 0.2f),
            Key(780, R.drawable.peri_anim_jump_07, 0f),
            Key(880, R.drawable.peri_anim_jump_09, 0f),
            Key(960, null, 0f),
        ), lands = 780)
        /** Down with her umbrella: floating from high up, swaying lower, and landing softly as she closes it. */
        val FLOAT = Drawn(listOf(
            Key(0, R.drawable.peri_anim_float_01, 0.9f),
            Key(700, R.drawable.peri_anim_float_02, 0.3f),
            Key(1_150, R.drawable.peri_anim_float_03, 0f),
            Key(1_600, null, 0f),
        ), lands = 1_150)
        /** Her entrances, at random: from the sky, out of the ground, down with her umbrella. */
        val ENTRANCES = listOf(DROP, SPROUT, FLOAT)
        /** The longest she stays out of sight waiting for her entrance's frames. */
        const val ENTER_WAIT_MS = 1_800L
        /** How often a moment looks again whether her entrance is over. */
        const val MOMENT_WAIT_MS = 250L
        val DANCE_FRAMES = listOf(R.drawable.peri_move_dance1, R.drawable.peri_move_dance2, R.drawable.peri_move_dance3, R.drawable.peri_move_dance4,
            R.drawable.peri_move_dance1, R.drawable.peri_move_dance2, R.drawable.peri_move_hips, R.drawable.peri_move_dance4)
        val TURN_STEPS = listOf(R.drawable.peri_turn_threequarter_right, R.drawable.peri_turn_side_right, R.drawable.peri_turn_threequarter_back_right,
            R.drawable.peri_turn_back, R.drawable.peri_turn_threequarter_back_left, R.drawable.peri_turn_side_left, R.drawable.peri_turn_threequarter_left)
        val TURNS = TURN_STEPS.toSet()
        /** Her turn's views start this far into a spin and take this share of it. */
        const val TURN_START = 0.06f
        const val TURN_SHARE = 0.84f
        /** Pictures she wears nothing in: turned away (her outfit can't follow her head), or still under the ground. */
        val BARE = TURNS + setOf(R.drawable.peri_anim_spin_02, R.drawable.peri_anim_spin_03, R.drawable.peri_anim_sprout_01)
        /** A bow: down, further down, up again, and herself; when each, in ms. */
        val BOW_STEPS = listOf(R.drawable.peri_move_bow1 to 0L, R.drawable.peri_move_bow2 to 220L, R.drawable.peri_move_bow1 to 760L, null to 960L)
        /** Each movement's own pictures. */
        val MOVES: Map<Idle, List<Int>> = mapOf(
            Idle.DANCE to DANCE_FRAMES.distinct(),
            Idle.SPIN to TURN_STEPS,
            Idle.STRETCH to listOf(R.drawable.peri_move_stretch_up),
            Idle.BOW to listOf(R.drawable.peri_move_bow1, R.drawable.peri_move_bow2),
        )
        val MOVE_FRAMES = MOVES.values.flatten().toSet() + listOf(JUMP, STAR, SPIN_JUMP, CHEER, DROP, SPROUT, FLOAT).flatMap { it.pictures }
        /** How long each movement owns the picture: up to just before she's herself again. */
        val MOVE_MS: Map<Idle, Long> = mapOf(
            Idle.DANCE to DANCE_STEPS * DANCE_MS - 20,
            Idle.SPIN to (SPIN_MS * (TURN_START + TURN_SHARE)).toLong() - 20,
            Idle.STRETCH to STRETCH_MS - 20,
            Idle.BOW to 940L,
        )
        const val DANCE_MS = 380L
        const val DANCE_STEPS = 8
        const val DROP_MS = 420f
        const val DROP_DP = 90f
        const val SPRING_K = 260f
        const val SPRING_C = 15f
        const val PRESS_SQUASH = 0.1f
        const val DRAG_K = 700f
        const val DRAG_C = 46f
        const val HOME_K = 190f
        const val HOME_C = 9f
        const val HOP_MS = 640f
        /** When a hop's feet touch the ground again. */
        const val HOP_LAND_MS = 480L
        const val HOP_DP = 18f
        const val STEP_MS = 340L
        const val STEPS = 8
        const val STRETCH_MS = 900L
        const val LAND_SQUASH = 0.07f
        const val NOD_MS = 170f
        const val NOD = 0.018f
        const val WAVE_START = 400L
        const val WAVE_STEP = 240L
        /** Where her feet are, as a fraction of the illustration's height. */
        const val FEET = 0.885f
        /** The shadow is a circle squashed flat onto the ground. */
        const val SHADOW_FLATNESS = 0.16f

        val MOTIONS = mapOf(
            Mood.CALM to Motion(tilt = 3f, tiltMs = 3_800, bob = 5f, bobMs = 1_900, sway = 2.5f, idles = listOf(Idle.HOP, Idle.STEPS, Idle.LOOK, Idle.WAVE, Idle.STRETCH, Idle.WINK, Idle.SPIN, Idle.DANCE, Idle.POSE, Idle.POSE, Idle.POSE, Idle.BOW)),
            Mood.LISTENING to Motion(tilt = 2f, tiltMs = 3_000, bob = 2.5f, bobMs = 1_500, sway = 1f),
            Mood.THINKING to Motion(tilt = 3.5f, tiltMs = 2_800, bob = 3f, bobMs = 1_400, sway = 1.5f, idles = listOf(Idle.LOOK, Idle.STEPS, Idle.STRETCH)),
            Mood.WORRIED to Motion(tilt = 1.5f, tiltMs = 2_000, bob = 1.5f, bobMs = 1_000, sway = 1.2f, idles = listOf(Idle.STEPS, Idle.LOOK)),
            Mood.PROTECTIVE to Motion(tilt = 1.2f, tiltMs = 3_200, bob = 2f, bobMs = 1_600, sway = 0.8f, idles = listOf(Idle.LOOK, Idle.STEPS)),
            Mood.CALLING to Motion(tilt = 2.5f, tiltMs = 2_800, bob = 3f, bobMs = 1_400, sway = 1.5f, idles = listOf(Idle.STEPS, Idle.LOOK, Idle.WINK)),
            Mood.WAITING to Motion(tilt = 4f, tiltMs = 4_400, bob = 2.5f, bobMs = 2_200, sway = 2.5f, idles = listOf(Idle.STEPS, Idle.LOOK, Idle.STRETCH, Idle.DANCE)),
            Mood.HAPPY to Motion(tilt = 4.5f, tiltMs = 1_800, bob = 16f, bobMs = 1_000, bounce = true, sway = 3f, idles = listOf(Idle.SPIN, Idle.WINK, Idle.HOP), runs = true),
            Mood.SLEEPING to Motion(tilt = 0.8f, tiltMs = 6_000, bob = 0f, bobMs = 1_000, breath = 0.03f, breathMs = 4_600, blinks = false, sway = 0f),
        )

        val FRAMES = mapOf(
            Mood.CALM to Frames(
                R.drawable.peri_calm, R.drawable.peri_calm_blinkhalf, R.drawable.peri_calm_blink, R.drawable.peri_calm_talk,
                R.drawable.peri_calm_talkwide, R.drawable.peri_calm_talko, R.drawable.peri_calm_glanceleft, R.drawable.peri_calm_glanceright,
                R.drawable.peri_calm_stepleft, R.drawable.peri_calm_stepright,
                R.drawable.peri_calm_gesturea, R.drawable.peri_calm_gestureb, R.drawable.peri_calm_wink,
                stepLeftHalf = R.drawable.peri_calm_steplefthalf, stepRightHalf = R.drawable.peri_calm_steprighthalf,
            ),
            Mood.LISTENING to Frames(
                R.drawable.peri_listening, R.drawable.peri_listening_blinkhalf, R.drawable.peri_listening_blink, R.drawable.peri_listening_talk,
                R.drawable.peri_listening_talkwide, R.drawable.peri_listening_talko, R.drawable.peri_listening_glanceleft, R.drawable.peri_listening_glanceright,
                R.drawable.peri_listening_stepleft, R.drawable.peri_listening_stepright,
                R.drawable.peri_listening_gesturea, R.drawable.peri_listening_gestureb, R.drawable.peri_listening_wink,
            ),
            Mood.THINKING to Frames(
                R.drawable.peri_thinking, R.drawable.peri_thinking_blinkhalf, R.drawable.peri_thinking_blink, R.drawable.peri_thinking_talk,
                R.drawable.peri_thinking_talkwide, R.drawable.peri_thinking_talko, R.drawable.peri_thinking_glanceleft, R.drawable.peri_thinking_glanceright,
                R.drawable.peri_thinking_stepleft, R.drawable.peri_thinking_stepright,
                R.drawable.peri_thinking_gesturea, R.drawable.peri_thinking_gestureb, R.drawable.peri_thinking_wink,
                stepLeftHalf = R.drawable.peri_thinking_steplefthalf, stepRightHalf = R.drawable.peri_thinking_steprighthalf,
            ),
            Mood.WORRIED to Frames(
                R.drawable.peri_worried, R.drawable.peri_worried_blinkhalf, R.drawable.peri_worried_blink, R.drawable.peri_worried_talk,
                R.drawable.peri_worried_talkwide, R.drawable.peri_worried_talko, R.drawable.peri_worried_glanceleft, R.drawable.peri_worried_glanceright,
                R.drawable.peri_worried_stepleft, R.drawable.peri_worried_stepright,
                R.drawable.peri_worried_gesturea, R.drawable.peri_worried_gestureb, R.drawable.peri_worried_wink,
                stepLeftHalf = R.drawable.peri_worried_steplefthalf, stepRightHalf = R.drawable.peri_worried_steprighthalf,
            ),
            Mood.PROTECTIVE to Frames(
                R.drawable.peri_protective, R.drawable.peri_protective_blinkhalf, R.drawable.peri_protective_blink, R.drawable.peri_protective_talk,
                R.drawable.peri_protective_talkwide, R.drawable.peri_protective_talko, R.drawable.peri_protective_glanceleft, R.drawable.peri_protective_glanceright,
                R.drawable.peri_protective_stepleft, R.drawable.peri_protective_stepright,
                R.drawable.peri_protective_gesturea, R.drawable.peri_protective_gestureb, R.drawable.peri_protective_wink,
                stepLeftHalf = R.drawable.peri_protective_steplefthalf, stepRightHalf = R.drawable.peri_protective_steprighthalf,
            ),
            Mood.CALLING to Frames(
                R.drawable.peri_calling, R.drawable.peri_calling_blinkhalf, R.drawable.peri_calling_blink, R.drawable.peri_calling_talk,
                R.drawable.peri_calling_talkwide, R.drawable.peri_calling_talko, R.drawable.peri_calling_glanceleft, R.drawable.peri_calling_glanceright,
                R.drawable.peri_calling_stepleft, R.drawable.peri_calling_stepright,
                R.drawable.peri_calling_gesturea, R.drawable.peri_calling_gestureb, R.drawable.peri_calling_wink,
                stepLeftHalf = R.drawable.peri_calling_steplefthalf, stepRightHalf = R.drawable.peri_calling_steprighthalf,
            ),
            Mood.WAITING to Frames(
                R.drawable.peri_waiting, R.drawable.peri_waiting_blinkhalf, R.drawable.peri_waiting_blink, R.drawable.peri_waiting_talk,
                R.drawable.peri_waiting_talkwide, R.drawable.peri_waiting_talko, R.drawable.peri_waiting_glanceleft, R.drawable.peri_waiting_glanceright,
                R.drawable.peri_waiting_stepleft, R.drawable.peri_waiting_stepright,
                R.drawable.peri_waiting_gesturea, R.drawable.peri_waiting_gestureb, R.drawable.peri_waiting_wink,
                stepLeftHalf = R.drawable.peri_waiting_steplefthalf, stepRightHalf = R.drawable.peri_waiting_steprighthalf,
            ),
            Mood.HAPPY to Frames(
                R.drawable.peri_happy, R.drawable.peri_happy_blinkhalf, R.drawable.peri_happy_blink, R.drawable.peri_happy_talk,
                R.drawable.peri_happy_talkwide, R.drawable.peri_happy_talko, R.drawable.peri_happy_glanceleft, R.drawable.peri_happy_glanceright,
                R.drawable.peri_happy_stepleft, R.drawable.peri_happy_stepright,
                R.drawable.peri_happy_gesturea, R.drawable.peri_happy_gestureb, R.drawable.peri_happy_wink,
                stepLeftHalf = R.drawable.peri_happy_steplefthalf, stepRightHalf = R.drawable.peri_happy_steprighthalf,
            ),
            Mood.SLEEPING to Frames(
                R.drawable.peri_sleeping, R.drawable.peri_sleeping_blinkhalf, R.drawable.peri_sleeping_blink, R.drawable.peri_sleeping_talk,
                R.drawable.peri_sleeping_talkwide, R.drawable.peri_sleeping_talko, R.drawable.peri_sleeping_glanceleft, R.drawable.peri_sleeping_glanceright,
            ),
        )

        val DESCRIPTIONS = mapOf(
            Mood.CALM to R.string.mood_calm,
            Mood.LISTENING to R.string.mood_listening,
            Mood.THINKING to R.string.mood_thinking,
            Mood.WORRIED to R.string.mood_worried,
            Mood.PROTECTIVE to R.string.mood_protective,
            Mood.CALLING to R.string.mood_calling,
            Mood.WAITING to R.string.mood_waiting,
            Mood.HAPPY to R.string.mood_happy,
            Mood.SLEEPING to R.string.mood_sleeping,
        )
    }
}
