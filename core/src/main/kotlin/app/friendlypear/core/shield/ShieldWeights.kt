package app.friendlypear.core.shield

/**
 * Log-odds the rules add per signal, and where the levels start. The shield uses the hand-set [PROVISIONAL];
 * [LEARNED] were fitted from them on data and are measured in eval/REPORT.md.
 */
data class ShieldWeights(
    val bias: Double,
    val perSignal: Map<Signal, Double>,
    val doubtAt: Double = 0.0,
    val scamAt: Double = 2.0,
) {
    init {
        require(doubtAt < scamAt) { "doubtAt must be below scamAt" }
    }

    fun of(signal: Signal): Double = perSignal[signal] ?: 0.0

    companion object {
        /**
         * Learned by logistic regression on 1,306 synthetic messages (training/fit.py), shrunk towards [PROVISIONAL]
         * with the strength cross-validation picked. On the hand-written held-out set (eval/reservado-200) it ranks
         * scams better (AUC 0.972 vs 0.964) with no firm false alarm, but stays silent on more scams (83 % flagged vs
         * 88 %). Not the default: a missed scam costs more than a doubt the model can still calm (eval/REPORT.md).
         */
        val LEARNED = ShieldWeights(
            bias = -3.69,
            perSignal = mapOf(
                Signal.UNKNOWN_SENDER to 0.79,
                Signal.LINK to 2.24,
                Signal.URL_SHORTENER to 1.89,
                Signal.INSECURE_LINK to 2.78,
                Signal.BRAND_LOOKALIKE to 4.04,
                Signal.URGENCY to 2.12,
                Signal.MONEY_REQUEST to 1.96,
                Signal.CREDENTIAL_REQUEST to 2.57,
                Signal.CODE_FORWARD_REQUEST to 2.22,
                Signal.NEW_NUMBER_CLAIM to 2.82,
                Signal.PRIZE_OR_PROFIT to 2.09,
                Signal.SECRECY to 1.87,
                Signal.DEVICE_SCARE to 2.42,
                Signal.ROMANCE_BAIT to 2.09,
                Signal.REMOTE_ACCESS_APP to 3.11,
                Signal.AI_INSTRUCTIONS to 3.0,
                Signal.ONE_TIME_CODE to 0.0,
                Signal.CODE_DURING_CALL to 4.0,
            ),
        )

        val PROVISIONAL = ShieldWeights(
            bias = -3.0,
            perSignal = mapOf(
                Signal.UNKNOWN_SENDER to 1.0,
                Signal.LINK to 0.5,
                Signal.URL_SHORTENER to 1.5,
                // With a link (0.5) it reaches "doubt" even from a contact: Peri always mentions an unsafe link.
                Signal.INSECURE_LINK to 2.5,
                Signal.BRAND_LOOKALIKE to 4.0,
                Signal.URGENCY to 1.0,
                Signal.MONEY_REQUEST to 1.5,
                Signal.CREDENTIAL_REQUEST to 2.0,
                Signal.CODE_FORWARD_REQUEST to 2.0,
                Signal.NEW_NUMBER_CLAIM to 2.5,
                Signal.PRIZE_OR_PROFIT to 2.0,
                Signal.SECRECY to 1.5,
                Signal.DEVICE_SCARE to 2.0,
                Signal.ROMANCE_BAIT to 1.5,
                Signal.REMOTE_ACCESS_APP to 3.0,
                Signal.AI_INSTRUCTIONS to 3.0,
                Signal.ONE_TIME_CODE to 0.0,
                Signal.CODE_DURING_CALL to 4.0,
            ),
        )
    }
}
