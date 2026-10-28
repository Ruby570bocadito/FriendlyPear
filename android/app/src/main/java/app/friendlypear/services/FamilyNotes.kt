package app.friendlypear.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import app.friendlypear.PeriApp
import app.friendlypear.R
import app.friendlypear.net.PearClient
import app.friendlypear.ui.peri.Palette
import app.friendlypear.ui.peri.PeriActivity

/**
 * The family's news on this phone with the app closed: a "was it you?" question answered from the notification itself,
 * a family member's Peri warning them, a stranger on their line (with "hang up their call"), the same trap reaching
 * several of you. Looked at every minute while Peri's process is up (her message listener keeps it), and every 15
 * minutes by the system otherwise ([FamilyJob]). Never while Peri's screen is open: there she says it herself. Each
 * question or alert once; nothing about a message or a call, only what the family's server already shows.
 */
class FamilyNotes(context: Context, private val pears: () -> PearClient, private val phoneOf: (String) -> String? = { null }) {
    private val context = context.applicationContext
    private val seen = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Off the main thread: two small requests, then a notification for anything new. */
    fun look() {
        val client = pears()
        if (!client.enabled) return
        client.openChecks()?.firstOrNull { it.kind == "new_number" }?.let { question(it) }
        client.family()?.let { family(it) }
    }

    private fun question(check: PearClient.Check) {
        if (!once("check:${check.id}")) return
        val from = check.from.ifEmpty { context.getString(R.string.check_someone) }
        notify(
            check.id.hashCode(),
            context.getString(R.string.check_question_title, from),
            context.getString(R.string.check_question_text, from),
            Palette.LILAC_INK,
            listOf(
                action(R.string.check_me, CHECK_ME, check.id, from),
                action(R.string.check_not_me, CHECK_NOT_ME, check.id, from),
            ),
        )
    }

    private fun family(family: PearClient.Family) {
        val campaign = family.campaign
        val pear = family.pears.firstOrNull { !it.self && it.state == "attention" }
        val key = when {
            campaign != null -> "campaign:$campaign"
            pear != null -> "pear:${pear.id}:${pear.kind}"
            else -> return seen.edit().remove(LAST_ALERT).apply()
        }
        if (seen.getString(LAST_ALERT, null) == key) return
        seen.edit().putString(LAST_ALERT, key).apply()
        when {
            campaign != null -> notify(
                ALERT_ID,
                context.getString(R.string.campaign_title),
                context.getString(R.string.campaign_says),
                Palette.CORAL_INK,
            )
            pear!!.kind == "breach" -> notify(
                ALERT_ID,
                context.getString(R.string.family_breach_title, pear.name),
                context.getString(R.string.family_breach_text),
                Palette.CORAL_INK,
                listOfNotNull(callAction(pear.name)),
            )
            pear.kind == "call" -> notify(
                ALERT_ID,
                context.getString(R.string.family_call_title, pear.name),
                context.getString(R.string.family_call_text),
                Palette.CORAL_INK,
                listOf(action(R.string.family_call_button, HANG_UP, pear.id, pear.name)),
            )
            else -> notify(
                ALERT_ID,
                context.getString(R.string.family_alert_title, pear.name),
                context.getString(R.string.family_alert_text),
                Palette.CORAL_INK,
                listOfNotNull(callAction(pear.name)),
            )
        }
    }

    /** False if it was already said: one notification per question or alert. */
    private fun once(key: String): Boolean {
        val keys = seen.getStringSet(QUESTIONS, emptySet()).orEmpty()
        if (key in keys) return false
        seen.edit().putStringSet(QUESTIONS, (keys.toList().takeLast(MAX_KEYS - 1) + key).toSet()).apply()
        return true
    }

    /** "Call" on a family member's alert, if their number is in the contacts: the phone's own dialer, ready to call. */
    private fun callAction(name: String): Notification.Action? {
        val phone = phoneOf(name) ?: return null
        val dial = Intent(Intent.ACTION_DIAL, android.net.Uri.fromParts("tel", phone, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pending = PendingIntent.getActivity(context, ("dial" + name).hashCode(), dial, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Action.Builder(null as Icon?, context.getString(R.string.family_notify_call), pending).build()
    }

    private fun action(label: Int, what: String, id: String, name: String): Notification.Action {
        val intent = Intent(context, FamilyActionReceiver::class.java).setAction(what).putExtra(EXTRA_ID, id).putExtra(EXTRA_NAME, name)
        val pending = PendingIntent.getBroadcast(context, (what + id).hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Action.Builder(null as Icon?, context.getString(label), pending).build()
    }

    fun notify(id: Int, title: String, text: String, colour: Int, actions: List<Notification.Action> = emptyList()) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.family_news_channel), NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, PeriActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_pear)
            .setColor(colour)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setGroup(GROUP)
            // On the lock screen, only that there's family news: names and answers once the phone is unlocked.
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(
                Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_pear).setColor(colour)
                    .setContentTitle(context.getString(R.string.family_notify_public)).build(),
            )
        actions.forEach { builder.addAction(it) }
        runCatching { manager.notify(id, builder.build()) }
    }

    companion object {
        const val CHANNEL = "family_news"
        const val CHECK_ME = "app.friendlypear.family.CHECK_ME"
        const val CHECK_NOT_ME = "app.friendlypear.family.CHECK_NOT_ME"
        const val HANG_UP = "app.friendlypear.family.HANG_UP"
        const val EXTRA_ID = "id"
        const val EXTRA_NAME = "name"
        const val ALERT_ID = 4_201
        const val HUNG_UP_ID = 4_203
        private const val GROUP = "family"
        private const val PREFS = "family_notes"
        private const val QUESTIONS = "questions"
        private const val LAST_ALERT = "last_alert"
        private const val MAX_KEYS = 30
    }
}

/** A button on a family notification: answer "was it you?", or ask a family member's Peri to hang up. */
class FamilyActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(FamilyNotes.EXTRA_ID) ?: return
        val name = intent.getStringExtra(FamilyNotes.EXTRA_NAME).orEmpty()
        val hub = PeriApp.hub(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val pending = goAsync()
        when (intent.action) {
            FamilyNotes.CHECK_ME, FamilyNotes.CHECK_NOT_ME -> hub.answerCheck(id, intent.action == FamilyNotes.CHECK_ME) { sent ->
                manager?.cancel(id.hashCode())
                if (sent) hub.familyNotes.notify(id.hashCode(), context.getString(R.string.check_thanks, name), "", Palette.LILAC_INK)
                pending.finish()
            }
            FamilyNotes.HANG_UP -> hub.requestHangUp(id) { sent ->
                val said = context.getString(if (sent) R.string.family_call_sent else R.string.family_call_failed, name)
                hub.familyNotes.notify(FamilyNotes.ALERT_ID, said, "", Palette.CORAL_INK)
                pending.finish()
            }
            else -> pending.finish()
        }
    }
}

/** Every 15 minutes, from the system: the family's news even when Peri's process isn't running. */
class FamilyJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val hub = PeriApp.hub(this)
        // The numbers the family blocked, kept fresh even when the app stays closed.
        hub.syncBlocks()
        if (hub.appVisible) return false
        Thread {
            runCatching { hub.familyNotes.look() }
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val ID = 4_202

        fun schedule(context: Context) {
            val jobs = context.getSystemService(JobScheduler::class.java) ?: return
            if (jobs.getPendingJob(ID) != null) return
            jobs.schedule(
                JobInfo.Builder(ID, ComponentName(context, FamilyJob::class.java))
                    .setPeriodic(15 * 60_000L)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build(),
            )
        }
    }
}
