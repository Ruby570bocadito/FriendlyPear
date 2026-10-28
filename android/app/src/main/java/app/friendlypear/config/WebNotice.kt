package app.friendlypear.config

import android.content.Context

/**
 * The last junk notice a website sent through the browser ("Your phone has a virus!"): which browser, which of
 * its notification channels (browsers give each site its own) and which site, so Peri can open the exact
 * switch to silence it. Kept on the phone for a few days, until the person silences or hides it.
 */
class WebNotice(context: Context) {
    data class Notice(val browser: String, val channel: String?, val site: String?, val at: Long)

    // The site, and the browser's channel for it (which names the site too), are sealed.
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).apply { sealOld(CHANNEL, SITE) }

    fun save(notice: Notice) {
        prefs.edit()
            .putString(BROWSER, notice.browser)
            .putString(CHANNEL, notice.channel?.let(Sealed::seal))
            .putString(SITE, notice.site?.let(Sealed::seal))
            .putLong(AT, notice.at)
            .putBoolean(HIDDEN, false)
            .apply()
    }

    /** The website to offer silencing: recent, and not hidden or handled yet. */
    fun offer(now: Long = System.currentTimeMillis()): Notice? {
        val browser = prefs.getString(BROWSER, null) ?: return null
        val at = prefs.getLong(AT, 0)
        if (prefs.getBoolean(HIDDEN, false) || now - at > KEEP_MS) return null
        return Notice(browser, prefs.getSealed(CHANNEL), prefs.getSealed(SITE), at)
    }

    fun hide() {
        prefs.edit().putBoolean(HIDDEN, true).apply()
    }

    private companion object {
        const val PREFS = "web_notice"
        const val BROWSER = "browser"
        const val CHANNEL = "channel"
        const val SITE = "site"
        const val AT = "at"
        const val HIDDEN = "hidden"
        const val KEEP_MS = 3 * 24 * 60 * 60_000L
    }
}
