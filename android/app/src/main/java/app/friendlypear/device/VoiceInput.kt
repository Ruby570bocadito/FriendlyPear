package app.friendlypear.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import app.friendlypear.core.model.Lang

/**
 * Android's speech recognizer in Peri's language, with long pauses allowed: older people speak slowly.
 * It never opens during a phone call ([mayListen]), so Peri can't hear the other person, even on speaker; if it
 * was already open when a call starts, [stop] closes it, and anything it heard by then is dropped.
 */
class VoiceInput(
    context: Context,
    private val lang: Lang,
    private val onText: (String) -> Unit,
    private val onListening: (Boolean) -> Unit,
    private val mayListen: () -> Boolean = { true },
    /** Muse Voice Transcribe, when the person turned it on in Settings; otherwise Android's own recognizer. */
    private val ears: Ears? = null,
    /** Nothing understood (silence, or the transcription failed). */
    private val onMissed: () -> Unit = {},
) {
    interface Ears {
        val on: Boolean
        fun transcribe(wav: ByteArray, done: (String?) -> Unit)
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val recorder = VoiceRecorder()
    @Volatile
    private var recording = false
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null

    val available: Boolean get() = recognizer != null

    init {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = onListening(true)
            override fun onResults(results: Bundle?) {
                onListening(false)
                // A call began while it was listening: nothing from around the call is acted on.
                if (!mayListen()) return
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onText)
            }

            override fun onError(error: Int) = onListening(false)
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
    }

    /** False when it may not listen now (a call is going on). */
    fun listen(): Boolean {
        if (!mayListen()) return false
        val muse = ears
        if (muse != null && muse.on) {
            if (recording) return true
            recording = true
            onListening(true)
            Thread {
                val wav = recorder.record()
                main.post {
                    recording = false
                    onListening(false)
                    if (!mayListen()) return@post
                    if (wav == null) return@post onMissed()
                    muse.transcribe(wav) { text -> if (text.isNullOrBlank()) onMissed() else onText(text) }
                }
            }.start()
            return true
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale(lang).toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
        recognizer?.startListening(intent)
        return true
    }

    /** Closes the microphone now, whatever it was doing (a call started, or the screen went away). */
    fun stop() {
        recorder.stop()
        recognizer?.cancel()
        onListening(false)
    }

    fun destroy() {
        recorder.stop()
        recognizer?.destroy()
    }
}
