package app.friendlypear.core.shield

import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Relation
import kotlin.math.exp

/** The one thing Peri suggests doing instead of what the message asks. */
enum class SafeAction { NONE, HANG_UP, CALL_SAVED_CONTACT, USE_OFFICIAL_APP, DONT_TOUCH_LINK, ASK_FAMILY }

data class Verdict(
    val level: Level,
    /** Log-odds from the rules alone. */
    val rulesScore: Double,
    /** What a model added, always within ±[Shield.MAX_MODEL_DELTA]. */
    val modelDelta: Double,
    val signals: Set<Signal>,
    val hardRule: HardRule?,
    val safeAction: SafeAction,
    /** Peri's sentence. Never contains links or codes, so it is safe to read aloud. */
    val explanation: String,
    /** The brand the message imitates or names, for "use the official app". */
    val brand: String?,
    /** Who the sender claims to be ("mamá…" → a son or daughter), to find the real one in the contacts. */
    val claimedRelation: Relation?,
) {
    val score: Double get() = rulesScore + modelDelta
    val probability: Double get() = 1.0 / (1.0 + exp(-score))
}

/**
 * Layer 1 of the shield: deterministic rules on the phone. Doubtful verdicts go on to Muse Glimmer and,
 * if still doubtful, to Muse Spark; their opinion enters through [withModelOpinion].
 */
class Shield(
    private val weights: ShieldWeights = ShieldWeights.PROVISIONAL,
    /** The language of Peri's sentence; signals are looked for in every language. */
    private val lang: Lang = Lang.ES,
) {

    fun evaluate(message: IncomingMessage, context: ShieldContext): Verdict {
        val detection = SignalDetector.detect(message, context)
        val score = weights.bias + detection.signals.sumOf { weights.of(it) }
        val hardRule = when {
            Signal.CODE_DURING_CALL in detection.signals -> HardRule.CODE_DURING_CALL
            Signal.BRAND_LOOKALIKE in detection.signals -> HardRule.BRAND_LOOKALIKE
            else -> null
        }
        val brand = detection.impersonated ?: detection.mentioned.firstOrNull()
        return build(score, 0.0, detection.signals, hardRule, brand?.name, detection.claimedRelation)
    }

    /**
     * A model's opinion moves the score at most ±2 and replaces any earlier model opinion.
     * It can never lower a verdict a hard rule decided.
     */
    fun withModelOpinion(verdict: Verdict, delta: Double): Verdict {
        require(delta.isFinite()) { "model delta must be a finite number" }
        val clamped = delta.coerceIn(-MAX_MODEL_DELTA, MAX_MODEL_DELTA)
        return build(verdict.rulesScore, clamped, verdict.signals, verdict.hardRule, verdict.brand, verdict.claimedRelation)
    }

    private fun build(
        rulesScore: Double,
        modelDelta: Double,
        signals: Set<Signal>,
        hardRule: HardRule?,
        brand: String?,
        claimedRelation: Relation?,
    ): Verdict {
        val level = if (hardRule != null) Level.SCAM else levelFor(rulesScore + modelDelta)
        val action = safeAction(level, signals, brand)
        return Verdict(
            level = level,
            rulesScore = rulesScore,
            modelDelta = modelDelta,
            signals = signals,
            hardRule = hardRule,
            safeAction = action,
            explanation = PeriExplainer.explain(level, hardRule, signals, action, brand, lang),
            brand = brand,
            claimedRelation = claimedRelation,
        )
    }

    private fun levelFor(score: Double): Level = when {
        score >= weights.scamAt -> Level.SCAM
        score >= weights.doubtAt -> Level.DOUBT
        else -> Level.OK
    }

    private fun safeAction(level: Level, signals: Set<Signal>, brand: String?): SafeAction = when {
        level == Level.OK -> SafeAction.NONE
        Signal.CODE_DURING_CALL in signals -> SafeAction.HANG_UP
        Signal.NEW_NUMBER_CLAIM in signals -> SafeAction.CALL_SAVED_CONTACT
        Signal.REMOTE_ACCESS_APP in signals -> SafeAction.ASK_FAMILY
        brand != null -> SafeAction.USE_OFFICIAL_APP
        Signal.LINK in signals -> SafeAction.DONT_TOUCH_LINK
        else -> SafeAction.ASK_FAMILY
    }

    companion object {
        const val MAX_MODEL_DELTA = 2.0
    }
}
