package app.friendlypear.ui.peri

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.provider.Settings
import android.widget.ImageView
import app.friendlypear.R
import app.friendlypear.core.agent.Mood

/**
 * Peri on screen: one Muse Image illustration per mood (assets/peri/), a soft cross-fade between them and a
 * slow breath. With "remove animations" on, she changes without moving (docs/DISENO.md).
 */
class PeriFace(context: Context) : ImageView(context) {
    private var shown: Mood? = null
    private val moves = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    private var breath: ObjectAnimator? = null

    init {
        scaleType = ScaleType.FIT_CENTER
        adjustViewBounds = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    fun show(mood: Mood) {
        if (mood == shown) return
        val first = shown == null
        shown = mood
        contentDescription = context.getString(DESCRIPTIONS.getValue(mood))
        if (!moves || first) return setImageResource(IMAGES.getValue(mood))
        animate().alpha(0f).setDuration(FADE_MILLIS).withEndAction {
            setImageResource(IMAGES.getValue(mood))
            animate().alpha(1f).setDuration(FADE_MILLIS).start()
        }.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!moves) return
        breath = ObjectAnimator.ofPropertyValuesHolder(
            this,
            PropertyValuesHolder.ofFloat(SCALE_X, 1f, 1.03f),
            PropertyValuesHolder.ofFloat(SCALE_Y, 1f, 1.03f),
        ).apply {
            duration = BREATH_MILLIS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    override fun onDetachedFromWindow() {
        breath?.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        const val FADE_MILLIS = 180L
        const val BREATH_MILLIS = 1_500L

        val IMAGES = mapOf(
            Mood.CALM to R.drawable.peri_calm,
            Mood.LISTENING to R.drawable.peri_listening,
            Mood.THINKING to R.drawable.peri_thinking,
            Mood.WORRIED to R.drawable.peri_worried,
            Mood.PROTECTIVE to R.drawable.peri_protective,
            Mood.CALLING to R.drawable.peri_calling,
            Mood.WAITING to R.drawable.peri_waiting,
            Mood.HAPPY to R.drawable.peri_happy,
            Mood.SLEEPING to R.drawable.peri_sleeping,
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
