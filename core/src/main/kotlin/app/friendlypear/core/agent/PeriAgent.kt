package app.friendlypear.core.agent

import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Lang
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
    /** The language Peri speaks; orders are understood in Spanish and English whatever it is. */
    lang: Lang = Lang.ES,
    private val shield: Shield = Shield(lang = lang),
    /** Whether the family agreed a secret word. Peri reminds people to ask for it, and never says it. */
    private val hasFamilyWord: Boolean = false,
    /** Family MFA for high-risk actions; without it they are refused, never done alone. */
    private val approvals: ApprovalPort? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** A high-risk action waiting for a guardian. Kept apart from [pending]: Peri keeps helping meanwhile. */
    private data class AwaitingGuardian(val ticket: String, val action: Action)

    private var awaitingGuardian: AwaitingGuardian? = null
    private var tickets = 0

    /** True while a guardian's answer is awaited; Android polls the backend until then. */
    val isAwaitingGuardian: Boolean get() = awaitingGuardian != null
    private val p = Phrases.of(lang)
    private val speech = SafeSpeech(p)

    private sealed interface Pending {
        data class Confirm(val action: Action, val question: String) : Pending
        data class Choose(val candidates: List<Contact>, val build: (Contact) -> Action, val question: String) : Pending
        data object Emergency : Pending
        /** After a call from an unknown number: "who did they say they were?" */
        data class WhoClaimed(val number: String) : Pending
    }

    /** An incoming call from a number not in the contacts, whether it was answered, and if Peri already warned about the bank. */
    private data class StrangeCall(val number: String, var answered: Boolean = false, var bankWarned: Boolean = false)

    private data class Received(val message: IncomingMessage, val verdict: Verdict)

    /** Something worrying happened a moment ago; for a while, opening a money app gets a safety pause, once. */
    private data class RecentRisk(val kind: RecentKind, val until: Long, val relation: Relation?, var paused: Boolean = false)

    private var recent: RecentRisk? = null
    /** The call the safety pause offered, so its button only ever confirms that offer. */
    private var pauseOffer: Pending.Confirm? = null

    /**
     * Whether Peri should notice which app comes to the front: during a picked-up call with a stranger, and for
     * [RISK_WINDOW_MILLIS] after a doubtful message, a scam or a stranger's call. Never otherwise.
     */
    /** A picked-up call with a number that isn't in the contacts is going on right now. */
    val strangerOnLine: Boolean get() = callActive && strangeCall?.answered == true

    val watchesApps: Boolean
        get() = (callActive && strangeCall?.answered == true) || (recent?.let { clock() < it.until } == true)

    private val directory = ContactDirectory(contacts)
    private val unread = ArrayDeque<Received>()
    private val awaitingOpinion = mutableSetOf<IncomingMessage>()
    private var pending: Pending? = null
    private var callActive = false
    private var strangeCall: StrangeCall? = null
    private var lastSaid = ""

    /** How Peri looks right now; the screen shows the matching illustration. */
    var mood: Mood = Mood.CALM
        private set

    /** True while Peri waits for a "yes", a name or a "cancel"; Android keeps the microphone open. */
    val isWaitingForAnswer: Boolean get() = pending != null

    /** True during the emergency countdown; Android calls [onEmergencyTimeout] when it ends. */
    val isCountingDown: Boolean get() = pending == Pending.Emergency

    /**
     * Call state: [active] while ringing or talking; [answered] once picked up. When a call from an unknown
     * number that was answered ends, Peri asks who the caller said they were. Peri never hears the call itself.
     */
    fun onCallStateChanged(active: Boolean, answered: Boolean = active) {
        val wasActive = callActive
        callActive = active
        val call = strangeCall
        if (active && answered) call?.answered = true
        if (wasActive && !active) {
            strangeCall = null
            // The call is over: Peri puts the phone down.
            if (mood == Mood.CALLING) mood = Mood.CALM
            if (call?.answered == true) recent = RecentRisk(RecentKind.CALL, clock() + RISK_WINDOW_MILLIS, null)
            if (call?.answered == true && pending == null) {
                pending = Pending.WhoClaimed(call.number)
                say(p.whoClaimed, Mood.WORRIED)
            }
        }
    }

    /**
     * A new app was installed. If it lets someone control the phone remotely, Peri warns, says to hang up if a
     * call is going on, and offers to tell the family; the text to them is sent only after a spoken "yes".
     */
    fun onAppInstalled(packageName: String) {
        val app = RemoteApps.nameOf(packageName) ?: return
        val warning = p.remoteAppInstalled(app, onCall = callActive)
        val guardian = emergencyContacts().firstOrNull()
        if (guardian == null || pending == Pending.Emergency) return say(warning, Mood.PROTECTIVE)
        val question = p.tellFamily(guardian.name)
        pending = Pending.Confirm(Action.SendSms(guardian, p.remoteAppFamilyText(app)), question)
        say("$warning $question", Mood.PROTECTIVE)
    }

    /**
     * An app came to the front. A banking app opened while talking to an unknown number is the fake-bank
     * script, so Peri says to hang up and call the bank herself. Once per call; never during a family call.
     */
    /**
     * A money or remote-control app came to the front. During a stranger's call, or a while after something
     * worrying, Peri stops the person for a moment over it (the safety pause), once each time.
     */
    fun onAppOpened(packageName: String) {
        val money = BankApps.isBank(packageName)
        val remote = RemoteApps.nameOf(packageName)
        if (!money && remote == null || pending == Pending.Emergency) return
        val call = strangeCall
        if (call != null && callActive) {
            if (!call.answered || call.bankWarned) return
            call.bankWarned = true
            // Still on the phone: first hang up. No call to the family offered until then.
            say(if (remote != null) p.remoteDuringCall(remote) else p.bankDuringCall, Mood.PROTECTIVE)
            return device.showPause(lastSaid, null)
        }
        val risk = recent?.takeIf { clock() < it.until && !it.paused } ?: return
        risk.paused = true
        val who = family(risk.relation).singleOrNull() ?: emergencyContacts().firstOrNull()
        val text = p.pauseAfter(risk.kind, remote)
        if (who == null) {
            say(text, Mood.PROTECTIVE)
            return device.showPause(lastSaid, null)
        }
        val question = p.pauseOffer(who.name)
        pending = Pending.Confirm(Action.Call(who), question).also { pauseOffer = it }
        say("$text $question", Mood.PROTECTIVE)
        device.showPause(lastSaid, who.name)
    }

    /** The person's choice on the safety pause. Calling is a choice they made, like a spoken "yes". */
    fun onPauseAnswer(answer: PauseAnswer) {
        val offered = (pending as? Pending.Confirm)?.takeIf { it === pauseOffer }
        pauseOffer = null
        when (answer) {
            PauseAnswer.CALL_FAMILY -> offered?.let {
                pending = null
                perform(it.action)
            }
            PauseAnswer.LEAVE -> {
                if (offered != null) pending = null
                say(p.pauseLeft, Mood.HAPPY)
            }
            PauseAnswer.CONTINUE -> {
                if (offered != null) pending = null
                say(p.pauseContinue, Mood.CALM)
            }
        }
    }

    /** Before picking up: warns when the number isn't in the contacts. Never blocks or answers the call. */
    fun onIncomingCall(number: String) {
        if (directory.byPhone(number) != null) {
            strangeCall = null
            return
        }
        strangeCall = StrangeCall(number)
        say(p.unknownCaller(hasFamilyWord), Mood.WORRIED)
    }

    /**
     * Evaluates a message and warns if needed. With [secondOpinion], a doubtful message waits silently for
     * [onModelOpinion] instead, so Peri speaks once, with the model's vote already counted.
     */
    fun onMessage(message: IncomingMessage, secondOpinion: Boolean = false): Verdict {
        val verdict = shield.evaluate(message, ShieldContext(contacts, callActive))
        unread.addLast(Received(message, verdict))
        if (unread.size > MAX_UNREAD) unread.removeFirst()
        if (secondOpinion && verdict.level == Level.DOUBT) {
            awaitingOpinion += message
            if (pending != Pending.Emergency) mood = Mood.THINKING
            return verdict
        }
        announce(verdict)
        return verdict
    }

    /**
     * The model's vote for a message held by [onMessage], or null when it failed or timed out: then the
     * rules' verdict stands. The vote moves the score by at most ±2 and Peri still speaks its own sentence,
     * never the model's, so a message cannot talk the model into saying something unsafe.
     */
    fun onModelOpinion(message: IncomingMessage, delta: Double?): Verdict? {
        if (!awaitingOpinion.remove(message)) return null
        val index = unread.indexOfFirst { it.message == message }
        val before = if (index >= 0) unread[index].verdict else shield.evaluate(message, ShieldContext(contacts, callActive))
        val after = if (delta == null) before else shield.withModelOpinion(before, delta)
        if (index >= 0) unread[index] = Received(message, after)
        if (after.level == Level.OK && mood == Mood.THINKING) mood = Mood.CALM
        announce(after)
        return after
    }

    private fun announce(verdict: Verdict) {
        if (verdict.level != Level.OK) {
            val kind = if (verdict.level == Level.SCAM) RecentKind.SCAM else RecentKind.DOUBT
            recent = RecentRisk(kind, clock() + RISK_WINDOW_MILLIS, verdict.claimedRelation)
        }
        if (verdict.level == Level.OK || pending == Pending.Emergency) return
        val warned = if (verdict.level == Level.SCAM) Mood.PROTECTIVE else Mood.WORRIED

        val real = if (verdict.safeAction == SafeAction.CALL_SAVED_CONTACT) family(verdict.claimedRelation) else emptyList()
        when (real.size) {
            0 -> say(verdict.explanation, warned)
            1 -> {
                pending = Pending.Confirm(Action.Call(real[0]), p.callUsual(real[0].name))
                say(p.offerCall(verdict.explanation, real[0].name), warned)
            }
            else -> {
                val question = p.whoToCall(speech.names(real.map { it.name }))
                pending = Pending.Choose(real, { Action.Call(it) }, question)
                say("${verdict.explanation} $question", warned)
            }
        }
    }

    fun onHeard(utterance: String) {
        val command = VoiceCommands.parse(utterance)
        if (command == Command.Emergency && pending != Pending.Emergency) {
            pending = null
            return handle(command)
        }
        when (val w = pending) {
            null -> handle(command)
            Pending.Emergency -> when (command) {
                Command.Cancel, Command.No -> {
                    pending = null
                    say(p.emergencyCancelled, Mood.HAPPY)
                }
                else -> say(p.sayCancelIfFine, Mood.WORRIED)
            }
            is Pending.WhoClaimed -> {
                pending = null
                answerWhoClaimed(w.number, utterance, command)
            }
            is Pending.Confirm -> when (command) {
                Command.Yes -> {
                    pending = null
                    perform(w.action)
                }
                Command.No, Command.Cancel -> {
                    pending = null
                    say(p.doingNothing)
                }
                Command.Repeat -> say(w.question)
                is Command.Unknown -> say(p.yesOrNo(w.question))
                else -> {
                    pending = null
                    handle(command)
                }
            }
            is Pending.Choose -> when (command) {
                Command.No, Command.Cancel -> {
                    pending = null
                    say(p.doingNothing)
                }
                Command.Repeat -> say(w.question)
                else -> {
                    val spoken = (command as? Command.Call)?.target ?: utterance
                    when (val r = ContactDirectory(w.candidates).resolve(spoken)) {
                        is Resolution.Found -> {
                            pending = null
                            request(w.build(r.contact))
                        }
                        else -> say(w.question)
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
            say(p.noFamily, Mood.WORRIED)
            return
        }
        val location = device.lastKnownLocation()
        val text = p.emergencySms(location)
        family.forEach { c -> c.phones.firstOrNull()?.let { phone -> attempt { device.sendSms(phone, text) } } }
        say(p.alerting(family.first().name), Mood.CALLING)
        if (family.none { c -> c.phones.any { attempt { device.call(it) } } }) {
            say(p.couldNotCallAnyone, Mood.WORRIED)
        }
    }

    /**
     * "Mi nieto" → that number isn't Pablo's → offer to call Pablo on his saved number. A bank or a company →
     * hang up and call the official number. Nobody, or someone unknown → general advice.
     */
    private fun answerWhoClaimed(number: String, utterance: String, command: Command) {
        if (command == Command.No || command == Command.Cancel) return say(p.callerAdvice)
        if (officialCaller.containsMatchIn(Text.fold(utterance))) return say(p.officialCaller, Mood.PROTECTIVE)
        when (val r = directory.resolve(utterance)) {
            is Resolution.Found -> {
                val contact = r.contact
                if (contact.phones.isEmpty() || contact.phones.any { Text.phoneDigits(it) == Text.phoneDigits(number) }) {
                    return say(p.callerAdvice)
                }
                pending = Pending.Confirm(Action.Call(contact), p.callUsual(contact.name))
                say(p.notTheirNumber(contact.name, hasFamilyWord), Mood.PROTECTIVE)
            }
            is Resolution.Ambiguous -> {
                val reachable = r.candidates.filter { it.phones.isNotEmpty() }
                if (reachable.isEmpty()) return say(p.callerAdvice)
                val question = p.notAnyOf(speech.names(reachable.map { it.name }))
                pending = Pending.Choose(reachable, { Action.Call(it) }, question)
                say(question)
            }
            Resolution.NotFound -> say(p.callerAdvice)
        }
    }

    private fun handle(command: Command) {
        when (command) {
            is Command.Call -> resolve(command.target) { Action.Call(it) }
            is Command.SendMessage -> resolve(command.target) { Action.SendSms(it, command.text) }
            is Command.ReadMessages -> readMessages(command.onlyLast)
            Command.Emergency -> {
                pending = Pending.Emergency
                say(p.countdown(emergencyContacts().firstOrNull()?.name), Mood.WORRIED)
                // Whatever is still missing is asked during the countdown; when it ends, Peri uses what it has.
                device.prepare(Capability.CALL) { device.prepare(Capability.SEND_SMS) { device.prepare(Capability.LOCATION) {} } }
            }
            Command.Yes -> say(p.whatToDo)
            Command.No, Command.Cancel ->
                if (awaitingGuardian != null) {
                    // Changing one's mind cancels the request: a later yes from the family won't send anything.
                    awaitingGuardian = null
                    say(p.doingNothing)
                } else {
                    say(p.ok)
                }
            Command.Repeat -> say(lastSaid.ifEmpty { p.nothingSaidYet })
            is Command.Unknown -> say(p.help)
        }
    }

    private fun resolve(target: String, build: (Contact) -> Action) {
        when (val r = directory.resolve(target)) {
            is Resolution.Found ->
                if (r.contact.phones.isEmpty()) say(p.noNumber(r.contact.name)) else request(build(r.contact))
            is Resolution.Ambiguous -> {
                val reachable = r.candidates.filter { it.phones.isNotEmpty() }
                when (reachable.size) {
                    0 -> say(p.noNumber(speech.names(r.candidates.map { it.name })))
                    1 -> request(build(reachable.single()))
                    else -> {
                        val question = p.whichOne(speech.names(reachable.map { it.name }))
                        pending = Pending.Choose(reachable, build, question)
                        say(question)
                    }
                }
            }
            Resolution.NotFound -> say(p.notFound)
        }
    }

    /** Friction grows with risk: low acts, medium asks for a spoken "yes", high needs a guardian. */
    private fun request(action: Action) {
        when (action.risk) {
            Risk.LOW -> perform(action)
            Risk.MEDIUM -> {
                val question = when (action) {
                    is Action.Call -> p.confirmCall(action.contact.name)
                    is Action.SendSms -> p.confirmSms(action.contact.name, action.text)
                }
                pending = Pending.Confirm(action, question)
                say(question)
            }
            Risk.HIGH -> {
                pending = null
                askGuardian(action)
            }
        }
    }

    private fun askGuardian(action: Action) {
        val guardians = emergencyContacts()
        if (approvals == null || guardians.isEmpty() || awaitingGuardian != null) return say(p.needsGuardian)
        // The link reaches the guardian by SMS, so that permission comes first.
        device.prepare(Capability.SEND_SMS) { allowed ->
            if (!allowed) return@prepare say(p.couldNotAsk)
            val ticket = "approval-${++tickets}"
            awaitingGuardian = AwaitingGuardian(ticket, action)
            say(p.askingGuardian(speech.names(guardians.take(3).map { it.name })), Mood.WAITING)
            approvals.request(ticket, ApprovalKind.SEND_CODE, action.contact.name, guardians.take(3)) { url ->
                p.guardianSms(action.contact.name, url)
            }
        }
    }

    /** A guardian decided, the link expired or the request failed. Only an explicit yes lets the action go ahead. */
    fun onApprovalResult(ticket: String, result: ApprovalResult) {
        val waiting = awaitingGuardian?.takeIf { it.ticket == ticket } ?: return
        awaitingGuardian = null
        when (result) {
            ApprovalResult.APPROVED -> {
                say(p.guardianApproved, Mood.HAPPY)
                perform(waiting.action)
            }
            ApprovalResult.REJECTED -> say(p.guardianRejected)
            ApprovalResult.EXPIRED -> say(p.guardianSilent)
            ApprovalResult.FAILED -> say(p.couldNotAsk)
        }
    }

    /** Acts and checks the result: tries each saved number, then tells the person what to do instead. */
    /** Asks for the phone permission the first time it's needed; without it, says how to do it by hand. */
    private fun perform(action: Action) {
        val capability = when (action) {
            is Action.Call -> Capability.CALL
            is Action.SendSms -> Capability.SEND_SMS
        }
        device.prepare(capability) { allowed ->
            when {
                allowed -> carryOut(action)
                action is Action.Call ->
                    say(p.callWithoutPermission(action.contact.name, SafeSpeech.phone(action.contact.phones.first())))
                else -> say(p.smsWithoutPermission(action.contact.name))
            }
        }
    }

    private fun carryOut(action: Action) {
        val name = action.contact.name
        val phones = action.contact.phones
        when (action) {
            is Action.Call -> {
                say(p.calling(name), Mood.CALLING)
                if (phones.none { attempt { device.call(it) } }) {
                    say(p.callFailed(name, SafeSpeech.phone(phones.first())))
                }
            }
            is Action.SendSms -> {
                if (phones.any { attempt { device.sendSms(it, action.text) } }) {
                    say(p.smsSent(name), Mood.HAPPY)
                } else {
                    val question = p.smsFailedOfferCall(name)
                    pending = Pending.Confirm(Action.Call(action.contact), question)
                    say(question)
                }
            }
        }
    }

    private fun readMessages(onlyLast: Boolean) {
        if (unread.isEmpty()) return say(p.noMessages)
        val items = if (onlyLast) listOf(unread.removeLast()) else unread.toList().also { unread.clear() }
        val intro = if (items.size > 1) p.youHave(items.size) else ""
        say(intro + items.joinToString(" ") { describe(it) })
    }

    private fun describe(r: Received): String {
        val from = p.from(senderName(r.message.sender))
        return when (r.verdict.level) {
            Level.OK -> "$from ${speech.clean(r.message.text)}"
            Level.DOUBT -> "$from ${speech.clean(r.message.text)} ${p.careful} ${r.verdict.explanation}"
            Level.SCAM -> "$from ${p.notReading} ${r.verdict.explanation}"
        }
    }

    private fun senderName(sender: String): String {
        directory.byPhone(sender)?.let { return it.name }
        val folded = Text.fold(sender).trim()
        contacts.firstOrNull { c -> Text.fold(c.name) == folded || c.aliases.any { Text.fold(it) == folded } }?.let { return it.name }
        return when {
            '@' in sender -> p.unknownEmail
            sender.count { it.isDigit() } >= 3 -> p.unknownNumber
            else -> p.quote(speech.clean(sender))
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

    /** Peri speaks and her face follows: most sentences are calm, warnings and waits say so. */
    private fun say(text: String, mood: Mood = Mood.CALM) {
        this.mood = mood
        lastSaid = text
        device.speak(text)
    }

    private companion object {
        const val RISK_WINDOW_MILLIS = 30 * 60_000L
        const val MAX_UNREAD = 50

        /** Callers who claim to be a bank, an official body or a company: the answer is always to call back yourself. */
        val officialCaller = Text.anyOf(
            listOf(
                "banco", "bank", "policia", "police", "guardia civil", "hacienda", "agencia tributaria", "seguridad social", "hmrc", "irs",
                "soporte", "support", "tecnico", "technician", "microsoft", "amazon", "compania", "company", "empresa", "seguro",
                "insurance", "correos", "telefonica", "movistar", "luz", "electricity",
            ),
        )
    }
}
