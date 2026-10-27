        /** Fitted on training/data (synthetic, Spark contributor); reported in eval/REPORT.md. */
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
