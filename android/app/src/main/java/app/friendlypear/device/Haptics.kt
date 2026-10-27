package app.friendlypear.device

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import app.friendlypear.core.agent.Mood

/**
 * Peri's touches, for people who don't hear well or don't look at the screen (docs/DISENO.md): a short tick
 * when she starts listening, one soft touch when she's worried, two firm ones when she protects you from a scam,
 * and a light double tick when she's happy. Warnings always vibrate; the small ticks follow the phone's own
 * "touch vibration" setting. No sounds: a beep from nowhere can frighten.
 */
class Haptics(context: Context) {
    private val context = context.applicationContext
    private val vibrator: Vibrator? = this.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private var last: Mood? = null

    /** Only when the mood changes to one that is felt, so a long warning buzzes once. */
    fun mood(mood: Mood) {
        if (mood == last) return
        last = mood
        when (mood) {
            Mood.PROTECTIVE -> play(VibrationEffect.createWaveform(longArrayOf(0, 140, 130, 140), intArrayOf(0, 220, 0, 220), -1))
            Mood.WORRIED -> play(VibrationEffect.createOneShot(90, 120))
            Mood.HAPPY -> if (ticksOn()) play(VibrationEffect.createWaveform(longArrayOf(0, 30, 80, 30), intArrayOf(0, 70, 0, 70), -1))
            else -> Unit
        }
    }

    fun listening() {
        if (ticksOn()) play(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }

    private fun ticksOn(): Boolean =
        runCatching { Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0 }.getOrDefault(true)

    private fun play(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(effect) }
    }
}
