package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Level
import app.friendlypear.core.model.Relation
import app.friendlypear.core.model.TrustLevel
import app.friendlypear.core.shield.SafeAction
import app.friendlypear.core.shield.Shield
import app.friendlypear.core.shield.ShieldContext
import app.friendlypear.core.shield.Text
import app.friendlypear.core.shield.Verdict
import app.friendlypear.core.trust.Risk
import app.friendlypear.core.voice.Command
import app.friendlypear.core.voice.ContactDirectory
import app.friendlypear.core.voice.Resolution
import app.friendlypear.core.voice.VoiceCommands

/**
 * Peri's loop: perceive (messages, call state, voice) → evaluate (shield) → decide → ask for permission
 * by risk → act through [DevicePort] → check the result and fall back if it failed.
 *
 * Reading and analysing need no permission; calling and sending need a spoken "yes"; high-risk actions
 * need a guardian and are never done alone. A [DevicePort] call that throws counts as a failed action.
 */
class PeriAgent(
    private val device: DevicePort,
    private val contacts: List<Contact>,
    private val shield: Shield = Shield(),
) {
    private sealed interface Pending {
        data class Confirm(val action: Action, val question: String) : Pending
        data class Choose(val candidates: List<Contact>, val build: (Contact) -> Action, val question: String) : Pending
        data object Emergency : Pending
    }

    private data class Received(val message: IncomingMessage, val verdict: Verdict)

    private val directory = ContactDirectory(contacts)
    private val unread = ArrayDeque<Received>()
    private var pending: Pending? = null
    private var callActive = false
    private var lastSaid = ""

    /** True while Peri waits for a "yes", a name or a "cancel"; Android keeps the microphone open. */
    val isWaitingForAnswer: Boolean get() = pending != null

    /** True during the emergency countdown; Android calls [onEmergencyTimeout] when it ends. */
    val isCountingDown: Boolean get() = pending == Pending.Emergency

    fun onCallStateChanged(active: Boolean) {
        callActive = active
    }

    fun onMessage(message: IncomingMessage): Verdict {
        val verdict = shield.evaluate(message, ShieldContext(contacts, callActive))
        unread.addLast(Received(message, verdict))
        if (unread.size > MAX_UNREAD) unread.removeFirst()
        if (verdict.level == Level.OK || pending == Pending.Emergency) return verdict

        val real = if (verdict.safeAction == SafeAction.CALL_SAVED_CONTACT) family(verdict.claimedRelation) else emptyList()
        when (real.size) {
            0 -> say(verdict.explanation)
            1 -> {
                val question = "¿Llamo a ${real[0].name} a su número de siempre?"
                pending = Pending.Confirm(Action.Call(real[0]), question)
                say("${verdict.explanation} Si me dices que sí, llamo a ${real[0].name}.")
            }
            else -> {
                val question = "¿A quién llamo: ${SafeSpeech.names(real.map { it.name })}?"
                pending = Pending.Choose(real, { Action.Call(it) }, question)
                say("${verdict.explanation} $question")
            }
        }
        return verdict
    }

    fun onHeard(utterance: String) {
        val command = VoiceCommands.parse(utterance)
        if (command == Command.Emergency && pending != Pending.Emergency) {
            pending = null
            return handle(command)
        }
        when (val p = pending) {
            null -> handle(command)
            Pending.Emergency -> when (command) {
                Command.Cancel, Command.No -> {
                    pending = null
                    say("Vale, me alegro de que estés bien. No aviso a nadie.")
                }
                else -> say("Si estás bien, di «cancelar».")
            }
            is Pending.Confirm -> when (command) {
                Command.Yes -> {
                    pending = null
                    perform(p.action)
                }
                Command.No, Command.Cancel -> {
                    pending = null
                    say("Vale, no hago nada.")
                }
                Command.Repeat -> say(p.question)
                is Command.Unknown -> say("Dime «sí» o «no». ${p.question}")
                else -> {
                    pending = null
                    handle(command)
                }
            }
            is Pending.Choose -> when (command) {
                Command.No, Command.Cancel -> {
                    pending = null
                    say("Vale, no hago nada.")
                }
                Command.Repeat -> say(p.question)
                else -> {
                    val spoken = (command as? Command.Call)?.target ?: utterance
                    when (val r = ContactDirectory(p.candidates).resolve(spoken)) {
                        is Resolution.Found -> {
                            pending = null
                            request(p.build(r.contact))
                        }
                        else -> say(p.question)
                    }
                }
            }
        }
    }

    /** The countdown ended without a "cancel": text and call the family, never an emergency number. */
    fun onEmergencyTimeout() {
        if (pending != Pending.Emergency) return
        pending = null
        val family = emergencyContacts()
        if (family.isEmpty()) {
            say("No tengo a nadie de tu familia guardado para avisar. Si puedes, pide ayuda en voz alta.")
            return
        }
        val location = device.lastKnownLocation()
        val text = "Aviso de Peri: tu familiar ha dicho que se ha caído y no ha cancelado el aviso." +
            (location?.let { " Ubicación: https://maps.google.com/?q=$it" } ?: " No tengo su ubicación.")
        family.forEach { c -> c.phones.firstOrNull()?.let { phone -> attempt { device.sendSms(phone, text) } } }
        say("Aviso a ${family.first().name}.")
        if (family.none { c -> c.phones.any { attempt { device.call(it) } } }) {
            say("No he podido llamar a nadie. Les he mandado un mensaje; si puedes, pide ayuda en voz alta.")
        }
    }

    private fun handle(command: Command) {
        when (command) {
            is Command.Call -> resolve(command.target) { Action.Call(it) }
            is Command.SendMessage -> resolve(command.target) { Action.SendSms(it, command.text) }
            is Command.ReadMessages -> readMessages(command.onlyLast)
            Command.Emergency -> {
                pending = Pending.Emergency
                val who = emergencyContacts().firstOrNull()?.name ?: "tu familia"
                say("¿Estás bien? Si estás bien, di «cancelar». Si no, en diez segundos aviso a $who.")
            }
            Command.Yes -> say("¿Qué quieres que haga?")
            Command.No, Command.Cancel -> say("Vale.")
            Command.Repeat -> say(lastSaid.ifEmpty { "No te había dicho nada todavía." })
            is Command.Unknown -> say("No te he entendido. Puedes decirme, por ejemplo, «llama a Laura» o «léeme los mensajes».")
        }
    }

    private fun resolve(target: String, build: (Contact) -> Action) {
        when (val r = directory.resolve(target)) {
            is Resolution.Found ->
                if (r.contact.phones.isEmpty()) say("No tengo el número de ${r.contact.name}.") else request(build(r.contact))
            is Resolution.Ambiguous -> {
                val reachable = r.candidates.filter { it.phones.isNotEmpty() }
                when (reachable.size) {
                    0 -> say("No tengo el número de ${SafeSpeech.names(r.candidates.map { it.name })}.")
                    1 -> request(build(reachable.single()))
                    else -> {
                        val question = "¿A quién: ${SafeSpeech.names(reachable.map { it.name })}?"
                        pending = Pending.Choose(reachable, build, question)
                        say(question)
                    }
                }
            }
            Resolution.NotFound -> say("No encuentro a esa persona en tu agenda. ¿Cómo se llama?")
        }
    }

    /** Friction grows with risk: low acts, medium asks for a spoken "yes", high needs a guardian. */
    private fun request(action: Action) {
        when (action.risk) {
            Risk.LOW -> perform(action)
            Risk.MEDIUM -> {
                val question = when (action) {
                    is Action.Call -> "¿Llamo a ${action.contact.name}?"
                    is Action.SendSms -> "¿Le mando a ${action.contact.name} este mensaje: «${action.text}»?"
                }
                pending = Pending.Confirm(action, question)
                say(question)
            }
            Risk.HIGH -> {
                pending = null
                say("Mandar un código es arriesgado: eso tiene que aprobarlo alguien de tu familia, así que no lo mando.")
            }
        }
    }

    /** Acts and checks the result: tries each saved number, then tells the person what to do instead. */
    private fun perform(action: Action) {
        val name = action.contact.name
        val phones = action.contact.phones
        when (action) {
            is Action.Call -> {
                say("Llamo a $name.")
                if (phones.none { attempt { device.call(it) } }) {
                    say("No he podido llamar a $name. Su número es ${SafeSpeech.phone(phones.first())}; puedes marcarlo tú.")
                }
            }
            is Action.SendSms -> {
                if (phones.any { attempt { device.sendSms(it, action.text) } }) {
                    say("Mensaje enviado a $name.")
                } else {
                    val question = "No he podido mandar el mensaje a $name. ¿Quieres que llame?"
                    pending = Pending.Confirm(Action.Call(action.contact), question)
                    say(question)
                }
            }
        }
    }

    private fun readMessages(onlyLast: Boolean) {
        if (unread.isEmpty()) return say("No tienes mensajes nuevos.")
        val items = if (onlyLast) listOf(unread.removeLast()) else unread.toList().also { unread.clear() }
        val intro = if (items.size > 1) "Tienes ${items.size} mensajes. " else ""
        say(intro + items.joinToString(" ") { describe(it) })
    }

    private fun describe(r: Received): String {
        val from = "De ${senderName(r.message.sender)}:"
        return when (r.verdict.level) {
            Level.OK -> "$from ${SafeSpeech.clean(r.message.text)}"
            Level.DOUBT -> "$from ${SafeSpeech.clean(r.message.text)} Ojo: ${r.verdict.explanation}"
            Level.SCAM -> "$from no te lo leo. ${r.verdict.explanation}"
        }
    }

    private fun senderName(sender: String): String {
        directory.byPhone(sender)?.let { return it.name }
        val folded = Text.fold(sender).trim()
        contacts.firstOrNull { c -> Text.fold(c.name) == folded || c.aliases.any { Text.fold(it) == folded } }?.let { return it.name }
        return when {
            '@' in sender -> "una dirección de correo que no está en tu agenda"
            sender.count { it.isDigit() } >= 3 -> "un número que no está en tu agenda"
            else -> "«${SafeSpeech.clean(sender)}»"
        }
    }

    private fun family(relation: Relation?): List<Contact> =
        if (relation == null) emptyList() else contacts.filter { it.relation == relation && it.phones.isNotEmpty() }

    private fun emergencyContacts(): List<Contact> {
        val reachable = contacts.filter { it.phones.isNotEmpty() }
        return reachable.filter { it.trust == TrustLevel.GUARDIAN }
            .ifEmpty { reachable.filter { it.relation in setOf(Relation.CHILD, Relation.PARTNER, Relation.SIBLING, Relation.GRANDCHILD) } }
    }

    private fun attempt(block: () -> Unit): Boolean = try {
        block()
        true
    } catch (e: Exception) {
        false
    }

    private fun say(text: String) {
        lastSaid = text
        device.speak(text)
    }

    private companion object {
        const val MAX_UNREAD = 50
    }
}
