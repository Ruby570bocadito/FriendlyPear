package app.friendlypear.device

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Records one voice order for Muse Voice Transcribe: 16 kHz mono 16-bit PCM, ending by itself after a long pause
 * (older people speak slowly: 1.8 s), after 7 s of nothing said, or at 15 s. Blocking: call it off the main thread.
 * Returns the order as a WAV, or null if nobody spoke. Nothing is written to disk.
 */
class VoiceRecorder {
    @Volatile
    private var stopped = false

    fun stop() {
        stopped = true
    }

    /** RECORD_AUDIO is checked by the caller (the permission gate) before it records. */
    @SuppressLint("MissingPermission")
    fun record(): ByteArray? {
        stopped = false
        val minimum = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minimum <= 0) return null
        val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minimum, FRAME * 2 * 4))
        if (recorder.state != AudioRecord.STATE_INITIALIZED) return null.also { recorder.release() }
        val pcm = ByteArrayOutputStream()
        val frame = ShortArray(FRAME)
        val noise = mutableListOf<Double>()
        var spoke = false
        var loud = 0
        var quiet = 0
        var frames = 0
        try {
            recorder.startRecording()
            while (!stopped && frames < MAX_FRAMES) {
                val n = recorder.read(frame, 0, FRAME)
                if (n <= 0) break
                frames++
                val bytes = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
                var sum = 0.0
                for (i in 0 until n) {
                    bytes.putShort(frame[i])
                    sum += frame[i].toDouble() * frame[i]
                }
                pcm.write(bytes.array())
                val rms = sqrt(sum / n)
                // The first few frames tell how loud the room is; speech is clearly above that.
                if (frames <= NOISE_FRAMES) noise += rms
                val threshold = max(noise.average() * 2.5, MIN_THRESHOLD)
                if (rms > threshold) {
                    loud++
                    quiet = 0
                    if (loud >= 2) spoke = true
                } else {
                    loud = 0
                    quiet++
                }
                if (spoke && quiet >= END_SILENCE_FRAMES) break
                if (!spoke && frames >= NOTHING_SAID_FRAMES) break
            }
        } finally {
            runCatching { recorder.stop() }
            recorder.release()
        }
        return if (spoke) wav(pcm.toByteArray()) else null
    }

    private companion object {
        const val RATE = 16_000
        /** 100 ms. */
        const val FRAME = 1_600
        const val NOISE_FRAMES = 3
        const val MIN_THRESHOLD = 350.0
        const val END_SILENCE_FRAMES = 18
        const val NOTHING_SAID_FRAMES = 70
        const val MAX_FRAMES = 150

        fun wav(pcm: ByteArray): ByteArray {
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(36 + pcm.size)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(1)
                putInt(RATE)
                putInt(RATE * 2)
                putShort(2)
                putShort(16)
                put("data".toByteArray())
                putInt(pcm.size)
            }
            return header.array() + pcm
        }
    }
}
