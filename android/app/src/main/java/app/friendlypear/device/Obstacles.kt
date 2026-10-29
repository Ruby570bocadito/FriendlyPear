package app.friendlypear.device

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * What Android puts between Peri and the person on a real phone, found the same way on the home screen and in the
 * checkup: battery saving that puts her to sleep, and the codes Android 15 hides from apps that read notifications.
 * Restricted settings are UnlockActivity's; drawing over other apps and the call filter role are one call each, made
 * where they're needed.
 */
object Obstacles {
    /** Android lets Peri run in the background: battery saving won't put her to sleep, and the family's alerts arrive on time. */
    fun runsFreely(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName) && !restricted(context)
    }

    /**
     * Where the person lets her: Android's own one-tap question, or the app's page if they restricted her battery
     * themselves (the question can't undo that). Not a store app, and a scam guard that sleeps protects nobody.
     */
    @SuppressLint("BatteryLife")
    fun freeIntent(context: Context): Intent =
        if (restricted(context)) appInfo(context)
        else Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))

    /** The app's page in Android's settings: permissions, battery, restricted settings. */
    fun appInfo(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /**
     * From Android 15, codes in notifications are hidden from apps like Peri. Only with texts allowed does she see a
     * code arrive during a call, the moment a scammer asks for it.
     */
    fun codesHidden(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED

    private fun restricted(context: Context): Boolean =
        context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
}
