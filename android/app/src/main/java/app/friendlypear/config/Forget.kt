package app.friendlypear.config

import android.content.Context
import app.friendlypear.net.DeviceKey

/**
 * "Forget what Peri knows" (Settings, behind the phone's lock): her diary, the last scam and what she looked into, the
 * log of what she did with each permission, the websites she silenced, the shields and badges, the game's best score
 * and what was done after a scam. With no guardians, the family setup, the blocked numbers and this phone's key go too,
 * and the server forgets the phone. With guardians those stay: who protects the person is the guardians' decision, so
 * nobody can use this to take them away. Settings and her outfit always stay.
 */
object Forget {
    private val HISTORY = listOf("diary", "last_scam", "investigations", "uses", "web_notice", "shields", "milestones", "quiz", "aftercare")
    private val PROTECTION = listOf("family", "blocked", "family_blocks")

    /** Whether someone protects the person: then the family, the blocks and the server's link stay. */
    fun guarded(context: Context): Boolean = FamilyStore(context).load().guardians.isNotEmpty()

    /** Off the main thread (the server is told to forget the phone); written at once, before the app restarts. */
    fun everything(context: Context) {
        val guarded = guarded(context)
        if (!guarded) DeviceKey.unpair(context)
        (if (guarded) HISTORY else HISTORY + PROTECTION).forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
    }
}
