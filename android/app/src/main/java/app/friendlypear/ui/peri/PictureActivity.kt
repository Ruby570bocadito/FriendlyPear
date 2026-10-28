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
import app.friendlypear.core.model.Level
import app.friendlypear.core.shield.QrAction
import app.friendlypear.core.shield.QrJudgement
import app.friendlypear.core.shield.QrRisk
import app.friendlypear.core.shield.QrTraps
import app.friendlypear.device.QrReader
import app.friendlypear.net.PictureMark
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
            sizeSp(TextSize.BODY)
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.1f)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            val tail = dp(12).toFloat()
            background = BubbleDrawable(Palette.WHITE, Palette.LINE, dp(1).toFloat(), dp(22).toFloat(), tail, BubbleDrawable.Tail.BOTTOM_START, tailInset = dp(44).toFloat())
            setPadding(dp(18), dp(14), dp(18), dp(16) + tail.toInt())
        }
        val again = Pill(this, getString(R.string.picture_another), R.drawable.ic_image_search, primary = false).apply {
            setOnClickListener { pick() }
        }
        val done = Pill(this, getString(R.string.picture_done), R.drawable.ic_chevron, primary = true).apply {
            setOnClickListener { finish() }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ground()
            clipChildren = false
            addView(heading(getString(R.string.picture_title)))
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
            shared != null && !allowed(shared) -> {
                said.text = getString(R.string.picture_refused)
                pick()
            }
            shared != null -> look(shared)
            else -> {
                said.text = getString(R.string.picture_intro)
                pick()
            }
        }
    }

    /**
     * Only a picture another app hands over is looked at: a content:// address that isn't the phone's address book,
     * call log or messages (Peri may read those; whoever shares must not borrow that). A file:// path could point at
     * Peri's own files, so it's refused; debug builds take one inside Peri's own folder, for the tests.
     */
    private fun allowed(uri: Uri): Boolean = when (uri.scheme?.lowercase()) {
        "content" -> uri.authority?.lowercase()?.let { a -> REFUSED.none { a == it || a.startsWith("$it.") } } == true
        "file" -> app.friendlypear.BuildConfig.DEBUG && runCatching {
            java.io.File(uri.path.orEmpty()).canonicalPath.startsWith(filesDir.canonicalPath + java.io.File.separator)
        }.getOrDefault(false)
        else -> false
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
        shown = null
        // Only the latest picture answers: an earlier one's late verdict never lands on this one.
        val turn = ++looks
        val hub = PeriApp.hub(this)
        work.execute {
            val bitmap = runCatching { load(uri) }.getOrNull()
            // Its QR codes, read and judged here, never opened: what they hold is decided before any model looks.
            val codes = bitmap?.let { QrReader.read(it) }.orEmpty().map { it to QrTraps.judge(it.text) }
            runOnUiThread {
                if (turn != looks) return@runOnUiThread
                if (bitmap == null) return@runOnUiThread answer(null, emptyList())
                shown = bitmap
                picture.show(bitmap)
                val risky = codes.any { it.second.risks.isNotEmpty() }
                hub.lookAtPicture(jpeg(bitmap), risky) { verdict -> if (!isFinishing && turn == looks) answer(verdict, codes) }
            }
        }
    }

    private var looks = 0

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

    /**
     * Peri's own sentence, danger first: the verdict, what the picture's QR codes would do (judged on the phone, the
     * worst one speaking for them), what not to do for each kind of risk seen, and the next step. A QR code can make
     * the verdict worse, never better: one that looks fine is mentioned only when the rest does too. With no model at
     * hand (offline, say) what the codes hold still stands.
     */
    private fun answer(v: PictureVerdict?, codes: List<Pair<QrReader.Found, QrJudgement>>) {
        val qr = codes.maxByOrNull { it.second.level }?.second
        val seen = when (v?.verdict) {
            null -> null
            "scam" -> Level.SCAM
            "ok" -> Level.OK
            else -> Level.DOUBT
        }
        val overall = listOfNotNull(seen, qr?.level).maxOrNull()
        val worried = seen == Level.SCAM || seen == Level.DOUBT
        val parts = mutableListOf<String>()
        if (overall == Level.SCAM) parts += getString(R.string.picture_scam)
        else if (worried) parts += getString(R.string.picture_doubt)
        if (qr != null && (qr.level != Level.OK || !worried)) parts += when {
            codes.size == 1 -> getString(R.string.qr_found) + " " + qrSentence(qr)
            qr.level == Level.OK -> getString(R.string.qr_many_ok)
            else -> getString(R.string.qr_found_many) + " " + qrSentence(qr)
        }
        // A trap code has already been told not to be scanned.
        if (worried) parts += v?.kinds.orEmpty().filter { !(it == "qr" && qr?.level == Level.SCAM) }.mapNotNull { DONTS[it] }.map { getString(it) }
        // The rings: the phone's own on its codes (those worth one, or all of them if the model worried about a
        // code), the model's for the rest, except where it ringed a code the phone has ringed already.
        val flagged = worried && v != null && ("qr" in v.kinds || v.marks.any { it.kind == "qr" })
        val ours = codes.filter { (_, j) -> j.level != Level.OK || flagged }.map { (found, j) ->
            found.mark to getString(if (j.level == Level.SCAM) R.string.qr_label_trap else R.string.picture_label_qr)
        }
        val theirs = v?.marks.orEmpty().filter { m -> m.kind != "qr" || codes.none { it.first.mark.overlaps(m) } }
            .map { it to getString(LABELS[it.kind] ?: R.string.picture_label_risk) }
        val rings = ours + theirs
        if (rings.isNotEmpty() && (overall == Level.SCAM || overall == Level.DOUBT)) parts += getString(R.string.picture_marked)
        parts += getString(when {
            v == null -> if (codes.isEmpty()) R.string.picture_failed else R.string.picture_only_qr
            overall == Level.SCAM -> R.string.picture_check_yourself
            overall == Level.DOUBT -> R.string.picture_ask_family
            else -> R.string.picture_ok
        })
        val text = parts.joinToString(" ")
        val mood = when {
            overall == Level.SCAM -> Mood.PROTECTIVE
            overall == Level.DOUBT -> Mood.WORRIED
            seen == Level.OK -> Mood.HAPPY
            else -> Mood.CALM
        }
        picture.show(shown, rings)
        face.show(mood)
        said.text = text
        mine = text
        PeriApp.hub(this).say(text)
    }

    /** What a QR code would do, in Peri's words: never its address or its number. */
    private fun qrSentence(j: QrJudgement): String {
        val risks = j.risks
        val scam = j.level == Level.SCAM
        return when {
            QrRisk.RUNS_CODE in risks -> getString(R.string.qr_run_code)
            QrRisk.REMOTE_APP in risks -> getString(R.string.qr_remote_app)
            QrRisk.INSTRUCTIONS in risks -> getString(R.string.qr_instructions)
            QrRisk.PREMIUM_NUMBER in risks -> getString(R.string.qr_premium)
            QrRisk.FAKE_CONTACT in risks -> getString(R.string.qr_fake_contact)
            scam && j.brand != null -> getString(R.string.qr_website_scam, j.brand)
            scam && (j.action == QrAction.WEBSITE || QrRisk.LOOKALIKE in risks) -> getString(R.string.qr_website_disguised)
            scam -> getString(R.string.qr_hidden)
            else -> getString(when (j.action) {
                QrAction.WEBSITE -> if (risks.isEmpty()) R.string.qr_website_ok else R.string.qr_website_doubt
                QrAction.TEXT_MESSAGE -> R.string.qr_message
                QrAction.CALL -> R.string.qr_call
                QrAction.WIFI -> R.string.qr_wifi
                QrAction.CONTACT -> R.string.qr_contact
                QrAction.APP -> R.string.qr_app
                QrAction.EMAIL -> R.string.qr_email
                else -> if (QrRisk.SCAM_WORDS in risks) R.string.qr_text_scam else R.string.qr_text_ok
            })
        }
    }

    /** Two rings over the same spot. */
    private fun PictureMark.overlaps(o: PictureMark) = x1 < o.x2 && o.x1 < x2 && y1 < o.y2 && o.y1 < y2

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
        /** What Peri can read and the sharing app maybe can't: never taken as "a picture to look at". */
        val REFUSED = listOf("com.android.contacts", "contacts", "call_log", "sms", "mms", "mms-sms", "icc", "com.android.calendar", "app.friendlypear")

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
