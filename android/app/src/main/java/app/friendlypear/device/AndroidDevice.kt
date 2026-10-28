package app.friendlypear.device

import android.telecom.TelecomManager
import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.telephony.SmsManager
import app.friendlypear.config.UseLog
import app.friendlypear.core.agent.Capability
import app.friendlypear.core.agent.DevicePort
import app.friendlypear.core.model.Lang
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * The phone side of [DevicePort]. Permissions are asked through [ask] the first time they're needed; a missing
 * permission or a failed intent at the moment of acting throws, which the agent answers with a plan B.
 */
class AndroidDevice(
    context: Context,
    lang: Lang,
    private val onSpeech: (String) -> Unit,
    private val onSpeechDone: () -> Unit,
    /** When the voice really starts playing: some engines report "done" early, so the mouth follows this too. */
    private val onSpeechStart: () -> Unit = {},
    private val ask: (Capability, (Boolean) -> Unit) -> Unit,
    private val pause: (text: String, callName: String?, hangUp: Boolean, keep: Boolean) -> Unit = { _, _, _, _ -> },
    /** Right after Peri places a call, so Mini Peri can stay beside the call screen. */
    private val onCalling: () -> Unit = {},
    /** Each call, text and location share, for the person's own log ("What Peri did"). */
    private val onUse: (UseLog.Kind, String?) -> Unit = { _, _ -> },
) : DevicePort {
    override fun prepare(capability: Capability, then: (Boolean) -> Unit) = ask(capability, then)

    override fun showAftercare(what: app.friendlypear.core.agent.Breach) {
        main.post { runCatching { context.startActivity(app.friendlypear.ui.peri.AftercareActivity.intent(context, what)) } }
    }

    override fun showPause(text: String, callName: String?, hangUp: Boolean, keep: Boolean) {
        main.post { pause(text, callName, hangUp, keep) }
    }

    /** Ends the call on the line: Android's permission to end calls is asked with the one to see calls. */
    @SuppressLint("MissingPermission")
    override fun hangUp(): Boolean {
        if (!granted(Manifest.permission.ANSWER_PHONE_CALLS)) return false
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        @Suppress("DEPRECATION")
        val ended = runCatching { telecom.endCall() }.getOrDefault(false)
        if (ended) onUse(UseLog.Kind.HANG_UP, null)
        return ended
    }

    /** Right after a hang-up the line takes a moment to free up; a call waits for it, a few seconds at most. */
    @SuppressLint("MissingPermission")
    private fun lineBusy(): Boolean =
        granted(Manifest.permission.READ_PHONE_STATE) && context.getSystemService(TelecomManager::class.java)?.isInCall == true

    private var lineWaits = 0

    private val context = context.applicationContext

    /** Peri's language; changing it switches her voice at once. */
    var lang: Lang = lang
        set(value) {
            field = value
            if (ready) useVoice()
        }
    private val main = Handler(Looper.getMainLooper())
    private val queued = ArrayDeque<String>()
    private val ids = AtomicInteger()
    private var ready = false
    private var usingGoogle = false
    /**
     * Google's speech engine when the phone has it: some makers' own engines sound robotic, and older people
     * understand a natural voice better. Otherwise the phone's default engine.
     */
    private val tts: TextToSpeech = run {
        val google = runCatching { this.context.packageManager.getPackageInfo(GOOGLE_TTS, 0) }.isSuccess
        usingGoogle = google
        val ready = TextToSpeech.OnInitListener { status -> main.post { onTtsReady(status) } }
        if (google) TextToSpeech(this.context, ready, GOOGLE_TTS) else TextToSpeech(this.context, ready)
    }

    private fun onTtsReady(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        useVoice()
        // A warm, unhurried voice (docs/PERI_VOZ.md): a little slower than default, at its natural pitch.
        tts.setSpeechRate(0.9f)
        tts.setPitch(1.0f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                main.post { onSpeechStart() }
            }
            override fun onDone(utteranceId: String?) {
                main.post { if (!tts.isSpeaking) onSpeechDone() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                main.post { onSpeechDone() }
            }
        })
        ready = true
        while (queued.isNotEmpty()) say(queued.removeFirst())
    }

    private val prefs = this.context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun useVoice() {
        val locale = speechLocale(lang)
        tts.language = locale
        // The voice the person chose in Settings, if this phone still has it; otherwise the best offline one.
        val chosen = prefs.getString(voiceKey(), null)?.let { name -> tts.voices.orEmpty().firstOrNull { it.name == name } }
        (chosen ?: bestOfflineVoice(locale))?.let { tts.voice = it }
    }

    private fun voiceKey() = "voice_${lang.name}"

    /** The installed voices for Peri's language, the best first, for the Settings chooser. */
    fun voices(): List<Voice> {
        if (!ready) return emptyList()
        val locale = speechLocale(lang)
        return tts.voices.orEmpty()
            .filter { it.locale.language == locale.language && "notInstalled" !in it.features }
            .sortedWith(compareByDescending<Voice> { it.locale.country == locale.country }.thenByDescending { it.quality }.thenBy { it.isNetworkConnectionRequired })
            .take(MAX_VOICES)
    }

    val voiceName: String? get() = if (ready) tts.voice?.name else null

    /** Whether Google's engine is the one speaking (the most natural voices on most phones). */
    val googleEngine: Boolean get() = usingGoogle

    fun chooseVoice(name: String) {
        prefs.edit().putString(voiceKey(), name).apply()
        if (ready) useVoice()
    }

    /** The best voice for the language that works without a connection, so Peri never goes silent. */
    private fun bestOfflineVoice(locale: Locale): Voice? = tts.voices.orEmpty()
        .filter { it.locale.language == locale.language && !it.isNetworkConnectionRequired && "notInstalled" !in it.features }
        .sortedWith(compareByDescending<Voice> { it.locale.country == locale.country }.thenByDescending { it.quality })
        .firstOrNull()

    override fun speak(text: String) {
        onSpeech(text)
        if (ready) say(text) else queued.addLast(text)
    }

    private fun say(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "peri-${ids.incrementAndGet()}")
    }

    override fun call(phone: String) {
        require(granted(Manifest.permission.CALL_PHONE)) { "CALL_PHONE not granted" }
        if (lineBusy() && lineWaits < LINE_WAITS) {
            lineWaits++
            main.postDelayed({ call(phone) }, LINE_WAIT_MS)
            return
        }
        lineWaits = 0
        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phone, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        onUse(UseLog.Kind.CALL, phone)
        main.post(onCalling)
    }

    override fun sendSms(phone: String, text: String) {
        require(granted(Manifest.permission.SEND_SMS)) { "SEND_SMS not granted" }
        val sms = context.getSystemService(SmsManager::class.java) ?: error("no SMS on this device")
        sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null)
        onUse(UseLog.Kind.TEXT, phone)
    }

    override fun lastKnownLocation(): String? {
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val best = try {
            manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        } ?: return null
        onUse(UseLog.Kind.LOCATION, null)
        return String.format(Locale.US, "%.5f,%.5f", best.latitude, best.longitude)
    }

    fun shutdown() = tts.shutdown()

    private fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

/** Peri speaks Spanish on a Spanish phone and English everywhere else, keeping the phone's English accent. */
fun phoneLang(): Lang = if (Locale.getDefault().language == "es") Lang.ES else Lang.EN

fun speechLocale(lang: Lang): Locale = when (lang) {
    Lang.ES -> Locale("es", "ES")
    Lang.EN -> Locale.getDefault().takeIf { it.language == "en" } ?: Locale.US
}

const val GOOGLE_TTS = "com.google.android.tts"
private const val MAX_VOICES = 8

/** How long a call waits for the line to free up after a hang-up: six times 0.8 s at most. */
private const val LINE_WAITS = 6
private const val LINE_WAIT_MS = 800L
