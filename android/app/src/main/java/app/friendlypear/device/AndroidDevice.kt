package app.friendlypear.device

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
import android.telephony.SmsManager
import app.friendlypear.core.agent.DevicePort
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * The phone side of [DevicePort]. A missing permission or a failed intent throws, which the agent
 * reads as a failed action and answers with a plan B.
 */
class AndroidDevice(
    context: Context,
    private val onSpeech: (String) -> Unit,
    private val onSpeechDone: () -> Unit,
) : DevicePort {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val queued = ArrayDeque<String>()
    private val ids = AtomicInteger()
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(this.context) { status -> main.post { onTtsReady(status) } }

    private fun onTtsReady(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        tts.language = Locale("es", "ES")
        tts.setSpeechRate(0.9f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
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

    override fun speak(text: String) {
        onSpeech(text)
        if (ready) say(text) else queued.addLast(text)
    }

    private fun say(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "peri-${ids.incrementAndGet()}")
    }

    override fun call(phone: String) {
        require(granted(Manifest.permission.CALL_PHONE)) { "CALL_PHONE not granted" }
        val intent = Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phone, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    override fun sendSms(phone: String, text: String) {
        require(granted(Manifest.permission.SEND_SMS)) { "SEND_SMS not granted" }
        val sms = context.getSystemService(SmsManager::class.java) ?: error("no SMS on this device")
        sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null)
    }

    override fun lastKnownLocation(): String? {
        if (!granted(Manifest.permission.ACCESS_FINE_LOCATION) && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val best = try {
            manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        } ?: return null
        return String.format(Locale.US, "%.5f,%.5f", best.latitude, best.longitude)
    }

    fun shutdown() = tts.shutdown()

    private fun granted(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
