package app.friendlypear.device

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.provider.Settings

/**
 * Apps that can see and use everything on the screen: an accessibility service that isn't part of the phone or a
 * known reader for people with low vision. It's what fake technicians and banking trojans ask the person to switch
 * on ("give it permission so I can help you"). Read from the phone's own settings; nothing is sent.
 */
object ScreenAccess {
    /** Screen readers and switch access that people really use, from Google and Samsung. */
    private val TRUSTED = setOf(
        "com.google.android.marvin.talkback",
        "com.android.talkback",
        "com.samsung.android.accessibility.talkback",
        "com.samsung.accessibility",
        "com.google.android.apps.accessibility.voiceaccess",
        "com.google.android.accessibility.switchaccess",
        "com.google.android.accessibility.soundamplifier",
    )

    /** The apps that can control the screen right now, as (package, name the person sees). */
    fun intruders(context: Context): List<Pair<String, String>> {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return raw.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it.trim())?.packageName }
            .distinct()
            .filter { it != context.packageName && it !in TRUSTED && !partOfThePhone(context, it) }
            .map { it to label(context, it) }
    }

    private fun partOfThePhone(context: Context, pkg: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM != 0
    }.getOrDefault(false)

    private fun label(context: Context, pkg: String): String = runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
