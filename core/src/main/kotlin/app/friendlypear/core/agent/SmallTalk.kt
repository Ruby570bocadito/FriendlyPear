package app.friendlypear.core.agent

/**
 * What people say to a friend: a hello, a thank you, "tell me a joke". Peri answers in her own words, written and
 * reviewed like the rest (Phrases), never a model's.
 */
enum class SmallTalk {
    HELLO, HOW_ARE_YOU, THANKS, WHO_ARE_YOU, WHAT_CAN_YOU_DO, JOKE, GOOD_NIGHT, BYE, LOVE, LONELY;

    companion object {
        /** Most specific first: "hola, ¿qué tal?" asks how she is, and "gracias, adiós" says goodbye. */
        private val patterns: List<Pair<SmallTalk, Regex>> = listOf(
            LONELY to Regex(
                """\b(me siento (sol[oa]|triste)|estoy (sol[oa]|triste|aburrid[oa])|me aburro|""" +
                    """i feel (lonely|sad)|i.?m (lonely|sad|bored)|i am (lonely|sad|bored))\b""",
            ),
            LOVE to Regex(
                """\b(te quiero|te adoro|eres (la mejor|genial|un sol|un amor)|""" +
                    """i love you|love you|you.?re (the best|great|lovely)|you are (the best|great|lovely))\b""",
            ),
            GOOD_NIGHT to Regex("""\b(buenas noches|me voy a (dormir|la cama|acostar)|a dormir|good ?night|night night|off to bed|going to bed)\b"""),
            BYE to Regex("""\b(adios|hasta luego|hasta pronto|hasta manana|chao|chau|nos vemos|bye|goodbye|see you|see ya)\b"""),
            WHO_ARE_YOU to Regex("""\b(quien eres|que eres|como te llamas|cual es tu nombre|who are you|what are you|what.?s your name|what is your name)\b"""),
            WHAT_CAN_YOU_DO to Regex(
                """\b(que sabes hacer|que puedes hacer|que haces|para que sirves|en que me (ayudas|puedes ayudar)|como me ayudas|""" +
                    """what can you do|what do you do|how can you help|what are you for)\b""",
            ),
            JOKE to Regex("""\b(chistes?|algo gracioso|hazme reir|hacerme reir|jokes?|make me laugh|something funny)\b"""),
            HOW_ARE_YOU to Regex("""\b(que tal|como estas|como te va|como andas|how are you|how are things|how.?s it going|how do you do)\b"""),
            THANKS to Regex("""\b(gracias|thank you|thanks|thank u|cheers)\b"""),
            HELLO to Regex("""\b(hola+|holi|buenas|buenos dias|buenas tardes|hey|hi|hiya|hello|good (morning|afternoon|evening))\b"""),
        )

        /** What [folded] (lowercase, without accents) says to Peri as a friend, if it says that. */
        fun of(folded: String): SmallTalk? = patterns.firstOrNull { (_, pattern) -> pattern.containsMatchIn(folded) }?.first
    }
}
