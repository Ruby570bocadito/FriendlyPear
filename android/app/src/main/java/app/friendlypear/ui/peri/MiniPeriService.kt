package app.friendlypear.ui.peri

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.config.Need
import app.friendlypear.core.agent.Mood
import app.friendlypear.device.VoiceInput
import app.friendlypear.services.PeriHub
import app.friendlypear.services.PeriView
import kotlin.math.abs

/**
 * Mini Peri: only the pear, small, floating over other apps, with a speech bubble for what she says.
 * Tap to talk, drag to move (she settles on the nearest side), long press to open the full app.
 * The app and Mini Peri are two modes: opening the app puts Mini Peri away.
 */
class MiniPeriService : Service(), PeriView {
    private lateinit var hub: PeriHub
    private lateinit var windows: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var face: PeriFace
    private lateinit var bubble: TextView
    private lateinit var voice: VoiceInput
    private lateinit var params: WindowManager.LayoutParams
    private val main = Handler(Looper.getMainLooper())
    private val hideBubble = Runnable { if (!hub.isWaitingForAnswer) bubble.visibility = View.GONE }
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
        if (::root.isInitialized) runCatching { windows.removeView(root) }
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
        bubble = TextView(this).apply {
            sizeSp(18f)
            typeface = weight(500)
            setTextColor(Palette.INK)
            setLineSpacing(0f, 1.1f)
            maxWidth = dp(250)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            elevation = dp(4).toFloat()
            visibility = View.GONE
            setOnClickListener { visibility = View.GONE }
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
            addView(bubble, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = -dp(6) })
            addView(face, LinearLayout.LayoutParams(dp(SIZE_DP), dp(SIZE_DP)))
        }
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or (if (onRight) Gravity.END else Gravity.START)
            x = 0
            y = prefs.getInt(KEY_Y, resources.displayMetrics.heightPixels / 3)
        }
        voice = VoiceInput(this, hub.lang, onText = { hub.heard(it) }, onListening = { on ->
            listening = on
            showFace(hub.mood)
        }, mayListen = { !hub.inCall }, ears = hub.ears, onMissed = { hub.missedVoice() })
        listenToTouches()
        windows.addView(root, params)
        appShown(hub.appVisible)
        showFace(hub.mood)
        if (hub.lastSaid.isNotEmpty()) said(hub.lastSaid)
    }

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
                    }
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY - dy.toInt()
                        windows.updateViewLayout(root, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    main.removeCallbacks(longPress)
                    if (dragging) settle() else if (!longPressed) face.performClick()
                }
                MotionEvent.ACTION_CANCEL -> main.removeCallbacks(longPress)
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

    /** She goes to the nearest side, out of the way, and remembers where she was. */
    private fun settle() {
        val faceCenter = params.x + face.left + face.width / 2
        onRight = faceCenter > resources.displayMetrics.widthPixels / 2
        params.gravity = Gravity.BOTTOM or (if (onRight) Gravity.END else Gravity.START)
        params.x = 0
        params.y = params.y.coerceIn(0, resources.displayMetrics.heightPixels - root.height)
        root.gravity = if (onRight) Gravity.END else Gravity.START
        shapeBubble()
        windows.updateViewLayout(root, params)
        prefs.edit().putBoolean(KEY_RIGHT, onRight).putInt(KEY_Y, params.y).apply()
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
        bubble.text = text
        bubble.visibility = View.VISIBLE
        main.removeCallbacks(hideBubble)
        main.postDelayed(hideBubble, BUBBLE_MILLIS)
    }

    override fun mood(mood: Mood) = showFace(mood)

    override fun appShown(visible: Boolean) {
        root.visibility = if (visible) View.GONE else View.VISIBLE
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
        private const val SIZE_DP = 128
        private const val BUBBLE_MILLIS = 9_000L
        private const val LONG_PRESS_EXTRA = 300L
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
