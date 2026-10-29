package app.friendlypear.ui.peri

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.config.Need
import app.friendlypear.core.agent.Mood
import app.friendlypear.device.VoiceInput
import app.friendlypear.services.PeriHub
import app.friendlypear.services.PeriMoment
import app.friendlypear.services.PeriView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Mini Peri: only the pear, small, floating over other apps, with a speech bubble for what she says.
 * Tap to talk, drag to move (she glides to the nearest side), drag onto the "×" at the bottom to put her away,
 * long press to open the full app. The app and Mini Peri are two modes: opening the app puts Mini Peri away.
 */
class MiniPeriService : Service(), PeriView {
    private lateinit var hub: PeriHub
    private lateinit var windows: WindowManager
    private lateinit var root: LinearLayout
    /** Peri and, behind her, the ring that pulses while she listens. */
    private lateinit var holder: FrameLayout
    private lateinit var ring: View
    private lateinit var face: PeriFace
    private lateinit var bubble: TextView
    /**
     * The bubble's own window, above hers and just its size: next to her nothing is covered, so what's underneath
     * stays usable (one window for both was as wide as a long sentence, and took the touches beside her).
     */
    private lateinit var bubbleRoot: FrameLayout
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var voice: VoiceInput
    private lateinit var params: WindowManager.LayoutParams
    /** The "drop here to put Peri away" target, in its own window, shown only while she is dragged. */
    private var target: LinearLayout? = null
    private var targetCircle: FrameLayout? = null
    private var targetCross: ImageView? = null
    private var targetLabel: TextView? = null
    private var overTarget = false
    private var snap: ValueAnimator? = null
    private var pulse: ValueAnimator? = null
    private val main = Handler(Looper.getMainLooper())
    private val hideBubble = Runnable { if (!hub.isWaitingForAnswer) fadeBubble() }
    private var listening = false
    /** Which side she sits on; the window is anchored there and to the bottom, so the bubble grows towards the middle and upwards. */
    private var onRight = true
    private val prefs by lazy { getSharedPreferences("mini_peri", MODE_PRIVATE) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        hub = PeriApp.hub(this)
        windows = getSystemService(WindowManager::class.java)
        startInForeground()
        if (!Settings.canDrawOverlays(this)) return stopSelf()
        buildWindow()
        hub.attach(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            // Only a Mini Peri that came out for the call goes away with it.
            ACTION_CALL_ENDED -> if (forCall) stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        forCall = false
        hub.detach(this)
        main.removeCallbacksAndMessages(null)
        snap?.cancel()
        pulse?.cancel()
        if (::root.isInitialized) runCatching { windows.removeView(root) }
        if (::bubbleRoot.isInitialized) runCatching { windows.removeView(bubbleRoot) }
        target?.let { runCatching { windows.removeView(it) } }
        if (::voice.isInitialized) voice.destroy()
        super.onDestroy()
    }

    /** The microphone type lets her listen while another app is in front; it's only used when tapped. */
    private fun startInForeground() {
        val channel = NotificationChannel(CHANNEL, getString(R.string.mini_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = PendingIntent.getActivity(this, 0, Intent(this, PeriActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, MiniPeriService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_pear)
            .setColor(Palette.GREEN_DARK)
            .setContentTitle(getString(R.string.mini_title))
            .setContentText(getString(R.string.mini_text))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.mini_hide), stop).build())
            .setOngoing(true)
            .build()
        // "Special use" exists from Android 14; before it, no type is needed without the microphone.
        val plain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        val type = if (hub.permissions.granted(Need.MIC)) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else plain
        // Started while another app is in front (a call), Android may refuse the microphone type: she still shows.
        try {
            startForeground(NOTIFICATION_ID, notification, type)
        } catch (e: SecurityException) {
            startForeground(NOTIFICATION_ID, notification, plain)
        }
    }

    private fun buildWindow() {
        // Only the pear: her illustrations are cut out, and she brings her own soft shadow.
        face = PeriFace(this)
        ring = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(RING_FILL)
                setStroke(dp(4), Palette.GREEN)
            }
            alpha = 0f
        }
        holder = FrameLayout(this).apply {
            clipChildren = false
            addView(ring, FrameLayout.LayoutParams(dp(RING_DP), dp(RING_DP), Gravity.CENTER))
            addView(face, FrameLayout.LayoutParams(dp(SIZE_DP), dp(SIZE_DP)))
            // She grows out of the ground and shrinks back into it.
            pivotX = dp(SIZE_DP) / 2f
            pivotY = dp(SIZE_DP).toFloat()
        }
        bubble = TextView(this).apply {
            sizeSp(BUBBLE_SP)
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.1f)
            maxWidth = bubbleWidth()
            minHeight = dp(44)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            elevation = dp(4).toFloat()
            visibility = View.GONE
        }
        onRight = prefs.getBoolean(KEY_RIGHT, true)
        shapeBubble()
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (onRight) Gravity.END else Gravity.START
            clipChildren = false
            clipToPadding = false
            // Room for the bubble's shadow inside the window.
            val edge = dp(10)
            setPadding(edge, edge, edge, edge)
            addView(holder, LinearLayout.LayoutParams(dp(SIZE_DP), dp(SIZE_DP)))
        }
        bubbleRoot = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            val edge = dp(10)
            setPadding(edge, edge, edge, edge)
            addView(bubble, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        }
        // Touchable, so Android keeps it fully opaque; a tap on it puts it away.
        bubbleParams = overlay(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        bubble.setOnClickListener { fadeBubble() }
        params = overlay().apply {
            gravity = Gravity.BOTTOM or (if (onRight) Gravity.END else Gravity.START)
            x = 0
            y = prefs.getInt(KEY_Y, resources.displayMetrics.heightPixels / 3)
        }
        voice = VoiceInput(this, hub.lang, onText = { hub.heard(it) }, onListening = { on ->
            if (on && !listening) hub.haptics.listening()
            listening = on
            showFace(hub.mood)
            pulseRing(on)
        }, mayListen = { !hub.inCall }, ears = hub.ears, onMissed = { hub.missedVoice() })
        listenToTouches()
        // The target's window goes in first, so Peri, dragged over it, stays in front.
        buildTarget()
        windows.addView(root, params)
        windows.addView(bubbleRoot, bubbleParams)
        placeBubble()
        appShown(hub.appVisible)
        showFace(hub.mood)
        if (hub.lastSaid.isNotEmpty()) said(hub.lastSaid)
    }

    private fun overlay(flags: Int = 0) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or flags,
        PixelFormat.TRANSLUCENT,
    )

    /** Wide enough for a sentence, never more than about two thirds of the screen. */
    private fun bubbleWidth() = minOf(dp(260), (resources.displayMetrics.widthPixels * 0.68f).toInt())

    private fun listenToTouches() {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        var longPressed = false
        val longPress = Runnable {
            longPressed = true
            openApp()
        }
        face.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // A glide still going ends where it was headed, so the numbers below start from a settled Peri.
                    snap?.cancel()
                    downX = e.rawX
                    downY = e.rawY
                    startX = if (onRight) resources.displayMetrics.widthPixels - root.width - params.x else params.x
                    startY = params.y
                    dragging = false
                    longPressed = false
                    main.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong() + LONG_PRESS_EXTRA)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                        main.removeCallbacks(longPress)
                        // While moving, measured from the left whatever side she was on.
                        params.gravity = Gravity.BOTTOM or Gravity.START
                        pickUp(true)
                    }
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY - dy.toInt()
                        windows.updateViewLayout(root, params)
                        placeBubble()
                        hover(e.rawX, e.rawY)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    main.removeCallbacks(longPress)
                    if (dragging) drop() else if (!longPressed) face.performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    main.removeCallbacks(longPress)
                    if (dragging) drop()
                }
            }
            true
        }
        face.setOnClickListener { talk() }
        // For TalkBack: double tap talks, and "open the app" is in the actions menu.
        face.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, getString(R.string.mini_talk)))
                info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, getString(R.string.mini_open)))
            }

            override fun performAccessibilityAction(host: View, action: Int, args: android.os.Bundle?): Boolean {
                if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) return true.also { openApp() }
                return super.performAccessibilityAction(host, action, args)
            }
        }
    }

    /** Lifted a little while dragged, with the target to put her away showing at the bottom. */
    private fun pickUp(on: Boolean) {
        val s = if (on) 1.08f else 1f
        holder.animate().scaleX(s).scaleY(s).setInterpolator(DecelerateInterpolator()).setDuration(140).start()
        showTarget(on)
    }

    private fun drop() {
        // Read before the target goes, which forgets it was hovered.
        val over = overTarget
        pickUp(false)
        if (over) putAway() else settle()
    }

    /** She glides to the nearest side, out of the way, and remembers where she was. */
    private fun settle() {
        val screenW = resources.displayMetrics.widthPixels
        val faceCenter = params.x + holder.left + holder.width / 2
        onRight = faceCenter > screenW / 2
        val toX = if (onRight) screenW - root.width else 0
        params.y = params.y.coerceIn(0, resources.displayMetrics.heightPixels - root.height)
        snap = ValueAnimator.ofInt(params.x, toX).apply {
            duration = SNAP_MILLIS
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                if (root.isAttachedToWindow) windows.updateViewLayout(root, params)
                placeBubble()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    snap = null
                    anchor()
                }
            })
            start()
        }
    }

    /** Anchored to her side again, so a bubble grows towards the middle of the screen. */
    private fun anchor() {
        if (!root.isAttachedToWindow) return
        params.gravity = Gravity.BOTTOM or (if (onRight) Gravity.END else Gravity.START)
        params.x = 0
        root.gravity = if (onRight) Gravity.END else Gravity.START
        shapeBubble()
        windows.updateViewLayout(root, params)
        placeBubble()
        prefs.edit().putBoolean(KEY_RIGHT, onRight).putInt(KEY_Y, params.y).apply()
    }

    /** Her bubble's window sits right above her, on her side, its tail on her. */
    private fun placeBubble() {
        if (!::bubbleRoot.isInitialized || !bubbleRoot.isAttachedToWindow) return
        bubbleParams.gravity = params.gravity
        bubbleParams.x = params.x
        bubbleParams.y = params.y + dp(SIZE_DP) + dp(10) * 2 - dp(16)
        bubbleRoot.foregroundGravity = if (onRight) Gravity.END else Gravity.START
        (bubble.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.gravity = if (onRight) Gravity.END else Gravity.START
            bubble.layoutParams = it
        }
        windows.updateViewLayout(bubbleRoot, bubbleParams)
    }

    /** Dropped on the "×": she shrinks into the ground and Mini Peri stops, until it's turned on again. */
    private fun putAway() {
        overTarget = false
        holder.animate().cancel()
        holder.animate().scaleX(0f).scaleY(0f).alpha(0f).setInterpolator(AccelerateInterpolator()).setDuration(220).withEndAction {
            Toast.makeText(this, R.string.mini_put_away, Toast.LENGTH_LONG).show()
            stopSelf()
        }.start()
    }

    private fun showTarget(on: Boolean) {
        val view = target ?: return
        overTarget = false
        targetCircle?.let { highlight(it, false) }
        view.animate().cancel()
        if (on) {
            view.visibility = View.VISIBLE
            view.translationY = dp(24).toFloat()
            view.animate().alpha(1f).translationY(0f).setDuration(180).start()
        } else {
            view.animate().alpha(0f).translationY(dp(24).toFloat()).setDuration(160).withEndAction { view.visibility = View.GONE }.start()
        }
    }

    /** A round "×" with a label, centred near the bottom: the same gesture as closing a chat bubble. */
    private fun buildTarget(): LinearLayout {
        // Light, like Android's own bubbles: a white disc with a soft "×", over a gentle shade at the bottom.
        val cross = ImageView(this).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Palette.INK_SOFT)
        }
        val circle = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(TARGET_FILL)
            }
            elevation = dp(4).toFloat()
            addView(cross, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        }
        val label = TextView(this).apply {
            text = getString(R.string.mini_drop_here)
            sizeSp(TextSize.SMALL)
            typeface = weight(600)
            setTextColor(Palette.WHITE)
            setShadowLayer(dp(4).toFloat(), 0f, dp(1).toFloat(), SHADE_TEXT)
            gravity = Gravity.CENTER
        }
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(SHADE, 0x00000000))
            setPadding(dp(16), dp(48), dp(16), dp(40))
            // The disc grows when Peri is over it: it may reach into the padding without being cut flat.
            clipToPadding = false
            alpha = 0f
            visibility = View.GONE
            addView(circle, LinearLayout.LayoutParams(dp(TARGET_DP), dp(TARGET_DP)).apply { bottomMargin = dp(10) })
            addView(label, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        // As wide as the screen, down to the very bottom; it can never be touched, and it's hidden unless dragging.
        val p = overlay(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS).apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 0
            // Android lets touches through other apps' windows only when they're this see-through or more.
            alpha = 0.8f
        }
        windows.addView(view, p)
        target = view
        targetCircle = circle
        targetCross = cross
        targetLabel = label
        return view
    }

    /** Close to the "×", it grows and turns red, with a little buzz, so it's clear letting go puts her away. */
    private fun hover(x: Float, y: Float) {
        val circle = targetCircle ?: return
        if (!circle.isAttachedToWindow || circle.width == 0) return
        val at = IntArray(2).also { circle.getLocationOnScreen(it) }
        val near = hypot(x - (at[0] + circle.width / 2f), y - (at[1] + circle.height / 2f)) < dp(HIT_DP)
        if (near == overTarget) return
        overTarget = near
        highlight(circle, near)
        // She shrinks a little over it, as if about to go in.
        val s = if (near) 0.8f else 1.08f
        holder.animate().scaleX(s).scaleY(s).setInterpolator(DecelerateInterpolator()).setDuration(120).start()
        if (near) circle.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun highlight(circle: FrameLayout, on: Boolean) {
        (circle.background as? GradientDrawable)?.setColor(if (on) Palette.DANGER else TARGET_FILL)
        targetCross?.setColorFilter(if (on) Palette.WHITE else Palette.INK_SOFT)
        targetLabel?.text = getString(if (on) R.string.mini_let_go else R.string.mini_drop_here)
        val s = if (on) 1.2f else 1f
        circle.animate().scaleX(s).scaleY(s).setInterpolator(OvershootInterpolator(2f)).setDuration(160).start()
    }

    /** She pops up, with a little bounce, whenever she (re)appears. */
    private fun popIn() {
        holder.animate().cancel()
        holder.scaleX = 0.3f
        holder.scaleY = 0.3f
        holder.alpha = 0f
        holder.animate().scaleX(1f).scaleY(1f).alpha(1f).setInterpolator(OvershootInterpolator(2.2f)).setDuration(POP_MILLIS).start()
    }

    /** While she listens, a soft green ring breathes out from behind her, like ripples. */
    private fun pulseRing(on: Boolean) {
        pulse?.cancel()
        pulse = null
        if (!on) {
            ring.animate().alpha(0f).setDuration(150).start()
            return
        }
        ring.animate().cancel()
        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PULSE_MILLIS
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val f = it.animatedFraction
                ring.scaleX = 0.82f + 0.36f * f
                ring.scaleY = 0.82f + 0.36f * f
                ring.alpha = 0.9f * (1 - f)
            }
            start()
        }
    }

    /** The bubble's tail points down at Peri, on whichever side she sits. */
    private fun shapeBubble() {
        val tail = dp(12)
        bubble.background = BubbleDrawable(
            Palette.WHITE,
            Palette.GREEN,
            dp(2).toFloat(),
            dp(20).toFloat(),
            tail.toFloat(),
            if (onRight) BubbleDrawable.Tail.BOTTOM_END else BubbleDrawable.Tail.BOTTOM_START,
            tailInset = dp(SIZE_DP / 2).toFloat(),
        )
        bubble.setPadding(dp(16), dp(12), dp(16), dp(14) + tail)
        bubble.pivotX = if (onRight) bubbleWidth().toFloat() else 0f
    }

    /** A long sentence gets slightly smaller letters, so the bubble never covers more than about a third of the screen. */
    private fun fitBubble() {
        val most = View.MeasureSpec.makeMeasureSpec(bubbleWidth(), View.MeasureSpec.AT_MOST)
        val free = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val room = (resources.displayMetrics.heightPixels * 0.32f).toInt()
        var sp = BUBBLE_SP
        bubble.sizeSp(sp)
        bubble.measure(most, free)
        while (bubble.measuredHeight > room && sp > BUBBLE_MIN_SP) {
            sp -= 1f
            bubble.sizeSp(sp)
            bubble.measure(most, free)
        }
    }

    private fun fadeBubble() {
        if (bubble.visibility != View.VISIBLE) return
        bubble.animate().cancel()
        bubble.animate().alpha(0f).translationY(dp(8).toFloat()).setDuration(180).withEndAction {
            bubble.visibility = View.GONE
            bubble.translationY = 0f
            // Nothing left of its window either.
            bubbleRoot.visibility = View.GONE
        }.start()
    }

    private fun talk() {
        if (!voice.available) return said(getString(R.string.no_recognizer))
        // Asking for the microphone needs the app's screen: open it there.
        if (!hub.permissions.granted(Need.MIC)) return openApp()
        if (!voice.listen()) hub.say(getString(R.string.no_mic_in_call))
    }

    private fun openApp() {
        startActivity(Intent(this, PeriActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun said(text: String) {
        // The app in front says it in its own bubble: hers, a window of its own, covered it with the same words.
        if (hub.appVisible) return
        bubbleRoot.visibility = View.VISIBLE
        bubble.text = text
        fitBubble()
        bubble.animate().cancel()
        if (bubble.visibility != View.VISIBLE || bubble.alpha < 1f) {
            // It rises out of Peri, from the tail's side.
            bubble.visibility = View.VISIBLE
            bubble.pivotY = bubble.measuredHeight.toFloat()
            bubble.alpha = 0f
            bubble.scaleX = 0.92f
            bubble.scaleY = 0.92f
            bubble.translationY = dp(10).toFloat()
            bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setInterpolator(DecelerateInterpolator()).setDuration(220).start()
        }
        main.removeCallbacks(hideBubble)
        main.postDelayed(hideBubble, BUBBLE_MILLIS)
    }

    override fun mood(mood: Mood) = showFace(mood)

    override fun moment(moment: PeriMoment) = face.moment(moment)

    override fun appShown(visible: Boolean) {
        val wasHidden = root.visibility != View.VISIBLE
        root.visibility = if (visible) View.GONE else View.VISIBLE
        if (visible && ::bubbleRoot.isInitialized) bubbleRoot.visibility = View.GONE
        if (!visible && (wasHidden || holder.alpha < 1f)) popIn()
    }

    override fun callStarted() {
        if (::voice.isInitialized) voice.stop()
    }

    override fun speaking(on: Boolean) {
        face.speaking(on)
        // Like the app: when Peri asked something, the microphone opens once she finishes speaking.
        if (!on && hub.isWaitingForAnswer && voice.available && hub.permissions.granted(Need.MIC)) voice.listen()
        if (!on) {
            main.removeCallbacks(hideBubble)
            main.postDelayed(hideBubble, BUBBLE_MILLIS)
        }
    }

    private fun showFace(mood: Mood) = face.show(if (listening) Mood.LISTENING else mood)

    companion object {
        @Volatile
        var running = false
            private set

        /** True when this Mini Peri came out only for a call, so she leaves when it ends. */
        @Volatile
        var forCall = false
            private set

        const val ACTION_STOP = "app.friendlypear.mini.STOP"
        private const val ACTION_CALL_ENDED = "app.friendlypear.mini.CALL_ENDED"
        private const val CHANNEL = "mini_peri"
        private const val NOTIFICATION_ID = 7
        private const val SIZE_DP = 100
        private const val RING_DP = 88
        private const val TARGET_DP = 60
        private const val TARGET_FILL = 0xF2FFFFFF.toInt()
        /** The shade behind the target: dark enough for white text on any wallpaper, never a wall. */
        private const val SHADE = 0x66000000
        private const val SHADE_TEXT = 0x99000000.toInt()
        /** How close (from the finger to the "×") counts as dropping her on it. */
        private const val HIT_DP = 72
        private const val RING_FILL = 0x267CC242
        private const val BUBBLE_SP = 18f
        private const val BUBBLE_MIN_SP = 14f
        private const val BUBBLE_MILLIS = 9_000L
        private const val LONG_PRESS_EXTRA = 300L
        private const val SNAP_MILLIS = 260L
        private const val POP_MILLIS = 420L
        private const val PULSE_MILLIS = 1_300L
        private const val KEY_RIGHT = "right"
        private const val KEY_Y = "y"

        fun start(context: Context) = context.startForegroundService(Intent(context, MiniPeriService::class.java))

        fun stop(context: Context) = context.stopService(Intent(context, MiniPeriService::class.java))

        /** Beside the call screen. If Mini Peri is already out, she just stays (and stays after the call). */
        fun startForCall(context: Context) {
            if (running) return
            forCall = true
            try {
                start(context)
            } catch (e: IllegalStateException) {
                // Android didn't allow starting it from the background right now; Peri still speaks.
                forCall = false
            }
        }

        fun endCall(context: Context) {
            if (running && forCall) context.startService(Intent(context, MiniPeriService::class.java).setAction(ACTION_CALL_ENDED))
        }
    }
}
