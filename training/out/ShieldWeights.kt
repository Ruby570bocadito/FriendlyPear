        /** Fitted on training/data (synthetic, Spark contributor); reported in eval/REPORT.md. */
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
