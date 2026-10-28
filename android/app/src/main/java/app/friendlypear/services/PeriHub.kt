package app.friendlypear.services

import android.os.SystemClock
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
import app.friendlypear.core.voice.Command
import app.friendlypear.core.voice.ContactDirectory
import app.friendlypear.core.voice.VoiceCommands
import app.friendlypear.net.FamilyRequestClient
import app.friendlypear.ui.peri.GuardianActivity
import app.friendlypear.config.FamilyStore
import app.friendlypear.config.InvestigationEntry
import app.friendlypear.config.InvestigationLog
import app.friendlypear.config.LastScam
import app.friendlypear.config.Shields
import app.friendlypear.net.ReportClient
import app.friendlypear.net.ScamReport
import app.friendlypear.config.UseLog
import app.friendlypear.config.WebNotice
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
import app.friendlypear.core.model.Channel
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
    /** A call has just started (or Peri placed one): any microphone still open closes at once. */
    fun callStarted() = Unit
    /** The person just earned a shield (a scam stopped, or they asked first); [total] is how many they have. */
    fun shieldEarned(total: Int) = Unit
    /** A scam was just stopped: the screen can offer to report it. */
    fun scamToReport() = Unit
}

/**
 * A pose Peri strikes for a couple of seconds: an idea, sorry, a thumbs up, a laugh, her heart, dizzy, a yawn, a
 * letter; and since round 2 her cape, a medal, peekaboo, a clap, her detective hat, a surprise, a party, her reading
 * glasses, her winter scarf and a goodbye.
 */
enum class PeriMoment { IDEA, SORRY, THUMBS_UP, LAUGH, LOVE, DIZZY, YAWN, LETTER, HERO, MEDAL, PEEKABOO, CLAP, DETECTIVE, SURPRISED, PARTY, GLASSES, SCARF, BYE }

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

    /** What Peri did with the phone (calls, texts, location, pictures, voice), for the person to check. */
    val uses = UseLog(this.context)

    /** A touch for each warning, so it's felt even with the sound low or the phone in a pocket. */
    val haptics = app.friendlypear.device.Haptics(this.context)

    /** One for each scam stopped and each time the person asks before acting. */
    val shields = Shields(this.context)

    /** What Peri did, for the "Diary" section: a word or two per entry, never a message's text or sender. */
    val diary = app.friendlypear.config.Diary(this.context)

    /** A website's junk notice, to silence that site: [onWebNotice] keeps it until the verdict says it's junk. */
    val webNotices = WebNotice(this.context)
    private val pendingWeb = HashMap<String, WebNotice.Notice>()

    /** A browser passed on a website's notice: checked like any message, and remembered in case it's junk. */
    fun onWebNotice(message: IncomingMessage, notice: WebNotice.Notice) = main.post {
        pendingWeb[message.text] = notice
        if (pendingWeb.size > 20) pendingWeb.remove(pendingWeb.keys.first())
        onMessage(message)
    }

    /** The last scam Peri stopped, for "Report it"; it leaves the phone only if the person asks. */
    val lastScam = LastScam(this.context)
    private val reports = ReportClient(BuildConfig.BACKEND_URL, { app.friendlypear.net.DeviceKey.current(this.context) })

    /** Muse Spark writes the last scam up for the police or a helpline; [done] gets null if anything failed. */
    fun writeReport(done: (ScamReport?) -> Unit) {
        val scam = lastScam.load() ?: return done(null)
        uses.add(UseLog.Kind.REPORT)
        waiting.execute {
            val report = reports.write(scam, lang)
            main.post {
                Log.i(TAG, "report written=${report != null}")
                done(report)
            }
        }
    }

    fun earn(kind: Shields.Kind) = main.post {
        shields.add(kind)
        Log.i(TAG, "shield earned kind=$kind total=${shields.total}")
        views.forEach { it.shieldEarned(shields.total) }
    }

    private val device: AndroidDevice = AndroidDevice(
        this.context,
        lang,
        onSpeech = { text ->
            lastSaid = text
            speaking = true
            haptics.mood(mood)
            views.forEach {
                it.said(text)
                it.mood(mood)
                it.speaking(true)
            }
            // The family's pear of this phone shows attention while Peri warns or waits for the family.
            if (pearState() != sharedState) publishPear()
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
        pause = { text, callName, hangUp, keep -> pauseScreen.show(text, callName, hangUp, keep) },
        onCalling = { miniBesideCall() },
        onUse = { kind, phone ->
            uses.add(kind, phone)
            if (kind == UseLog.Kind.HANG_UP) main.post { earn(Shields.Kind.STOPPED) }
        },
    )

    /** The safety pause over a money app; the person's choice goes back to the agent. */
    private val pauseScreen: PauseOverlay = PauseOverlay(this.context) { answer ->
        main.post {
            Log.i(TAG, "safety pause answer=$answer")
            agent.onPauseAnswer(answer)
            if (answer == PauseAnswer.LEAVE) views.forEach { it.moment(PeriMoment.THUMBS_UP) }
            when (answer) {
                PauseAnswer.LEAVE -> {
                    earn(Shields.Kind.STOPPED)
                    diary.add(app.friendlypear.config.Diary.Kind.PAUSE_LEFT)
                }
                PauseAnswer.CALL_FAMILY -> {
                    earn(Shields.Kind.FAMILY)
                    diary.add(app.friendlypear.config.Diary.Kind.FAMILY_CALLED)
                }
                PauseAnswer.HANG_UP, PauseAnswer.KEEP_CALL, PauseAnswer.CONTINUE -> Unit
            }
            // Keeping a call isn't a safety pause the family follows: only the pause's own answers are shared.
            if (answer != PauseAnswer.KEEP_CALL) share(NewsEntry(System.currentTimeMillis(), "pause", answer = answer.name.lowercase()))
        }
    }

    /** One permission at a time, with Peri's explanation; the screen registers itself to show Android's dialog. */
    /** Permission explanations are shown in Peri's bubble, not read aloud: at first start they'd be a lot of talking. */
    val permissions: PermissionGate = PermissionGate(this.context) { text -> show(text) }
    private var contacts: List<Contact>? = null
    private val family = FamilyStore(this.context)
    private var familyWord = false
    private val approvalClient = ApprovalClient(BuildConfig.BACKEND_URL, { app.friendlypear.net.DeviceKey.current(this.context) })
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
                earn(Shields.Kind.FAMILY)
                main.post { diary.add(app.friendlypear.config.Diary.Kind.FAMILY_ASKED) }
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
            if (!active) pauseScreen.callEnded()
            if (active) views.forEach { it.callStarted() }
            agent.onCallStateChanged(active, answered)
            views.forEach { it.mood(mood) }
            watchApps()
            if (agent.strangerOnLine) miniBesideCall() else if (!active) MiniPeriService.endCall(context)
            // The family sees a stranger on the line (if the person shares Peri's news), and can ask Peri to hang up.
            if (pearState() != sharedState || pearKind() != sharedKind) publishPear()
            main.removeCallbacks(hangUpWatch)
            if (agent.strangerOnLine) main.postDelayed(hangUpWatch, HANGUP_POLL_MS)
        }
    }
    private val countdownEnd = Runnable { agent.onEmergencyTimeout() }
    private val apps = AppWatcher(this.context) { packageName -> onAppInstalled(packageName) }
    val bank = BankWatcher(this.context, keepGoing = { agent.watchesApps }) { packageName -> onAppOpened(packageName) }
    private val opinion = OpinionClient(BuildConfig.BACKEND_URL, { app.friendlypear.net.DeviceKey.current(this.context) })
    private val network = Executors.newSingleThreadExecutor()
    private val investigations = InvestigationLog(this.context)
    private val guardianClient = GuardianClient(BuildConfig.BACKEND_URL, { app.friendlypear.net.DeviceKey.current(this.context) })
    private val pictures = PictureClient(BuildConfig.BACKEND_URL, { app.friendlypear.net.DeviceKey.current(this.context) })
    private val transcriber = TranscribeClient(BuildConfig.BACKEND_URL, { app.friendlypear.net.DeviceKey.current(this.context) })
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
            uses.add(UseLog.Kind.VOICE)
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

    /** The family's pears: this phone's goes to the family's server, the others' come back for the family section. */
    private val pearClient = app.friendlypear.net.PearClient(BuildConfig.BACKEND_URL) {
        if (app.friendlypear.net.DeviceKey.paired(this.context)) app.friendlypear.net.DeviceKey.current(this.context) else null
    }
    private val pearPrefs = this.context.getSharedPreferences("pear", Context.MODE_PRIVATE)
    private var sharedState = ""

    /** Only a phone paired with the family's server (its own key) has a pear there. */
    val seesFamilyPears: Boolean get() = pearClient.enabled

    /** On a call with a number that isn't in the contacts, right now: a guardian's yes waits until it's over. */
    val strangerOnLine: Boolean get() = agent.strangerOnLine

    /** What the family calls the person ("Mamá", "Ana"): the name on this phone's pear. Empty until they choose. */
    var pearName: String
        get() = pearPrefs.getString(PEAR_NAME, "").orEmpty()
        set(value) {
            // Letters, numbers and a few signs, as the family's server accepts them.
            val clean = value.filter { it.isLetterOrDigit() || it in " .'()-" }.trim().take(24)
            pearPrefs.edit().putString(PEAR_NAME, clean).apply()
            publishPear()
        }

    /** Attention while Peri warns or waits for the family, but only if the person shares Peri's news with them. */
    private fun pearState(): String =
        if (news.sharing && (mood in ATTENTION_MOODS || agent.strangerOnLine || screenIntruder != null || helpWanted())) "attention" else "calm"

    /** Why their pear needs attention, in a word: on a stranger's call right now, or the kind of a recent scam. */
    private fun pearKind(): String {
        if (pearState() != "attention") return ""
        if (agent.strangerOnLine) return "call"
        if (screenIntruder != null) return "remote"
        if (helpWanted()) return "breach"
        return lastScam.load()?.takeIf { System.currentTimeMillis() - it.at < SCAM_KIND_MS }?.kind.orEmpty()
    }

    private var sharedKind = ""
    private var lastPayload = ""
    private var lastPublished = 0L

    /** While a stranger is on the line, a guardian may ask this Peri to hang up: she looks every few seconds. */
    private val hangUpWatch = object : Runnable {
        override fun run() {
            if (!agent.strangerOnLine || !pearClient.enabled) return
            network.execute {
                val from = pearClient.hangUpRequest()
                main.post {
                    if (from != null) {
                        Log.i(TAG, "a guardian asked to hang up")
                        val name = from.ifEmpty { context.getString(R.string.check_someone) }
                        if (agent.onFamilyHangUp(name)) main.postDelayed({
                            // Hung up with the app out of sight: a notification says who asked, so it's never a mystery.
                            if (agent.onFamilyHangUpDue() && !appVisible) {
                                familyNotes.notify(FamilyNotes.HUNG_UP_ID, context.getString(R.string.family_hung_up_title, name), context.getString(R.string.family_hung_up_text), app.friendlypear.ui.peri.Palette.CORAL_INK)
                            }
                        }, HANGUP_WAIT_MS)
                    }
                }
            }
            main.postDelayed(this, HANGUP_POLL_MS)
        }
    }

    /** Asks a family member's Peri to hang up their stranger's call; [done] learns if the request got there. */
    fun requestHangUp(pearId: String, done: (Boolean) -> Unit) {
        network.execute {
            val sent = pearClient.hangUp(pearId)
            main.post { done(sent) }
        }
    }

    /** The family's news as notifications while the app is closed ([FamilyNotes]): a look every minute. */
    val familyNotes = FamilyNotes(this.context, { pearClient }) { name -> phoneOf(name) }
    /** An app that can control the screen right now (its name), if any. */
    @Volatile private var screenIntruder: String? = null

    /**
     * Looks for apps that can see and use the screen ([app.friendlypear.device.ScreenAccess]): a new one is said once,
     * with a notification that opens the accessibility settings; while one is on, their pear shows it to the family.
     */
    fun checkScreenAccess() = main.post { screenAccess(app.friendlypear.device.ScreenAccess.intruders(context)) }

    private fun screenAccess(found: List<Pair<String, String>>) {
        val first = found.firstOrNull()
        val was = screenIntruder
        screenIntruder = first?.second
        if (screenIntruder != was) publishPear()
        val seen = intruders.getStringSet(WARNED, emptySet()).orEmpty()
        val fresh = found.filter { it.first !in seen }
        intruders.edit().putStringSet(WARNED, found.map { it.first }.toSet()).apply()
        val (pkg, name) = fresh.firstOrNull() ?: return
        Log.i(TAG, "an app can control the screen")
        diary.add(app.friendlypear.config.Diary.Kind.SCREEN_INTRUDER, pkg.takeLast(20))
        say(context.getString(R.string.intruder_says, name))
        notifyIntruder(name)
    }

    private val intruders = this.context.getSharedPreferences("screen_access", Context.MODE_PRIVATE)

    private fun notifyIntruder(name: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(ALERTS_CHANNEL, context.getString(R.string.alerts_channel), NotificationManager.IMPORTANCE_HIGH))
        val settings = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = PendingIntent.getActivity(context, 4_204, settings, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = context.getString(R.string.intruder_text, name)
        val notification = Notification.Builder(context, ALERTS_CHANNEL)
            .setSmallIcon(R.drawable.ic_pear)
            .setColor(app.friendlypear.ui.peri.Palette.CORAL_INK)
            .setContentTitle(context.getString(R.string.intruder_title))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(4_204, notification) }
    }

    private val familyLoop = object : Runnable {
        override fun run() {
            checkScreenAccess()
            if (SystemClock.elapsedRealtime() - lastBlocksSync > BLOCKS_SYNC_MS) syncBlocks()
            if (!appVisible && seesFamilyPears) network.execute { runCatching { familyNotes.look() } }
            main.postDelayed(this, FAMILY_NOTES_MS)
        }
    }

    /** A family member's number from the saved contacts, by the name their pear goes by ("Mamá" or "Mama"). */
    private fun phoneOf(name: String): String? {
        val key = plain(name)
        if (key.isEmpty()) return null
        return contacts.orEmpty().firstOrNull { plain(it.name) == key }?.phones?.firstOrNull()
    }

    private fun plain(name: String): String =
        java.text.Normalizer.normalize(name.lowercase().trim(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    /**
     * After giving something away, the person chose to tell the family: for a day their pear asks for a hand
     * ("breach"), if they share Peri's news. Only that: never what was given.
     */
    fun needsFamilyHelp() {
        aftercare.edit().putLong(HELP_UNTIL, System.currentTimeMillis() + HELP_MS).apply()
        publishPear()
    }

    private val aftercare = this.context.getSharedPreferences("aftercare_help", Context.MODE_PRIVATE)

    private fun helpWanted() = aftercare.getLong(HELP_UNTIL, 0L) > System.currentTimeMillis()

    /** Whether the family's phones see this pear (and so a call for a hand): it has a name and Peri shares its news. */
    val familySeesPear: Boolean get() = news.sharing && pearClient.enabled && pearName.isNotEmpty()

    /** The first guardian's number, to call them from the steps. */
    fun guardianPhone(): String? {
        val guardians = family.load().guardians
        return contacts.orEmpty().firstOrNull { it.id in guardians }?.phones?.firstOrNull()
    }

    /** Numbers the person blocked from a scam's card. */
    val blocked = app.friendlypear.config.BlockList(this.context)

    /** Numbers the rest of the family blocked, as fingerprints. */
    private val familyBlocks = app.friendlypear.config.FamilyBlocks(this.context)

    /** Who blocked a sender whose calls are turned away and texts silenced here. */
    enum class Blocker { ME, FAMILY }

    /**
     * Blocked here, or by someone else in the family; a family block never applies to a saved contact, so nobody can
     * cut this phone off from its own people, not even with a stolen family key. On the main thread.
     */
    fun blockedHere(sender: String): Blocker? {
        if (blocked.has(sender)) return Blocker.ME
        if (!familyBlocks.has(sender)) return null
        if (contacts == null) refreshNow()
        return if (familyContact(sender) == null) Blocker.FAMILY else null
    }

    /** From the scam's card: blocked on this phone, and for the rest of the family. */
    fun block(sender: String) {
        blocked.add(sender)
        syncBlocks()
    }

    fun unblock(sender: String) {
        blocked.remove(sender)
        syncBlocks()
    }

    /** Whether a number blocked here protects the rest of the family too (this phone is paired with its server). */
    val blocksReachFamily: Boolean get() = pearClient.enabled

    /**
     * The family's blocked numbers, both ways: this phone's as fingerprints under the family's salt (never a number),
     * and the other phones' kept here, sealed. After a block or unblock, at start and every few minutes.
     */
    fun syncBlocks() {
        if (!pearClient.enabled) return
        lastBlocksSync = SystemClock.elapsedRealtime()
        network.execute {
            val family = pearClient.blocks() ?: return@execute
            val mine = blocked.all().mapNotNull { app.friendlypear.config.FamilyBlocks.print(family.salt, it) }.distinct()
            val shared = pearClient.shareBlocks(mine)
            familyBlocks.save(family.salt, family.prints)
            Log.i(TAG, "blocks shared=$shared mine=${mine.size} family=${family.prints.size}")
        }
    }

    /** The first look goes at once, however long the phone has been on. */
    private var lastBlocksSync = -BLOCKS_SYNC_MS

    /** From [CallScreener]: a blocked number's call was turned away before it rang. */
    fun onBlockedCall(by: Blocker) = main.post {
        diary.add(app.friendlypear.config.Diary.Kind.BLOCKED_CALL, if (by == Blocker.FAMILY) app.friendlypear.config.Diary.BY_FAMILY else "")
        Log.i(TAG, "blocked call turned away")
    }

    /**
     * Sends this phone's pear: its name, what Peri wears, the role (looked after, if there are guardians here), and
     * calm or attention. Nothing about messages, calls or places.
     */
    fun publishPear() = main.post {
        val name = pearName
        if (name.isEmpty() || !pearClient.enabled) return@post
        val state = pearState()
        sharedState = state
        val outfit = app.friendlypear.ui.peri.Outfit.load(context).code()
        val role = if (family.load().guardians.isNotEmpty()) "protected" else "guardian"
        // The kind of trap, if a scam is why their pear needs attention (the family's server spots one reaching many),
        // or "call" while a stranger is on the line.
        val kind = pearKind()
        sharedKind = kind
        // The same pear again within a few seconds (Peri says several sentences in a row) goes once.
        val payload = listOf(name, outfit, role, state, kind).joinToString("|")
        val now = SystemClock.elapsedRealtime()
        if (payload == lastPayload && now - lastPublished < REPUBLISH_MS) return@post
        lastPayload = payload
        lastPublished = now
        waiting.execute {
            val sent = pearClient.publish(name, outfit, role, state, kind)
            Log.i(TAG, "pear published=$sent state=$state")
        }
    }

    /** Everyone's pears, this phone's included, and any campaign; null if the server can't be reached. */
    fun familyPears(done: (app.friendlypear.net.PearClient.Family?) -> Unit) {
        waiting.execute {
            val family = pearClient.family()
            main.post { done(family) }
        }
    }

    /** The "was it you?" questions other family phones are asking this one. */
    fun openChecks(done: (List<app.friendlypear.net.PearClient.Check>?) -> Unit) {
        waiting.execute {
            val checks = pearClient.openChecks()
            main.post { done(checks) }
        }
    }

    fun answerCheck(id: String, me: Boolean, done: (Boolean) -> Unit) {
        waiting.execute {
            val sent = pearClient.answer(id, me)
            main.post { done(sent) }
        }
    }

    /**
     * "Was it you?": someone wrote from a new number saying they're family. Peri asks the family's other Peris in
     * silence and says what they answered: if one says it was them, still call their usual number first; if all say
     * no, it's a trap. Only the question travels, never the message or the number.
     */
    private fun askFamily() {
        // Like the pear's attention, only while the person shares Peri's news with the family.
        if (!pearClient.enabled || !news.sharing) return
        waiting.execute {
            val id = pearClient.ask("new_number") ?: return@execute
            main.post {
                diary.add(app.friendlypear.config.Diary.Kind.FAMILY_CHECKED)
                val question = FamilyQuestion(id, android.os.SystemClock.uptimeMillis())
                main.postDelayed(object : Runnable {
                    override fun run() = hearFamily(question, this)
                }, CHECK_POLL_MS)
            }
        }
    }

    /** A "was it you?" being waited on: when it was asked, and when the first "it wasn't me" came. */
    private class FamilyQuestion(val id: String, val started: Long, var firstNo: Long = 0L)

    /**
     * Someone said it was them: still call their usual number. Everyone said no, or someone said no a minute ago
     * and nobody said yes since: it's a trap. Nobody answered in ten minutes: call their usual number.
     */
    private fun hearFamily(question: FamilyQuestion, again: Runnable) {
        waiting.execute {
            val result = pearClient.answers(question.id)
            main.post {
                val now = android.os.SystemClock.uptimeMillis()
                val me = result?.answers?.firstOrNull { it.me }
                val noes = result?.answers?.filter { !it.me }.orEmpty()
                if (noes.isNotEmpty() && question.firstNo == 0L) question.firstNo = now
                val someone = context.getString(R.string.check_someone)
                when {
                    me != null -> tell(context.getString(R.string.check_result_me, me.name.ifEmpty { someone }))
                    result?.gone == true -> tell(context.getString(R.string.check_result_silence))
                    result != null && result.asked > 0 && noes.size >= result.asked -> tell(context.getString(R.string.check_result_none))
                    question.firstNo > 0 && now - question.firstNo > CHECK_GRACE_MS ->
                        tell(context.getString(R.string.check_result_named, noes.joinToString(", ") { it.name.ifEmpty { someone } }))
                    now - question.started > CHECK_MAX_MS -> tell(context.getString(R.string.check_result_silence))
                    else -> main.postDelayed(again, CHECK_POLL_MS)
                }
            }
        }
    }

    /** Said aloud, unless Peri is talking or waiting for an answer: then only written in her bubble. */
    private fun tell(text: String) = if (speaking || agent.isWaitingForAnswer) show(text) else say(text)

    val isWaitingForAnswer: Boolean get() = agent.isWaitingForAnswer

    /** A phone call is going on: the microphone stays closed so Peri never hears it. */
    val inCall: Boolean get() = callActive

    /** Peri's face for the screens; it changes whenever she speaks. */
    val mood: Mood get() = agent.mood

    init {
        main.postDelayed(familyLoop, FAMILY_NOTES_MS)
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
        weeklySummary()
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
        // A number the person (or the family) blocked: never read aloud, only a line in the diary.
        blockedHere(message.sender)?.let { by ->
            diary.add(app.friendlypear.config.Diary.Kind.BLOCKED_TEXT, if (by == Blocker.FAMILY) app.friendlypear.config.Diary.BY_FAMILY else "")
            Log.i(TAG, "message from a blocked sender silenced")
            return@post
        }
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
        if (asking && verdict.level == Level.DOUBT) askModel(message, verdict) else shareVerdict(message, verdict)
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
                after?.let { shareVerdict(message, it) }
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

    /**
     * The weekly summary for the guardians: only the counts they can already see on their page, once a week while
     * sharing is on. Checked when the app opens and once a day by [app.friendlypear.services.SummaryReceiver]; [now]
     * sends it at once (debug builds, to try it).
     */
    fun weeklySummary(now: Boolean = false) = main.post {
        if (!news.sharing || !permissions.granted(Need.SEND_SMS)) return@post
        val at = System.currentTimeMillis()
        if (!now && !app.friendlypear.core.alerts.WeeklySummary.due(at, news.lastSummary, news.sharingSince)) return@post
        val guardians = contacts.orEmpty().filter { it.trust == TrustLevel.GUARDIAN && it.phones.isNotEmpty() }
        if (guardians.isEmpty()) return@post
        val events = news.load().mapNotNull { e ->
            when (e.kind) {
                "warning" -> app.friendlypear.core.alerts.FamilyEvent.Warning(e.at, scam = e.level == "scam")
                "pause" -> app.friendlypear.core.alerts.FamilyEvent.Pause(e.at, e.answer)
                else -> null
            }
        }
        val text = app.friendlypear.core.alerts.WeeklySummary.text(events, at, lang)
        news.lastSummary = at
        waiting.execute {
            val sent = guardians.count { g -> runCatching { device.sendSms(g.phones.first(), text) }.isSuccess }
            Log.i(TAG, "weekly summary sent to $sent of ${guardians.size} guardian(s)")
        }
    }

    /** Only what Peri decided, never the message: a scam or a doubt, and the channel it came by. */
    private fun shareVerdict(message: IncomingMessage, verdict: Verdict) {
        val level = verdict.level
        // A website's scary or scammy notice: offer to silence that site, the way back to a calm phone.
        if (message.channel == Channel.BROWSER_NOTIFICATION) {
            val notice = pendingWeb.remove(message.text)
            if (notice != null && level != Level.OK) {
                webNotices.save(notice)
                views.forEach { it.scamToReport() }
            }
        }
        if (level == Level.SCAM) {
            earn(Shields.Kind.STOPPED)
            val signals = verdict.signals.map { it.name }
            val kind = app.friendlypear.ui.peri.TrapActivity.kindOf(signals, verdict.brand)
            lastScam.save(LastScam.Scam(message.channel.name, message.sender, message.text, message.receivedAtMillis, verdict.explanation, signals, kind))
            diary.add(app.friendlypear.config.Diary.Kind.SCAM_STOPPED, kind)
            views.forEach { it.scamToReport() }
        }
        if (level == Level.DOUBT) diary.add(app.friendlypear.config.Diary.Kind.DOUBT_CHECKED)
        // A stranger's number claiming to be family: the family's Peris are asked "was it you?".
        if (level != Level.OK && app.friendlypear.core.shield.Signal.NEW_NUMBER_CLAIM in verdict.signals && familyContact(message.sender) == null) askFamily()
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
        uses.add(UseLog.Kind.PICTURE)
        earn(Shields.Kind.ASKED)
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
        // "Read my messages": she holds up a letter while she reads.
        val reading = !agent.isWaitingForAnswer && VoiceCommands.parse(utterance) is Command.ReadMessages
        agent.onHeard(utterance)
        if (reading) views.forEach { it.moment(PeriMoment.LETTER) }
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
                // An app able to control the screen, as if switched on in Settings: "--es intruder 'package|name'".
                intent.getStringExtra("intruder")?.split("|", limit = 2)?.takeIf { it.size == 2 }?.let { (pkg, name) ->
                    main.post { screenAccess(listOf(pkg to name)) }
                }
                // An email as its notification reaches Peri: "--es email 'sender|subject. text'" (no Gmail account here).
                intent.getStringExtra("email")?.split("|", limit = 2)?.takeIf { it.size == 2 }?.let { (sender, text) ->
                    onMessage(IncomingMessage(Channel.EMAIL, sender, text, System.currentTimeMillis()))
                }
                // A website's notice through Chrome: "--es web 'site|title|text'".
                intent.getStringExtra("web")?.split("|", limit = 3)?.takeIf { it.size == 3 }?.let { (site, title, text) ->
                    val now = System.currentTimeMillis()
                    onWebNotice(IncomingMessage(Channel.BROWSER_NOTIFICATION, site, "$title. $text", now), WebNotice.Notice("com.android.chrome", null, site, now))
                }
                if (intent.getStringExtra("summary") == "now") weeklySummary(now = true)
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
        const val PEAR_NAME = "name"
        /** A scam this recent is why the pear needs attention, and its kind travels with it. */
        const val SCAM_KIND_MS = 30 * 60_000L
        const val CHECK_POLL_MS = 10_000L
        const val HANGUP_POLL_MS = 6_000L
        /** Three seconds for "keep talking", and a moment for Peri to say it. */
        const val HANGUP_WAIT_MS = 6_000L
        const val FAMILY_NOTES_MS = 60_000L
        /** The family's blocked numbers are fetched again after this long (and at once after a change here). */
        const val BLOCKS_SYNC_MS = 10 * 60_000L
        const val HELP_UNTIL = "until"
        const val HELP_MS = 24 * 60 * 60_000L
        const val ALERTS_CHANNEL = "peri_alerts"
        const val WARNED = "warned"
        const val REPUBLISH_MS = 10_000L
        /** After this, a "was it you?" stops waiting for answers. */
        const val CHECK_MAX_MS = 10 * 60_000L
        /** After the first "it wasn't me", this long for anyone else to say it was them. */
        const val CHECK_GRACE_MS = 60_000L
        /** Peri warning, protecting, or waiting for the family's answer: the family's pear needs attention. */
        val ATTENTION_MOODS = setOf(Mood.WORRIED, Mood.PROTECTIVE, Mood.WAITING)
    }
}
