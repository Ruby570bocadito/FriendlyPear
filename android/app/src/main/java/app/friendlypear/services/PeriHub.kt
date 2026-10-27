package app.friendlypear.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.friendlypear.BuildConfig
import app.friendlypear.config.PhoneContacts
import app.friendlypear.core.agent.ApprovalKind
import app.friendlypear.core.agent.ApprovalPort
import app.friendlypear.core.agent.ApprovalResult
import app.friendlypear.core.agent.PeriAgent
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.model.Level
import app.friendlypear.core.shield.Verdict
import app.friendlypear.device.AndroidDevice
import app.friendlypear.device.phoneLang
import app.friendlypear.net.ApprovalClient
import app.friendlypear.net.OpinionClient
import java.util.concurrent.Executors

/**
 * Owns the one [PeriAgent] of the app and feeds it on the main thread: messages from [MessageListener],
 * call state from [CallStateWatcher] and voice from the screen. Doubtful messages get Muse Spark Open's
 * second opinion through the backend when it is configured, and high-risk actions wait for a guardian.
 */
class PeriHub(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /** The screen shows what Peri says, large, for people who hear badly. */
    var onSaid: ((String) -> Unit)? = null
    var onSpeechDone: (() -> Unit)? = null
    var lastSaid: String = ""
        private set

    /** Read once: Peri keeps one language for the whole session. */
    val lang = phoneLang()

    private val device = AndroidDevice(
        this.context,
        lang,
        onSpeech = { text ->
            lastSaid = text
            onSaid?.invoke(text)
        },
        onSpeechDone = { onSpeechDone?.invoke() },
    )
    private var contacts: List<Contact>? = null
    private val approvalClient = ApprovalClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    /** Waiting for a guardian can take minutes, so it never shares a thread with second opinions. */
    private val waiting = Executors.newCachedThreadPool()

    /** Family MFA: create the one-time link, text it to the guardians, then wait for their answer. */
    private val approvals: ApprovalPort = object : ApprovalPort {
        override fun request(ticket: String, kind: ApprovalKind, toName: String, guardians: List<Contact>, smsFor: (url: String) -> String) {
            val asking = agent
            waiting.execute {
                val created = approvalClient.create(kind, toName, lang)
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
        }
    }
    private val countdownEnd = Runnable { agent.onEmergencyTimeout() }
    private val opinion = OpinionClient(BuildConfig.BACKEND_URL, BuildConfig.DEVICE_TOKEN)
    private val network = Executors.newSingleThreadExecutor()

    val isWaitingForAnswer: Boolean get() = agent.isWaitingForAnswer

    init {
        if (BuildConfig.DEBUG) registerDebugVoice()
    }

    /** Reloads contacts and listeners after permissions change; the agent keeps its state if nothing changed. */
    fun refresh() = main.post {
        calls.start()
        val fresh = PhoneContacts.load(context)
        if (fresh != contacts) {
            contacts = fresh
            agent = newAgent(fresh).also { it.onCallStateChanged(callActive) }
        }
    }

    fun onMessage(message: IncomingMessage) = main.post {
        if (contacts == null) refreshNow()
        val asking = opinion.enabled
        val verdict = agent.onMessage(message, secondOpinion = asking)
        // Never the text or the sender: only what Peri decided.
        Log.i(TAG, "message channel=${message.channel} level=${verdict.level} hardRule=${verdict.hardRule}")
        if (asking && verdict.level == Level.DOUBT) askModel(message, verdict)
    }

    /** Off the main thread; the agent hears back on it, with the vote or with null if anything failed. */
    private fun askModel(message: IncomingMessage, verdict: Verdict) {
        val waitingAgent = agent
        val call = callActive
        network.execute {
            val delta = opinion.ask(message, verdict, call, lang)
            main.post {
                val after = waitingAgent.onModelOpinion(message, delta)
                Log.i(TAG, "second opinion delta=${delta ?: "none"} level=${after?.level}")
            }
        }
    }

    /** From [CallScreener], before the call is picked up. The number itself is never logged. */
    fun onIncomingCall(number: String) = main.post {
        if (contacts == null) refreshNow()
        agent.onIncomingCall(number)
        Log.i(TAG, "incoming call screened")
    }

    fun heard(utterance: String) = main.post {
        agent.onHeard(utterance)
        main.removeCallbacks(countdownEnd)
        if (agent.isCountingDown) main.postDelayed(countdownEnd, COUNTDOWN_MILLIS)
    }

    private fun newAgent(contacts: List<Contact>): PeriAgent =
        PeriAgent(device, contacts, lang, approvals = if (approvalClient.enabled) approvals else null)

    private fun refreshNow() {
        calls.start()
        contacts = PhoneContacts.load(context).also { agent = newAgent(it).apply { onCallStateChanged(callActive) } }
    }

    /**
     * Debug builds only: lets the emulator "speak" with
     * `adb shell am broadcast -a app.friendlypear.debug.HEAR --es text "si"`, since it has no microphone.
     */
    private fun registerDebugVoice() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                intent.getStringExtra("text")?.let { heard(it) }
            }
        }
        context.registerReceiver(receiver, IntentFilter(DEBUG_HEAR), Context.RECEIVER_EXPORTED)
    }

    private companion object {
        const val TAG = "FriendlyPear"
        const val COUNTDOWN_MILLIS = 10_000L
        const val DEBUG_HEAR = "app.friendlypear.debug.HEAR"
    }
}
