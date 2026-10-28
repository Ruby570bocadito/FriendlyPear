package app.friendlypear

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import app.friendlypear.services.PeriHub

class PeriApp : Application() {
    private val lazyHub = lazy { PeriHub(this) }
    val hub: PeriHub by lazyHub

    override fun onCreate() {
        super.onCreate()
        app.friendlypear.ui.peri.Fonts.init(this)
        // The process starts with the phone (the message listener), so the daily check is set up again after a reboot.
        app.friendlypear.services.SummaryReceiver.schedule(this)
    }

    /** A new language (from Settings, or the phone's) reaches Peri's voice without restarting the app. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (lazyHub.isInitialized()) hub.languageChanged()
    }

    companion object {
        fun hub(context: Context): PeriHub = (context.applicationContext as PeriApp).hub
    }
}
