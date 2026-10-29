package app.friendlypear.core.agent

import app.friendlypear.core.voice.Command
import app.friendlypear.core.voice.VoiceCommands
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * What people really write in "Ask Peri", as they write it: with typos, without accents, short or rambling, in Spanish
 * and in English. Each line must reach what it means: a topic of advice, an order, a chat, or a message pasted in to
 * check. Every miss is listed at once, so a change that breaks one shows them all.
 */
class ChatPhrasesTest {
    private sealed interface Want {
        data class Advice(val topics: Set<Topic>) : Want
        data class Order(val type: Class<out Command>) : Want
        data class Talk(val kind: SmallTalk) : Want
        data object Pasted : Want
        data object NotPasted : Want
    }

    private fun advice(vararg t: Topic) = Want.Advice(t.toSet())
    private fun order(type: Class<out Command>) = Want.Order(type)

    private val cases: List<Pair<String, Want>> = listOf(
        // Spanish: questions and situations.
        "me ha llamado mi banco" to advice(Topic.BANK_CALL),
        // "Me han robado": first aid for a robbery (call your bank on the number on your card) comes before advice.
        "me llamo el banco diciendo que me han robado" to order(Command.Gave::class.java),
        "me llamo el banco diciendo que hay un cargo raro" to advice(Topic.BANK_CALL),
        "me llamaron del banco santander" to advice(Topic.BANK_CALL),
        "me a llamado el banco q hago" to advice(Topic.BANK_CALL),
        "un señor dice que es del banco y me pide el codigo" to advice(Topic.BANK_CALL, Topic.CODE),
        "me ha llegado un sms con un codigo" to advice(Topic.CODE),
        "me piden el codigo que me ha llegado" to advice(Topic.CODE),
        "mi hijo me escribe desde otro numero y me pide dinero" to advice(Topic.FAMILY_MONEY),
        "un numero nuevo dice que es mi nieta" to advice(Topic.FAMILY_MONEY),
        "me han dicho que instale anydesk" to advice(Topic.REMOTE_APP),
        "me llamaron de microsoft que tengo un virus" to advice(Topic.REMOTE_APP),
        "me ha llegado un mensaje de correos" to advice(Topic.PARCEL),
        "tengo un paquete retenido en aduanas" to advice(Topic.PARCEL),
        "he ganado un iphone" to advice(Topic.PRIZE),
        "me ha tocado un premio" to advice(Topic.PRIZE),
        "es seguro este enlace" to advice(Topic.LINK),
        "puedo abrir un link que me mandaron" to advice(Topic.LINK),
        "me han robado el movil" to advice(Topic.STOLEN_PHONE),
        "he perdido el telefono" to advice(Topic.STOLEN_PHONE),
        "mi movil se ha quedado sin linea" to advice(Topic.SIM_SWAP),
        "como protejo mi whatsapp" to advice(Topic.WHATSAPP),
        "que es el phishing" to advice(Topic.WHAT_IS_A_SCAM),
        "que es un timo" to advice(Topic.WHAT_IS_A_SCAM),
        "no se si es un timo" to advice(Topic.WHAT_IS_A_SCAM, Topic.CHECK_MESSAGE),
        "me ha escrito un chico que conoci por internet y me pide dinero" to advice(Topic.ROMANCE),
        "quiero invertir en bitcoin" to advice(Topic.INVESTMENT),
        "me llama la policia" to advice(Topic.FAKE_AUTHORITY),
        "hacienda me ha mandado un mensaje" to advice(Topic.FAKE_AUTHORITY),
        "tengo una multa de trafico por sms" to advice(Topic.FAKE_AUTHORITY),
        "voy a comprar en una tienda online muy barata" to advice(Topic.SHOPPING),
        "me piden que haga un bizum" to advice(Topic.BIZUM),
        "cual es una buena contraseña" to advice(Topic.PASSWORDS),
        "me llama un numero que no conozco" to advice(Topic.UNKNOWN_CALL),
        "tengo un problema" to advice(Topic.TROUBLE),
        "Tengo un problema!!" to advice(Topic.TROUBLE),
        "creo que me han timado" to advice(Topic.TROUBLE),
        "ayudame con una cosa" to advice(Topic.TROUBLE),
        "estoy muy preocupada" to advice(Topic.TROUBLE),
        "me ha llegado un mensaje raro" to advice(Topic.CHECK_MESSAGE),
        "me an mandado un sms raro" to advice(Topic.CHECK_MESSAGE),
        // Spanish: orders and chat stay what they are.
        "llama a mi hija" to order(Command.Call::class.java),
        "llama a mi hija que tengo un problema" to order(Command.Call::class.java),
        "dile a pablo que llego tarde" to order(Command.SendMessage::class.java),
        "lee mis mensajes" to order(Command.ReadMessages::class.java),
        "me he caido" to order(Command.Emergency::class.java),
        "hola" to Want.Talk(SmallTalk.HELLO),
        "gracias peri" to Want.Talk(SmallTalk.THANKS),
        "cuentame un chiste" to Want.Talk(SmallTalk.JOKE),
        "me siento sola" to Want.Talk(SmallTalk.LONELY),
        // English.
        "my bank called me" to advice(Topic.BANK_CALL),
        "someone from the bank asked for my code" to advice(Topic.BANK_CALL, Topic.CODE),
        "i got a text with a code" to advice(Topic.CODE),
        "my son is texting from a new number asking for money" to advice(Topic.FAMILY_MONEY),
        "they want me to install anydesk" to advice(Topic.REMOTE_APP),
        "i got a message about a parcel" to advice(Topic.PARCEL),
        "i won a prize" to advice(Topic.PRIZE),
        "is this link safe" to advice(Topic.LINK),
        "my phone was stolen" to advice(Topic.STOLEN_PHONE),
        "how do i protect my whatsapp" to advice(Topic.WHATSAPP),
        "what is phishing" to advice(Topic.WHAT_IS_A_SCAM),
        "someone i met online wants money" to advice(Topic.ROMANCE),
        "should i invest in crypto" to advice(Topic.INVESTMENT),
        "the police called me" to advice(Topic.FAKE_AUTHORITY),
        "an unknown number keeps calling me" to advice(Topic.UNKNOWN_CALL),
        "i have a problem" to advice(Topic.TROUBLE),
        "i think i got scammed" to advice(Topic.TROUBLE),
        "i got a weird message" to advice(Topic.CHECK_MESSAGE),
        "call my daughter" to order(Command.Call::class.java),
        "tell laura i'm running late" to order(Command.SendMessage::class.java),
        "hello" to Want.Talk(SmallTalk.HELLO),
        "thank you" to Want.Talk(SmallTalk.THANKS),
        "tell me a joke" to Want.Talk(SmallTalk.JOKE),
        // A message pasted in, or retold with its link, amount or code: the shield looks at it.
        "Me ha llegado esto: Su cuenta ha sido bloqueada, verifique en bbva-online.es" to Want.Pasted,
        "BBVA: su cuenta ha sido bloqueada. Acceda a bbva-verificar.com para desbloquearla" to Want.Pasted,
        "Your parcel is waiting. Pay 1.99 GBP at royalmail-fee.co.uk today" to Want.Pasted,
        "Mama se me ha roto el movil, hazme un bizum de 300 euros a este numero" to Want.Pasted,
        "Tu codigo de verificacion es 482913. No lo compartas con nadie." to Want.Pasted,
        "me ha llamado el banco, que hago" to Want.NotPasted,
        "es seguro bbva.es" to Want.NotPasted,
        "llama a mi hija" to Want.NotPasted,
    )

    @Test
    fun `what people write reaches what it means`() {
        val misses = cases.mapNotNull { (text, want) ->
            val got = VoiceCommands.parse(text)
            val ok = when (want) {
                is Want.Advice -> got is Command.Advise && got.topic in want.topics
                is Want.Order -> want.type.isInstance(got)
                is Want.Talk -> got is Command.Chat && got.kind == want.kind
                Want.Pasted -> PastedMessage.find(text) != null
                Want.NotPasted -> PastedMessage.find(text) == null
            }
            if (ok) null else "\"$text\": wanted $want, got ${if (want is Want.Pasted || want is Want.NotPasted) PastedMessage.find(text) else got}"
        }
        assertTrue(misses.isEmpty(), "${misses.size} of ${cases.size} missed:\n" + misses.joinToString("\n"))
    }
}
