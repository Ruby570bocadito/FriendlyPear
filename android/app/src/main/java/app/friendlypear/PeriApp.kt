package app.friendlypear

import android.app.Application
import android.content.Context
import app.friendlypear.services.PeriHub

class PeriApp : Application() {
    val hub: PeriHub by lazy { PeriHub(this) }

    companion object {
        fun hub(context: Context): PeriHub = (context.applicationContext as PeriApp).hub
    }
}
