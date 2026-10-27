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
import app.friendlypear.core.agent.PeriAgent
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.device.AndroidDevice
import app.friendlypear.device.phoneLang

/**
 * Owns the one [PeriAgent] of the app and feeds it on the main thread: messages from [MessageListener],
 * call state from [CallStateWatcher] and voice from the screen.
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
    private var agent = PeriAgent(device, emptyList(), lang)
    private var callActive = false
    private val calls = CallStateWatcher(this.context) { active ->
        main.post {
            Log.i(TAG, "call active=$active")
            callActive = active
            agent.onCallStateChanged(active)
        }
    }
    private val countdownEnd = Runnable { agent.onEmergencyTimeout() }

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
            agent = PeriAgent(device, fresh, lang).also { it.onCallStateChanged(callActive) }
        }
    }

    fun onMessage(message: IncomingMessage) = main.post {
        if (contacts == null) refreshNow()
        val verdict = agent.onMessage(message)
        // Never the text or the sender: only what Peri decided.
        Log.i(TAG, "message channel=${message.channel} level=${verdict.level} hardRule=${verdict.hardRule}")
    }

    fun heard(utterance: String) = main.post {
        agent.onHeard(utterance)
        main.removeCallbacks(countdownEnd)
        if (agent.isCountingDown) main.postDelayed(countdownEnd, COUNTDOWN_MILLIS)
    }

    private fun refreshNow() {
        calls.start()
        contacts = PhoneContacts.load(context).also { agent = PeriAgent(device, it, lang).apply { onCallStateChanged(callActive) } }
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
