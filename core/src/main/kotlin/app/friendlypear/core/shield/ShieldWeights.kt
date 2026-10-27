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
         * scams better (AUC 0.965 vs 0.957) and warns firmly on 66.7% of scams instead of 52.9%; but on the 20/20
         * set it makes a genuine gym reminder a firm scam. Not the default until real legitimate messages close
         * that gap (eval/REPORT.md).
         */
        val LEARNED = ShieldWeights(
            bias = -3.55,
            perSignal = mapOf(
                Signal.UNKNOWN_SENDER to 0.92,
                Signal.LINK to 2.0,
                Signal.URL_SHORTENER to 1.9,
                Signal.INSECURE_LINK to 2.77,
                Signal.BRAND_LOOKALIKE to 4.02,
                Signal.URGENCY to 2.56,
                Signal.MONEY_REQUEST to 2.07,
                Signal.CREDENTIAL_REQUEST to 2.45,
                Signal.CODE_FORWARD_REQUEST to 2.0,
                Signal.NEW_NUMBER_CLAIM to 2.61,
                Signal.PRIZE_OR_PROFIT to 2.08,
                Signal.REMOTE_ACCESS_APP to 3.29,
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
                Signal.REMOTE_ACCESS_APP to 3.0,
                Signal.AI_INSTRUCTIONS to 3.0,
                Signal.ONE_TIME_CODE to 0.0,
                Signal.CODE_DURING_CALL to 4.0,
            ),
        )
    }
}
