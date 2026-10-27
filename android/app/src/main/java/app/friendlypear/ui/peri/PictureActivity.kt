package app.friendlypear.ui.peri

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.net.PictureVerdict
import app.friendlypear.services.PeriView
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.max

/**
 * "Look at this picture": the person picks a photo or screenshot (a letter, an invoice, a QR code), or shares
 * one with Peri from any app. Muse Spark reads it and SAM 3.1 marks what's risky; Peri says what to do in her
 * own words, built from the verdict and the kinds of risk, never the model's. The picture is never stored.
 */
class PictureActivity : Activity() {
    private lateinit var picture: MarkedPicture
    private lateinit var face: PeriFace
    private lateinit var said: TextView
    private val work = Executors.newSingleThreadExecutor()
    private var shown: Bitmap? = null
    /** The sentence about the picture: while it's on screen, its face stays (the agent's own mood is for other things). */
    private var mine: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picture = MarkedPicture(this)
        face = PeriFace(this).apply { show(Mood.CALM) }
        said = TextView(this).apply {
            textSize = 20f
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.1f)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            val tail = dp(12).toFloat()
            background = BubbleDrawable(Palette.WHITE, 0, 0f, dp(22).toFloat(), tail, BubbleDrawable.Tail.BOTTOM_START, tailInset = dp(44).toFloat())
            elevation = dp(3).toFloat()
            setPadding(dp(18), dp(14), dp(18), dp(16) + tail.toInt())
        }
        val again = Pill(this, getString(R.string.picture_another), R.drawable.ic_image_search, primary = false, size = 18f).apply {
            setOnClickListener { pick() }
        }
        val done = Pill(this, getString(R.string.picture_done), R.drawable.ic_chevron, primary = true, size = 18f).apply {
            setOnClickListener { finish() }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Palette.CREAM, Palette.MEADOW))
            clipChildren = false
            addView(heading(getString(R.string.picture_title), 26f))
            addView(picture, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10) })
            addView(said, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
            addView(face, LinearLayout.LayoutParams(dp(110), dp(110)).apply { gravity = Gravity.START })
            addView(again, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)))
            addView(done, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)).apply { topMargin = dp(10) })
        }
        val pad = dp(20)
        root.setOnApplyWindowInsetsListener { v, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(pad + bars.left, dp(12) + bars.top, pad + bars.right, pad + bars.bottom)
            insets
        }
        setContentView(root)

        val shared = if (intent?.action == Intent.ACTION_SEND) sharedUri(intent) else null
        when {
            savedInstanceState != null -> said.text = getString(R.string.picture_intro)
            shared != null -> look(shared)
            else -> {
                said.text = getString(R.string.picture_intro)
                pick()
            }
        }
    }

    private fun sharedUri(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)

    /** Android's photo picker: Peri only sees the one picture the person chooses, and needs no permission. */
    private fun pick() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Intent(MediaStore.ACTION_PICK_IMAGES)
        else Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
        startActivityForResult(intent, REQUEST_PICK)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK) return
        val uri = data?.data
        if (uri != null) look(uri) else if (shown == null) finish()
    }

    private fun look(uri: Uri) {
        face.show(Mood.THINKING)
        said.text = getString(R.string.picture_looking)
        picture.show(null)
        val hub = PeriApp.hub(this)
        work.execute {
            val bitmap = runCatching { load(uri) }.getOrNull()
            runOnUiThread {
                if (bitmap == null) return@runOnUiThread answer(null)
                shown = bitmap
                picture.show(bitmap)
                hub.lookAtPicture(jpeg(bitmap)) { verdict -> if (!isFinishing) answer(verdict) }
            }
        }
    }

    /** At most 1280 px on the long side, upright: about 1,400 tokens for Muse Spark, sharp enough for a QR code. */
    private fun load(uri: Uri): Bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
        val longest = max(info.size.width, info.size.height)
        if (longest > MAX_SIDE) {
            val f = MAX_SIDE.toFloat() / longest
            decoder.setTargetSize((info.size.width * f).toInt(), (info.size.height * f).toInt())
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }

    private fun jpeg(bitmap: Bitmap): String = ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /** Peri's own sentence: the verdict, then what not to do for each kind of risk she saw. */
    private fun answer(v: PictureVerdict?) {
        val text: String
        val mood: Mood
        if (v == null) {
            text = getString(R.string.picture_failed)
            mood = Mood.CALM
        } else {
            val donts = v.kinds.mapNotNull { DONTS[it] }.map { getString(it) }
            val marked = if (v.marks.isNotEmpty()) " " + getString(R.string.picture_marked) else ""
            when (v.verdict) {
                "scam" -> {
                    text = (listOf(getString(R.string.picture_scam)) + donts).joinToString(" ") + marked + " " + getString(R.string.picture_check_yourself)
                    mood = Mood.PROTECTIVE
                }
                "ok" -> {
                    text = getString(R.string.picture_ok)
                    mood = Mood.HAPPY
                }
                else -> {
                    text = (listOf(getString(R.string.picture_doubt)) + donts).joinToString(" ") + marked + " " + getString(R.string.picture_ask_family)
                    mood = Mood.WORRIED
                }
            }
            picture.show(shown, v.marks.map { it to getString(LABELS[it.kind] ?: R.string.picture_label_risk) })
        }
        face.show(mood)
        said.text = text
        mine = text
        PeriApp.hub(this).say(text)
    }

    /** Peri on this screen is still Peri: a warning that arrives meanwhile (a scam text, a call) shows here too. */
    private val view = object : PeriView {
        override fun said(text: String) {
            said.text = text
        }

        override fun mood(mood: Mood) {
            if (said.text.toString() != mine) face.show(mood)
        }

        override fun speaking(on: Boolean) = face.speaking(on)
    }

    override fun onResume() {
        super.onResume()
        val hub = PeriApp.hub(this)
        hub.attach(view)
        // Peri is on screen here: Mini Peri steps aside, as with the main screen.
        hub.appVisible = true
    }

    override fun onPause() {
        val hub = PeriApp.hub(this)
        hub.detach(view)
        hub.appVisible = false
        face.speaking(false)
        super.onPause()
    }

    override fun onDestroy() {
        work.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        const val REQUEST_PICK = 1
        const val MAX_SIDE = 1280

        val DONTS = mapOf(
            "qr" to R.string.picture_dont_qr,
            "link" to R.string.picture_dont_link,
            "phone" to R.string.picture_dont_phone,
            "payment" to R.string.picture_dont_pay,
            "code_request" to R.string.picture_dont_code,
            "remote_app" to R.string.picture_dont_app,
        )
        val LABELS = mapOf("qr" to R.string.picture_label_qr)
    }
}
