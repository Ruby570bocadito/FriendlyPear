package app.friendlypear.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import app.friendlypear.BuildConfig
import app.friendlypear.R
import app.friendlypear.core.trust.FamilyRequestLink
import app.friendlypear.core.voice.ContactDirectory
import app.friendlypear.net.FamilyRequestClient
import app.friendlypear.ui.peri.GuardianActivity
import app.friendlypear.config.FamilyStore
import app.friendlypear.config.InvestigationEntry
import app.friendlypear.config.InvestigationLog
import app.friendlypear.config.NewsEntry
import app.friendlypear.config.SharedNews
import app.friendlypear.config.Need
import app.friendlypear.config.PermissionGate
import app.friendlypear.config.PhoneContacts
import app.friendlypear.core.agent.ApprovalKind
import app.friendlypear.core.agent.ApprovalPort
import app.friendlypear.core.agent.ApprovalResult
import app.friendlypear.core.agent.Mood
import app.friendlypear.core.agent.PauseAnswer
import app.friendlypear.core.agent.PeriAgent
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.TrustLevel
import app.friendlypear.core.trust.CircleRules
import app.friendlypear.core.trust.FamilyConfig
import app.friendlypear.core.trust.PendingCircleChange
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Level
import app.friendlypear.core.shield.Shield
import app.friendlypear.core.shield.Verdict
import app.friendlypear.device.AndroidDevice
import app.friendlypear.device.phoneLang
import app.friendlypear.device.speechLocale
import app.friendlypear.net.ApprovalClient
import app.friendlypear.net.GuardianClient
import app.friendlypear.net.OpinionClient
import app.friendlypear.net.PictureClient
import app.friendlypear.net.PictureVerdict
import app.friendlypear.net.TranscribeClient
import app.friendlypear.device.VoiceInput
import app.friendlypear.ui.peri.MiniPeriService
import app.friendlypear.ui.peri.PauseOverlay
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/** A screen showing Peri: the full app or the mini Peri floating over other apps. Called on the main thread. */
interface PeriView {
    fun said(text: String) = Unit
    fun mood(mood: Mood) = Unit
    /** While Peri's voice is playing, so her mouth can move. */
    fun speaking(on: Boolean) = Unit
    /** The full app came to the front or left it: Mini Peri steps aside meanwhile, so Peri never shows twice. */
    fun appShown(visible: Boolean) = Unit
    /** A short pose for a moment: an idea (a tip), "sorry" (didn't catch it), a thumbs up (well done). */
    fun moment(moment: PeriMoment) = Unit
}

enum class PeriMoment { IDEA, SORRY, THUMBS_UP }

/**
 * Owns the one [PeriAgent] of the app and feeds it on the main thread: messages from [MessageListener],
 * call state from [CallStateWatcher] and voice from the screen. Doubtful messages get Muse Spark Open's
 * second opinion through the backend when it is configured, and, if still doubtful, the Muse Spark
 * investigator's. High-risk actions wait for a guardian.
 */
class PeriHub(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /** The screens show what Peri says, large, for people who hear badly. */
    private val views = CopyOnWriteArraySet<PeriView>()
    var lastSaid: String = ""
        private set
    var speaking = false
        private set

    fun attach(view: PeriView) = views.add(view)

    /** Set by the app's screen while it's in front. */
    var appVisible = false
        set(value) {
            field = value
            views.forEach { it.appShown(value) }
        }
    fun detach(view: PeriView) = views.remove(view)

    /** The app's language (Settings, or the phone's); Peri speaks it and still understands both. */
    var lang = phoneLang()
        private set

    private val device: AndroidDevice = AndroidDevice(
        this.context,
        lang,
        onSpeech = { text ->
            lastSaid = text
            speaking = true
            views.forEach {
                it.said(text)
                it.mood(mood)
                it.speaking(true)
            }
        },
        onSpeechDone = {
            speaking = false
            views.forEach { it.speaking(false) }
        },
        onSpeechStart = {
            speaking = true
            views.forEach { it.speaking(true) }
        },
        ask = { capability, then -> permissions.ask(Need.of(capability), then) },
        pause = { text, callName -> pauseScreen.show(text, callName) },
        onCalling = { miniBesideCall() },
    )

    /** The safety pause over a money app; the person's choice goes back to the agent. */
    private val pauseScreen: PauseOverlay = PauseOverlay(this.context) { answer ->
        main.post {
            Log.i(TAG, "safety pause answer=$answer")
            agent.onPauseAnswer(answer)
            if (answer == PauseAnswer.LEAVE) views.forEach { it.moment(PeriMoment.THUMBS_UP) }
            share(NewsEntry(System.currentTimeMillis(), "pause", answer = answer.name.lowercase()))
        }
    }

    /** One permission at a time, with Peri's explanation; the screen registers itself to show Android's dialog. */
    /** Permission explanations are shown in Peri's bubble, not read aloud: at first start they'd be a lot of talking. */
    val permissions: PermissionGate = PermissionGate(this.context) { text -> show(text) }
    private var contacts: List<Contact>? = null
    private val family = FamilyStore(this.context)
    private var familyWord = false
    private val approvalClient = ApprovalClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    /** Waiting for a guardian can take minutes, so it never shares a thread with second opinions. */
    private val waiting = Executors.newCachedThreadPool()

    /** Family MFA: create the one-time link, text it to the guardians, then wait for their answer. */
    private val approvals: ApprovalPort = object : ApprovalPort {
        override fun request(ticket: String, kind: ApprovalKind, toName: String, guardians: List<Contact>, onCall: Boolean, smsFor: (url: String) -> String) {
            val asking = agent
            waiting.execute {
                val created = approvalClient.create(kind, toName, lang, onCall)
                if (created == null) {
                    main.post { asking.onApprovalResult(ticket, ApprovalResult.FAILED) }
                    return@execute
                }
                val text = smsFor(created.url)
                val sent = guardians.count { g -> g.phones.firstOrNull()?.let { runCatching { device.sendSms(it, text) }.isSuccess } == true }
                Log.i(TAG, "approval requested, texted $sent of ${guardians.size} guardian(s)")
                val result = if (sent == 0) ApprovalResult.FAILED else approvalClient.await(created.id)
                main.post {
                    Log.i(TAG, "approval result=$result")
                    asking.onApprovalResult(ticket, result)
                }
            }
        }
    }

    private var agent: PeriAgent = newAgent(emptyList())
    private var callActive = false
    private val calls = CallStateWatcher(this.context) { active, answered ->
        main.post {
            Log.i(TAG, "call active=$active answered=$answered")
            callActive = active
            agent.onCallStateChanged(active, answered)
            views.forEach { it.mood(mood) }
            watchApps()
            if (agent.strangerOnLine) miniBesideCall() else if (!active) MiniPeriService.endCall(context)
        }
    }
    private val countdownEnd = Runnable { agent.onEmergencyTimeout() }
    private val apps = AppWatcher(this.context) { packageName -> onAppInstalled(packageName) }
    val bank = BankWatcher(this.context, keepGoing = { agent.watchesApps }) { packageName -> onAppOpened(packageName) }
    private val opinion = OpinionClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    private val network = Executors.newSingleThreadExecutor()
    private val investigations = InvestigationLog(this.context)
    private val guardianClient = GuardianClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    private val pictures = PictureClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    private val transcriber = TranscribeClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    private val settings = this.context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Whether voice orders go to Muse Voice Transcribe (the person's choice in Settings; off by default). */
    var museVoice: Boolean
        get() = settings.getBoolean(MUSE_VOICE, false)
        set(value) {
            settings.edit().putBoolean(MUSE_VOICE, value).apply()
        }

    /** Can Muse Voice Transcribe be offered at all: a backend is configured. */
    val museVoiceAvailable: Boolean get() = transcriber.enabled

    /** Peri's ears for every screen that listens: Muse when it's on, Android's recognizer otherwise. */
    val ears: VoiceInput.Ears = object : VoiceInput.Ears {
        override val on: Boolean get() = museVoice && transcriber.enabled

        override fun transcribe(wav: ByteArray, done: (String?) -> Unit) {
            waiting.execute {
                val text = transcriber.transcribe(wav)
                // Never the words: only whether it worked.
                main.post {
                    Log.i(TAG, "voice transcribed=${text != null}")
                    done(text)
                }
            }
        }
    }

    /** Peri's voices for the Settings chooser, and the one in use. */
    fun voices() = device.voices()
    val voiceName: String? get() = device.voiceName
    val googleVoice: Boolean get() = device.googleEngine

    /** The person picked a voice: Peri uses it from now on and says a sample sentence with it. */
    fun chooseVoice(name: String) = main.post {
        device.chooseVoice(name)
        device.speak(context.getString(R.string.voice_sample))
    }

    /** Nothing understood: Peri says so kindly, and how to try again. */
    fun missedVoice() {
        say(context.getString(R.string.voice_missed))
        main.post { views.forEach { it.moment(PeriMoment.SORRY) } }
    }

    /** A pose on every screen showing Peri. */
    fun moment(moment: PeriMoment) = main.post { views.forEach { it.moment(moment) } }

    /** Peri's news for the guardians, only if the person turned it on; the same list stays on this phone. */
    val news = SharedNews(this.context)

    val isWaitingForAnswer: Boolean get() = agent.isWaitingForAnswer

    /** A phone call is going on: the microphone stays closed so Peri never hears it. */
    val inCall: Boolean get() = callActive

    /** Peri's face for the screens; it changes whenever she speaks. */
    val mood: Mood get() = agent.mood

    init {
        apps.start()
        if (BuildConfig.DEBUG) registerDebugVoice()
    }

    /** From [BankWatcher], only during a picked-up call. */
    fun onAppOpened(packageName: String) = main.post {
        // Debug builds only, and never which app: enough to check the watcher runs during calls.
        if (BuildConfig.DEBUG) Log.d(TAG, "app came to the front during a call")
        agent.onAppOpened(packageName)
    }

    /** From [AppWatcher]. Only the package name is looked at, and only remote-control apps make Peri speak. */
    fun onAppInstalled(packageName: String) = main.post {
        if (contacts == null) refreshNow()
        agent.onAppInstalled(packageName)
    }

    /** Reloads contacts and listeners after permissions change; the agent keeps its state if nothing changed. */
    fun refresh() = main.post {
        calls.start()
        family.loadPending()?.takeIf { !it.approved }?.let { awaitCircle(it) }
        val config = currentConfig()
        val fresh = config.applyTo(PhoneContacts.load(context))
        if (fresh != contacts || config.hasFamilyWord != familyWord) {
            contacts = fresh
            familyWord = config.hasFamilyWord
            agent = newAgent(fresh).also { it.onCallStateChanged(callActive) }
        }
    }

    fun onMessage(message: IncomingMessage) = main.post {
        if (contacts == null) refreshNow()
        // Peri to Peri: a family member's request, from a saved contact and to Peri's own server, opens in the app.
        FamilyRequestLink.find(message.text, BuildConfig.BACKEND_URL)?.let { found ->
            val from = familyContact(message.sender)
            if (from != null) return@post familyRequest(found, from)
        }
        // Talks like Peri but links elsewhere: never a real request, so no model is asked either.
        val fake = FamilyRequestLink.imitatesPeri(message.text, BuildConfig.BACKEND_URL)
        val asking = opinion.enabled && !fake
        val verdict = agent.onMessage(message, secondOpinion = asking, imitatesPeri = fake)
        // Waiting for the models, Peri looks through her magnifying glass before she says anything.
        views.forEach { it.mood(mood) }
        // Never the text or the sender: only what Peri decided.
        Log.i(TAG, "message channel=${message.channel} level=${verdict.level} hardRule=${verdict.hardRule}")
        if (asking && verdict.level == Level.DOUBT) askModel(message, verdict) else shareVerdict(message, verdict.level)
        watchApps()
    }

    /** The SMS and its notification both arrive: each request is handled once. */
    private val familyRequests = LinkedHashSet<String>()

    /** The saved contact who sent [sender]: by number (SMS), or by the name a notification shows. */
    private fun familyContact(sender: String): Contact? {
        val saved = contacts.orEmpty()
        return ContactDirectory(saved).byPhone(sender) ?: saved.firstOrNull { it.name.equals(sender.trim(), ignoreCase = true) }
    }

    /**
     * What the request says is read from the server (the link is the key), never from the SMS. Peri says it aloud
     * and opens the guardian's screen, or a notification if the app isn't in front.
     */
    private fun familyRequest(found: FamilyRequestLink.Found, from: Contact) {
        if (!familyRequests.add(found.token)) return
        if (familyRequests.size > MAX_FAMILY_REQUESTS) familyRequests.remove(familyRequests.first())
        Log.i(TAG, "family request from a saved contact")
        network.execute {
            val request = FamilyRequestClient.fetch(found.url)
            main.post {
                if (request == null || !request.pending) {
                    say(context.getString(R.string.guardian_gone))
                    return@post
                }
                val screen = GuardianActivity.intent(context, found.url, from.name, from.phones.firstOrNull(), request)
                val line = if (request.onCall) R.string.guardian_says_urgent else R.string.guardian_says
                say(context.getString(line, from.name, request.toName))
                if (appVisible) context.startActivity(screen) else notifyFamilyRequest(screen, from.name, found.token)
            }
        }
    }

    private fun notifyFamilyRequest(screen: Intent, from: String, token: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(FAMILY_CHANNEL, context.getString(R.string.guardian_channel), NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(context, token.hashCode(), screen, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(context, FAMILY_CHANNEL)
            .setSmallIcon(R.drawable.ic_pear)
            .setColor(app.friendlypear.ui.peri.Palette.GREEN_DARK)
            .setContentTitle(context.getString(R.string.guardian_title, from))
            .setContentText(context.getString(R.string.guardian_notify_text))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(token.hashCode(), notification) }
    }

    /** Off the main thread; the agent hears back on it, with the vote or with null if anything failed. */
    private fun askModel(message: IncomingMessage, verdict: Verdict) {
        val waitingAgent = agent
        val call = callActive
        val relations = contacts.orEmpty().mapNotNull { it.relation }.toSet()
        network.execute {
            val first = opinion.ask(message, verdict, call, lang)
            // Still doubtful after the quick opinion: the investigator looks things up before Peri speaks.
            val stillDoubt = first == null || Shield().withModelOpinion(verdict, first).level == Level.DOUBT
            val found = if (stillDoubt) opinion.investigate(message, verdict, call, lang, relations) else null
            if (found != null) investigations.add(InvestigationEntry(System.currentTimeMillis(), found.verdict, found.evidence))
            // Both votes together stay within the model's ±2: models only tip the balance, rules decide.
            val delta = if (found == null) first else ((first ?: 0.0) + found.delta).coerceIn(-Shield.MAX_MODEL_DELTA, Shield.MAX_MODEL_DELTA)
            main.post {
                val after = waitingAgent.onModelOpinion(message, delta)
                views.forEach { it.mood(mood) }
                watchApps()
                after?.let { shareVerdict(message, it.level) }
                Log.i(TAG, "second opinion delta=${first ?: "none"} investigator=${found?.delta ?: "none"} level=${after?.level}")
            }
        }
    }

    /**
     * The person turned sharing on or off in the family setup. On: each guardian gets their own link by SMS.
     * Off: every link stops working and the list is forgotten. [done] learns whether sharing is on afterwards.
     */
    fun shareNews(on: Boolean, done: (Boolean) -> Unit) = main.post {
        if (!on) {
            news.sharing = false
            news.clear()
            waiting.execute { guardianClient.revokeAll() }
            device.speak(context.getString(R.string.share_off))
            return@post done(false)
        }
        val guardians = (contacts ?: emptyList()).filter { it.trust == TrustLevel.GUARDIAN && it.phones.isNotEmpty() }
        if (!guardianClient.enabled || guardians.isEmpty()) {
            device.speak(context.getString(R.string.share_no_guardians))
            return@post done(false)
        }
        permissions.ask(Need.SEND_SMS) { allowed ->
            if (!allowed) return@ask done(false)
            waiting.execute {
                val sent = guardians.count { g ->
                    val url = guardianClient.createLink(g.name.trim().split(" ").first(), lang) ?: return@count false
                    runCatching { device.sendSms(g.phones.first(), context.getString(R.string.share_sms, url)) }.isSuccess
                }
                main.post {
                    Log.i(TAG, "news shared with $sent of ${guardians.size} guardian(s)")
                    news.sharing = sent > 0
                    device.speak(context.getString(if (sent > 0) R.string.share_sent else R.string.share_failed))
                    done(sent > 0)
                }
            }
        }
    }

    /** Only what Peri decided, never the message: a scam or a doubt, and the channel it came by. */
    private fun shareVerdict(message: IncomingMessage, level: Level) {
        val name = when (level) {
            Level.SCAM -> "scam"
            Level.DOUBT -> "doubt"
            else -> return
        }
        share(NewsEntry(System.currentTimeMillis(), "warning", level = name, channel = message.channel.name))
    }

    /** Kept on the phone only once the backend has it, so the person's list is exactly what the guardians see. */
    private fun share(entry: NewsEntry) {
        if (!news.sharing) return
        waiting.execute {
            val ok = if (entry.kind == "warning") guardianClient.warning(entry.level, entry.channel) else guardianClient.pause(entry.answer)
            if (ok) main.post { news.add(entry) }
        }
    }

    /**
     * During a call Peri placed, or a picked-up call from a stranger, Peri stays small beside the call screen
     * instead of the full app, and goes away when the call ends (unless the person had Mini Peri on already).
     */
    private fun miniBesideCall() {
        if (Settings.canDrawOverlays(context)) MiniPeriService.startForCall(context)
    }

    /** Only while there's a reason: a stranger's call, or a while after something worrying. */
    private fun watchApps() {
        if (agent.watchesApps) bank.start()
    }

    /** From [CallScreener], before the call is picked up. The number itself is never logged. */
    fun onIncomingCall(number: String) = main.post {
        if (contacts == null) refreshNow()
        agent.onIncomingCall(number)
        Log.i(TAG, "incoming call screened")
    }

    /**
     * The app's language changed (Settings, or the phone's when following it): Peri's voice and her sentences
     * switch at once, keeping what she knows about the family, and she says so in the new language.
     */
    fun languageChanged() = main.post {
        val now = phoneLang()
        if (now == lang) return@post
        lang = now
        device.lang = now
        agent = newAgent(contacts ?: emptyList()).also { it.onCallStateChanged(callActive) }
        val local = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(speechLocale(now)) })
        device.speak(local.getString(R.string.language_changed))
    }

    /**
     * "Look at this picture": Muse Spark and SAM 3.1 through the backend. A scam or a doubt makes the next money
     * app pause, like a scam message would. [done] gets null if anything failed; the picture is never kept.
     */
    fun lookAtPicture(jpegBase64: String, done: (PictureVerdict?) -> Unit) {
        waiting.execute {
            val verdict = pictures.look(jpegBase64, lang)
            main.post {
                Log.i(TAG, "picture verdict=${verdict?.verdict ?: "none"} marks=${verdict?.marks?.size ?: 0}")
                if (verdict != null && verdict.verdict != "ok") {
                    agent.onOutsideWarning()
                    watchApps()
                }
                done(verdict)
            }
        }
    }

    /** Peri says something outside the agent, e.g. why she needs a permission. */
    fun say(text: String) = main.post { device.speak(text) }

    /** Something Peri writes in her bubble without saying it aloud: explanations to read at one's own pace. */
    fun show(text: String) = main.post {
        lastSaid = text
        views.forEach { it.said(text) }
    }

    fun heard(utterance: String) = main.post {
        agent.onHeard(utterance)
        main.removeCallbacks(countdownEnd)
        if (agent.isCountingDown) main.postDelayed(countdownEnd, COUNTDOWN_MILLIS)
    }

    /** The family setup in force: an approved change of guardians applies once its 48 hours are up. */
    private fun currentConfig(): FamilyConfig {
        val config = family.load()
        val pending = family.loadPending()
        val now = System.currentTimeMillis()
        if (!CircleRules.isDue(pending, now)) return config
        return CircleRules.effective(config, pending, now).also {
            family.save(it)
            family.savePending(null)
        }
    }

    /**
     * Asks the current guardians to approve a change of guardians ([change] says it in plain words, e.g.
     * "add Marisa"). It waits as pending; with a yes it applies 48 hours after the request, otherwise never.
     */
    fun requestCircleChange(proposed: FamilyConfig, change: String) = main.post {
        val pending = CircleRules.propose(proposed, System.currentTimeMillis())
        val guardians = (contacts ?: emptyList()).filter { it.trust == TrustLevel.GUARDIAN && it.phones.isNotEmpty() }
        if (!approvalClient.enabled || guardians.isEmpty()) return@post device.speak(context.getString(R.string.circle_cannot_ask))
        family.savePending(pending)
        device.speak(context.getString(R.string.circle_explain))
        waiting.execute {
            val created = approvalClient.create(ApprovalKind.CIRCLE_CHANGE, change, lang)
            val text = created?.let { context.getString(R.string.circle_sms, change, it.url) }
            val sent = if (text == null) 0 else guardians.count { g -> runCatching { device.sendSms(g.phones.first(), text) }.isSuccess }
            main.post {
                if (created == null || sent == 0) return@post settleCircle(pending, ApprovalResult.FAILED)
                // Kept with the change, so waiting resumes if the app is restarted before the family answers.
                val asked = pending.copy(approvalId = created.id)
                if (family.loadPending()?.requestedAt == pending.requestedAt) family.savePending(asked)
                awaitCircle(asked)
            }
        }
    }

    private var awaitingCircle: String? = null

    private fun awaitCircle(pending: PendingCircleChange) {
        val id = pending.approvalId ?: return
        if (awaitingCircle == id) return
        awaitingCircle = id
        waiting.execute {
            val result = approvalClient.await(id)
            main.post {
                awaitingCircle = null
                settleCircle(pending, result)
            }
        }
    }

    private fun settleCircle(pending: PendingCircleChange, result: ApprovalResult) {
        Log.i(TAG, "circle change result=$result")
        // Only the request still on file counts: the person may have cancelled it meanwhile.
        if (family.loadPending()?.requestedAt != pending.requestedAt) return
        if (result == ApprovalResult.APPROVED) {
            family.savePending(pending.copy(approved = true))
            device.speak(context.getString(R.string.circle_approved))
        } else {
            family.savePending(null)
            device.speak(context.getString(if (result == ApprovalResult.REJECTED) R.string.circle_rejected else R.string.circle_no_answer))
        }
    }

    private fun newAgent(contacts: List<Contact>): PeriAgent =
        PeriAgent(device, contacts, lang, hasFamilyWord = familyWord, approvals = if (approvalClient.enabled) approvals else null)

    private fun refreshNow() {
        calls.start()
        val config = currentConfig()
        familyWord = config.hasFamilyWord
        contacts = config.applyTo(PhoneContacts.load(context)).also { agent = newAgent(it).apply { onCallStateChanged(callActive) } }
    }

    /**
     * Debug builds only: lets the emulator "speak" with
     * `adb shell am broadcast -a app.friendlypear.debug.HEAR --es text "si"`, since it has no microphone.
     */
    private fun registerDebugVoice() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                intent.getStringExtra("text")?.let { heard(it) }
                intent.getStringExtra("installed")?.let { onAppInstalled(it) }
                intent.getStringExtra("opened")?.let { onAppOpened(it) }
                // A WAV in the app's own files stands in for the microphone, through Muse Voice Transcribe.
                intent.getStringExtra("wav")?.let { name ->
                    val file = java.io.File(context.filesDir, name)
                    if (file.isFile) ears.transcribe(file.readBytes()) { text -> if (text != null) heard(text) else missedVoice() }
                }
            }
        }
        // Only adb's shell holds DUMP: no app on the phone can make Peri "hear" a "yes".
        context.registerReceiver(receiver, IntentFilter(DEBUG_HEAR), Manifest.permission.DUMP, null, Context.RECEIVER_EXPORTED)
    }

    private companion object {
        const val TAG = "FriendlyPear"
        const val FAMILY_CHANNEL = "family_requests"
        const val MAX_FAMILY_REQUESTS = 50
        const val COUNTDOWN_MILLIS = 10_000L
        const val DEBUG_HEAR = "app.friendlypear.debug.HEAR"
        const val MUSE_VOICE = "muse_voice"
    }
}
