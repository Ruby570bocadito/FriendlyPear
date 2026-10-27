package app.friendlypear.core.shield

import app.friendlypear.core.model.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallStoryTest {
    @Test
    fun `asking for a code, money, an app or the card is a scam whoever called`() {
        assertEquals(CallStory.Ask.CODE, CallStory.read("Del banco, querían el código que me llegó").mainAsk)
        assertEquals(CallStory.Ask.MOVE_MONEY, CallStory.read("La policía, que moviera el dinero a una cuenta segura").mainAsk)
        assertEquals(CallStory.Ask.HAND_OVER, CallStory.read("vendrá un mensajero a recoger la tarjeta").mainAsk)
        assertEquals(CallStory.Ask.INSTALL_APP, CallStory.read("Microsoft, que me descargara AnyDesk").mainAsk)
        assertEquals(CallStory.Ask.CARD_DETAILS, CallStory.read("they wanted my card number and the three digits").mainAsk)
        listOf("que les diera la contraseña", "my PIN, quickly", "transfer it to a safe account").forEach {
            assertEquals(Level.SCAM, CallStory.read(it).level, it)
        }
    }

    @Test
    fun `the worst ask is the one Peri talks about`() {
        val r = CallStory.read("el banco: mi DNI y luego el código del SMS")
        assertEquals(setOf(CallStory.Ask.CODE, CallStory.Ask.PERSONAL_DATA), r.asks)
        assertEquals(CallStory.Ask.CODE, r.mainAsk)
    }

    @Test
    fun `a claim alone is a doubt, nothing is ok`() {
        val bank = CallStory.read("del banco")
        assertEquals(setOf(CallStory.Claim.BANK), bank.claims)
        assertEquals(Level.DOUBT, bank.level)
        assertEquals(Level.DOUBT, CallStory.read("me dijo que no se lo dijera a nadie").level)
        assertEquals(Level.OK, CallStory.read("era la vecina para lo del sábado").level)
    }

    @Test
    fun `secrecy and hurry are noticed`() {
        val r = CallStory.read("Hacienda, que era urgente y que no se lo dijera a mis hijos")
        assertTrue(r.secrecy)
        assertTrue(r.urgency)
        assertEquals(setOf(CallStory.Claim.TAX), r.claims)
    }
}
