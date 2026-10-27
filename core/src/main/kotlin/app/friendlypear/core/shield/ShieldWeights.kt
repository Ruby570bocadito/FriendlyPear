package app.friendlypear.core.shield

/**
 * Log-odds the rules add per signal, and where the levels start. [PROVISIONAL] is set by hand until
 * training/ learns them with logistic regression (pesos-aprendidos).
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
        val PROVISIONAL = ShieldWeights(
            bias = -3.0,
            perSignal = mapOf(
                Signal.UNKNOWN_SENDER to 1.0,
                Signal.LINK to 0.5,
                Signal.URL_SHORTENER to 1.5,
                Signal.BRAND_LOOKALIKE to 4.0,
                Signal.URGENCY to 1.0,
                Signal.MONEY_REQUEST to 1.5,
                Signal.CREDENTIAL_REQUEST to 2.0,
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
