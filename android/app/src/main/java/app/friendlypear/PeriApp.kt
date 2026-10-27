package app.friendlypear

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import app.friendlypear.services.PeriHub

class PeriApp : Application() {
    private val lazyHub = lazy { PeriHub(this) }
    val hub: PeriHub by lazyHub

    /** A new language (from Settings, or the phone's) reaches Peri's voice without restarting the app. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (lazyHub.isInitialized()) hub.languageChanged()
    }

    companion object {
        fun hub(context: Context): PeriHub = (context.applicationContext as PeriApp).hub
    }
}
