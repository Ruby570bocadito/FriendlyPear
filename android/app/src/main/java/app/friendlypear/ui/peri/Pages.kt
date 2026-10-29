package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.icu.text.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import app.friendlypear.R
import app.friendlypear.core.agent.Mood
import app.friendlypear.net.PearClient
import app.friendlypear.config.Diary
import app.friendlypear.config.FamilyStore
import app.friendlypear.config.PhoneContacts
import app.friendlypear.config.Shields
import app.friendlypear.config.UseLog
import app.friendlypear.services.PeriHub
import java.util.Calendar
import java.util.concurrent.Executors

/*
 * The sections beside Peri's own on her screen: her diary, learning with her, and the family. Each one is a
 * scroll of rubber cards over Peri's world, opened with a hero card in the section's colour and one of Peri's
 * poses; titles never sit right on the sky, so they read the same by day and by night.
 */

/**
 * A section's first card: its title, a line of Peri's from [bank] and one of her poses, in the section's colour.
 * Peri breathes there; a tap makes her hop and say another line.
 */
internal fun Context.heroCard(title: String, bank: Int, pose: Int, face: Int, lip: Int, ink: Int, decor: Floaties.Decor? = null): View = FrameLayout(this).apply {
    clipChildren = false
    clipToPadding = false
    val line = body(Lines.next(this@heroCard, bank), 17f).apply {
        setTextColor(Palette.INK)
        setPadding(0, dp(6), 0, 0)
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    val text = LinearLayout(this@heroCard).apply {
        orientation = LinearLayout.VERTICAL
        background = tintedCard(face, lip)
        setPadding(dp(20), dp(18), dp(124), dp(22))
        minimumHeight = dp(128)
        addView(heading(title).apply { setTextColor(ink) })
        addView(line)
    }
    addView(text, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
    // Hearts, stars or sparkles drift up around her, over the card but never over its words.
    decor?.let { addView(Floaties(this@heroCard, it), FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)) }
    // Peri peeks over the top edge of her card.
    addView(ImageView(this@heroCard).apply {
        setImageDrawable(sized(pose, dp(124)))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        popIn(160)
        alive {
            val next = Lines.next(this@heroCard, bank)
            if (!animationsOn()) line.text = next
            else line.animate().alpha(0f).setDuration(120).withEndAction {
                line.text = next
                line.animate().alpha(1f).setDuration(220).start()
            }.start()
        }
    }, FrameLayout.LayoutParams(dp(124), dp(124), Gravity.END or Gravity.TOP).apply { marginEnd = dp(2) })
}

/** A small title for a group of cards, on a white pill so it reads on any sky. */
internal fun Context.label(value: String): View = TextView(this).apply {
    text = value
    sizeSp(17f)
    typeface = Fonts.display(600)
    setTextColor(Palette.INK)
    background = rounded(Palette.WHITE, dp(18).toFloat())
    setPadding(dp(14), dp(6), dp(14), dp(7))
}

internal fun Context.whiteCard(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    background = cardBackground()
    setPadding(dp(18), dp(16), dp(18), dp(20))
}

/** One of Peri's illustrations decoded at the size it's shown, not its full 1024 px. */
internal fun Context.sized(res: Int, px: Int): Drawable = runCatching {
    ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, res)) { decoder, info, _ ->
        if (info.size.width > px) decoder.setTargetSize(px, px * info.size.height / info.size.width)
    }
}.getOrElse { getDrawable(res)!! }

/** A small rubber button with a word and no icon, for narrow places (a pear's card). */
internal fun Context.chipButton(label: String, onClick: () -> Unit): View = TextView(this).apply {
    text = label
    sizeSp(15f)
    typeface = Fonts.display(600)
    setTextColor(Palette.GREEN_DARK)
    gravity = Gravity.CENTER
    isSingleLine = true
    background = RubberDrawable(Palette.GREEN_TINT, 0xFFB7D798.toInt(), dp(3).toFloat(), dp(23).toFloat())
    setPadding(dp(6), 0, dp(6), dp(3))
    isClickable = true
    isFocusable = true
    stateListAnimator = pressScale()
    setOnClickListener { onClick() }
}

/** A round, tinted badge with an icon, for the rows of a list. */
internal fun Context.badge(icon: Int, face: Int, ink: Int, size: Int = 40): View = FrameLayout(this).apply {
    background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(face)
    }
    addView(ImageView(this@badge).apply {
        setImageResource(icon)
        setColorFilter(ink)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }, FrameLayout.LayoutParams(dp(size * 0.55f), dp(size * 0.55f), Gravity.CENTER))
}

/** Something that happened, for a timeline: when, its badge's icon and colours, and what, in Peri's words. */
internal class Moment(val at: Long, val icon: Int, val face: Int, val ink: Int, val text: String)

internal const val DAY_MS = 24 * 60 * 60 * 1000L

internal fun dayKey(at: Long): Int = Calendar.getInstance().apply { timeInMillis = at }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }

/** "Today", "Yesterday", or the day's name and date. */
internal fun Context.dayName(at: Long): String {
    val today = dayKey(System.currentTimeMillis())
    val yesterday = dayKey(System.currentTimeMillis() - DAY_MS)
    return when (dayKey(at)) {
        today -> getString(R.string.diary_today)
        yesterday -> getString(R.string.diary_yesterday)
        else -> DateFormat.getInstanceForSkeleton("EEEEdMMMM").format(at).replaceFirstChar { it.uppercase() }
    }
}

/**
 * [moments], newest first, as a timeline by day: a heading in [dayInk] for each day, then each moment's badge on a
 * line that joins it to the day's others, what happened across the whole width, and the time under it.
 */
internal fun Context.timeline(into: LinearLayout, moments: List<Moment>, dayInk: Int) {
    var day = -1
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)
    moments.forEachIndexed { i, m ->
        val key = dayKey(m.at)
        if (key != day) {
            day = key
            into.addView(heading(dayName(m.at), 18f).apply {
                setTextColor(dayInk)
                setPadding(0, if (into.childCount == 0) 0 else dp(12), 0, dp(8))
            })
        }
        val first = i == 0 || dayKey(moments[i - 1].at) != key
        val last = i == moments.lastIndex || dayKey(moments[i + 1].at) != key
        into.addView(moment(m, time.format(m.at), first, last))
    }
}

private fun Context.moment(m: Moment, at: String, first: Boolean, last: Boolean): View {
    val badge = dp(40)
    val centre = dp(4) + badge / 2
    return LinearLayout(this).apply {
        addView(FrameLayout(this@moment).apply {
            if (!(first && last)) addView(View(this@moment).apply { setBackgroundColor(Palette.LINE_STRONG) }, FrameLayout.LayoutParams(
                dp(3),
                if (last) centre else FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_HORIZONTAL or Gravity.TOP,
            ).apply { if (first) topMargin = centre })
            addView(badge(m.icon, m.face, m.ink), FrameLayout.LayoutParams(badge, badge, Gravity.CENTER_HORIZONTAL or Gravity.TOP).apply { topMargin = dp(4) })
        }, LinearLayout.LayoutParams(badge, LinearLayout.LayoutParams.MATCH_PARENT))
        addView(LinearLayout(this@moment).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(6), 0, dp(16))
            addView(body(m.text, 18f).apply { setTextColor(Palette.INK) })
            addView(body(at, 15f).apply { setPadding(0, dp(3), 0, 0) })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }
}

/**
 * A row you can tap in a list of tools: a round tinted badge with its icon, a title and perhaps a line under it, and
 * a chevron. The whole row is one button, read by TalkBack as one.
 */
internal fun Context.actionRow(icon: Int, title: String, line: String? = null, face: Int = Palette.GREEN_TINT, ink: Int = Palette.GREEN_DARK, onClick: () -> Unit): View =
    LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(64)
        setPadding(dp(4), dp(8), dp(4), dp(8))
        background = pressable(rounded(Palette.WHITE, dp(16).toFloat()), Palette.RIPPLE_DARK)
        isClickable = true
        isFocusable = true
        contentDescription = if (line != null) "$title. $line" else title
        setOnClickListener { onClick() }
        addView(ImageView(this@actionRow).apply {
            setImageResource(icon)
            setColorFilter(ink)
            background = rounded(face, dp(20).toFloat())
            setPadding(dp(9), dp(9), dp(9), dp(9))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(40), dp(40)))
        addView(LinearLayout(this@actionRow).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            addView(heading(title, TextSize.BODY))
            if (line != null) addView(body(line, TextSize.SMALL).apply { setTextColor(Palette.INK_SOFT) })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(ImageView(this@actionRow).apply {
            setImageResource(R.drawable.ic_chevron)
            setColorFilter(Palette.INK_SOFT)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
    }

/** [rows] one under another on a white card, under a [title] if there is one. */
internal fun Context.actionCard(rows: List<View>, title: String? = null): LinearLayout = whiteCard().apply {
    if (title != null) addView(heading(title, TextSize.BODY).apply { setPadding(dp(4), 0, 0, dp(6)) })
    rows.forEachIndexed { i, row ->
        addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { if (i > 0) topMargin = dp(4) })
    }
}

private fun Context.below(top: Int = 14) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }

/** A section's scroll, padded by Peri's screen to clear the bars. */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal abstract class Page(context: Context) : ScrollView(context) {
    protected val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
    }

    /** Where the status bar ends: cards scrolling up stop there instead of running under the clock. */
    var clipTop = 0
        set(value) {
            field = value
            invalidate()
        }

    init {
        clipToPadding = false
        isVerticalScrollBarEnabled = false
        addView(list)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val saved = canvas.save()
        if (clipTop > 0) canvas.clipRect(0, scrollY + clipTop, width, scrollY + height)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(saved)
    }

    /** Builds the section again from what's on the phone now, its pieces arriving one after another. */
    fun refresh() {
        list.removeAllViews()
        build()
        for (i in 0 until list.childCount) list.getChildAt(i).floatIn(40L + 60L * i)
    }

    protected abstract fun build()
}

/**
 * "Peri's diary": what she did for the person, newest first and by day: the scams she stopped, the doubtful messages
 * she checked, the calls and texts she made for them, the pictures they looked at, the games of "Scam or not?".
 * Above, three totals. All of it comes from this phone and stays on it; never a message's words.
 */
@SuppressLint("ViewConstructor")
internal class DiaryPage(context: Context, private val hub: PeriHub) : Page(context) {
    override fun build() {
        val c = context
        list.addView(c.heroCard(c.getString(R.string.diary_title), R.array.diary_hello, diaryArt(), Palette.SUN_TINT, Palette.SUN_LIP, Palette.AMBER_INK, Floaties.Decor.DIARY))
        list.addView(stats(), c.below())
        val lines = lines()
        if (lines.isEmpty()) {
            list.addView(c.whiteCard().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(ImageView(c).apply {
                    setImageDrawable(c.sized(R.drawable.peri_ui_empty_diary, c.dp(120)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    alive()
                }, LinearLayout.LayoutParams(c.dp(120), c.dp(120)))
                addView(c.body(c.getString(R.string.diary_empty)).apply {
                    setTextColor(Palette.INK)
                    gravity = Gravity.CENTER
                })
            }, c.below())
            return
        }
        days(lines)
    }

    /**
     * Short, however much there is: today in full (the first few, then "show more"), and each earlier day folded to a
     * line with its badges that opens on a tap; only the last week. All of it was one long list before.
     */
    private fun days(lines: List<Moment>) {
        val c = context
        val today = dayKey(System.currentTimeMillis())
        lines.groupBy { dayKey(it.at) }.entries.take(MAX_DAYS).forEach { (key, moments) ->
            val card = c.whiteCard()
            if (key == today) {
                c.timeline(card, moments.take(TODAY_LINES), Palette.AMBER_INK)
                if (moments.size > TODAY_LINES) {
                    card.addView(c.chipButton(c.resources.getQuantityString(R.plurals.diary_more, moments.size - TODAY_LINES, moments.size - TODAY_LINES)) {
                        card.removeAllViews()
                        c.timeline(card, moments, Palette.AMBER_INK)
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, c.dp(46)).apply { topMargin = c.dp(4) })
                }
            } else {
                card.addView(folded(moments) {
                    card.removeAllViews()
                    c.timeline(card, moments, Palette.AMBER_INK)
                })
            }
            list.addView(card, c.below())
        }
    }

    /** A past day in one line: its name, how many things happened, their badges, and a chevron; a tap opens it. */
    private fun folded(moments: List<Moment>, open: () -> Unit): View {
        val c = context
        val day = c.dayName(moments.first().at)
        val count = c.resources.getQuantityString(R.plurals.diary_day_count, moments.size, moments.size)
        return LinearLayout(c).apply {
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            contentDescription = "$day. $count"
            background = pressable(rounded(Palette.WHITE, c.dp(16).toFloat()), Palette.RIPPLE_DARK)
            setOnClickListener { open() }
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                addView(c.heading(day, 18f).apply { setTextColor(Palette.AMBER_INK) })
                addView(c.body(count, TextSize.SMALL).apply { setTextColor(Palette.INK_SOFT) })
                addView(LinearLayout(c).apply {
                    moments.distinctBy { it.icon to it.face }.take(FOLDED_BADGES).forEachIndexed { i, m ->
                        addView(c.badge(m.icon, m.face, m.ink, 30), LinearLayout.LayoutParams(c.dp(30), c.dp(30)).apply { if (i > 0) marginStart = c.dp(6) })
                    }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(8) })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(c).apply {
                setImageResource(R.drawable.ic_chevron)
                setColorFilter(Palette.INK_SOFT)
                rotation = 90f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(c.dp(24), c.dp(24)))
        }
    }

    /** The shields earned, the scams stopped and the days Peri and the person have been together. */
    private fun stats(): View {
        val c = context
        val days = runCatching {
            val first = c.packageManager.getPackageInfo(c.packageName, 0).firstInstallTime
            ((System.currentTimeMillis() - first) / DAY_MS + 1).toInt()
        }.getOrDefault(1)
        val shields = hub.shields.total
        val stopped = hub.shields.count(Shields.Kind.STOPPED)
        return LinearLayout(c).apply {
            listOf(
                Triple(shields, c.resources.getQuantityString(R.plurals.diary_stat_shields, shields), Rubber.SUN),
                Triple(stopped, c.resources.getQuantityString(R.plurals.diary_stat_scams, stopped), Rubber.CORAL),
                Triple(days, c.resources.getQuantityString(R.plurals.diary_stat_days, days), Rubber.GREEN),
            ).forEachIndexed { i, (n, word, rubber) ->
                addView(LinearLayout(c).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = c.tintedCard(rubber.face, rubber.lip)
                    setPadding(c.dp(6), c.dp(12), c.dp(6), c.dp(16))
                    contentDescription = "$n $word"
                    addView(TextView(c).apply {
                        text = java.text.NumberFormat.getIntegerInstance().format(n)
                        sizeSp(28f)
                        typeface = Fonts.display(700)
                        setTextColor(rubber.ink)
                        gravity = Gravity.CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    })
                    addView(TextView(c).apply {
                        text = word
                        sizeSp(14f)
                        typeface = Fonts.display(600)
                        setTextColor(rubber.ink)
                        gravity = Gravity.CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i > 0) marginStart = c.dp(8)
                })
            }
        }
    }

    /** Peri on her diary's card, by the hour: her morning tea, writing in her diary, reading under her lamp at night. */
    private fun diaryArt(): Int = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 6..11 -> R.drawable.peri_scene_morning
        in 12..20 -> R.drawable.peri_scene_diary
        else -> R.drawable.peri_scene_night
    }

    private fun lines(): List<Moment> {
        val c = context
        val diary = hub.diary.all().mapNotNull { e ->
            when (e.kind) {
                Diary.Kind.SCAM_STOPPED -> Moment(e.at, R.drawable.ic_shield, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(SCAMS[e.detail] ?: R.string.diary_scam_link))
                Diary.Kind.DOUBT_CHECKED -> Moment(e.at, R.drawable.ic_message, Palette.SUN_TINT, Palette.AMBER_INK, c.getString(R.string.diary_doubt))
                Diary.Kind.PAUSE_LEFT -> Moment(e.at, R.drawable.ic_bank, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_pause_left))
                Diary.Kind.FAMILY_CALLED -> Moment(e.at, R.drawable.ic_family, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_family_called))
                Diary.Kind.FAMILY_ASKED -> Moment(e.at, R.drawable.ic_family, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_family_asked))
                Diary.Kind.WEB_SILENCED -> Moment(e.at, R.drawable.ic_notifications, Palette.SKY_TINT, Palette.SKY_INK, c.getString(R.string.diary_web))
                Diary.Kind.QUIZ -> Moment(e.at, R.drawable.ic_learn, Palette.SKY_TINT, Palette.SKY_INK, c.getString(R.string.diary_quiz, e.detail))
                Diary.Kind.FAMILY_CHECKED -> Moment(e.at, R.drawable.ic_family, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_family_checked))
                Diary.Kind.BLOCKED_CALL -> Moment(e.at, R.drawable.ic_shield, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(if (e.detail == Diary.BY_FAMILY) R.string.diary_family_blocked_call else R.string.diary_blocked_call))
                Diary.Kind.BLOCKED_TEXT -> Moment(e.at, R.drawable.ic_shield, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(if (e.detail == Diary.BY_FAMILY) R.string.diary_family_blocked_text else R.string.diary_blocked_text))
                Diary.Kind.SCREEN_INTRUDER -> Moment(e.at, R.drawable.ic_shield, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(R.string.diary_intruder))
                Diary.Kind.LINE_ALERT -> Moment(e.at, R.drawable.ic_shield, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(if (e.detail == "new") R.string.diary_line_new else R.string.diary_line_lost))
            }
        }
        val uses = hub.uses.all().map { e ->
            val who = e.who ?: c.getString(R.string.use_someone)
            when (e.kind) {
                UseLog.Kind.CALL -> Moment(e.at, R.drawable.ic_call, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_call, who))
                UseLog.Kind.TEXT -> Moment(e.at, R.drawable.ic_message, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_text, who))
                UseLog.Kind.LOCATION -> Moment(e.at, R.drawable.ic_location, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_location))
                UseLog.Kind.PICTURE -> Moment(e.at, R.drawable.ic_image_search, Palette.SKY_TINT, Palette.SKY_INK, c.getString(R.string.diary_picture))
                UseLog.Kind.VOICE -> Moment(e.at, R.drawable.ic_mic, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_voice))
                UseLog.Kind.REPORT -> Moment(e.at, R.drawable.ic_message, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(R.string.diary_report))
                UseLog.Kind.HANG_UP -> Moment(e.at, R.drawable.ic_call, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(R.string.diary_hang_up))
            }
        }
        return (diary + uses).sortedByDescending { it.at }.take(MAX_LINES)
    }

    private companion object {
        const val MAX_LINES = 120
        /** Today's first moments in full; earlier days, a week of them, folded. */
        const val TODAY_LINES = 5
        const val MAX_DAYS = 7
        const val FOLDED_BADGES = 6
        val SCAMS = mapOf(
            "parcel" to R.string.diary_scam_parcel,
            "link" to R.string.diary_scam_link,
            "code" to R.string.diary_scam_code,
            "family" to R.string.diary_scam_family,
            "remote" to R.string.diary_scam_remote,
            "prize" to R.string.diary_scam_prize,
            "callback" to R.string.diary_scam_callback,
            "payment" to R.string.diary_scam_payment,
            "official" to R.string.diary_scam_official,
            "bill" to R.string.diary_scam_bill,
            "romance" to R.string.diary_scam_romance,
        )
    }
}

/**
 * "Learn with Peri": the game "Scam or not?", the traps Peri knows (each opens its three panels) and her golden
 * rules. Practising is the best shield, and it's fun.
 */
@SuppressLint("ViewConstructor")
internal class LearnPage(context: Context, private val hub: PeriHub, private val open: (Intent) -> Unit) : Page(context) {
    override fun build() {
        val c = context
        list.addView(c.heroCard(c.getString(R.string.learn_title), R.array.learn_hello, R.drawable.peri_scene_learn, Palette.SKY_TINT, 0xFFA9D5F2.toInt(), Palette.SKY_INK, Floaties.Decor.LEARN))
        list.addView(quiz(), c.below())
        list.addView(c.label(c.getString(R.string.learn_traps_title)), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(20) })
        list.addView(traps(), c.below(10))
        // Her tools: a checkup of the phone, asking her in writing, and what to do if one already got through.
        list.addView(c.label(c.getString(R.string.learn_tools_title)), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(20) })
        list.addView(tools(), c.below(10))
        list.addView(c.label(c.getString(R.string.learn_tips_title)), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(20) })
        list.addView(tips(), c.below(10))
    }

    private fun quiz(): View {
        val c = context
        val best = QuizActivity.best(c)
        return LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = c.tintedCard(Palette.SUN, Palette.SUN_LIP)
            setPadding(c.dp(20), c.dp(18), c.dp(20), c.dp(22))
            addView(LinearLayout(c).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(LinearLayout(c).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(c.heading(c.getString(R.string.quiz_title)))
                    addView(c.body(c.getString(R.string.quiz_card_text), 17f).apply {
                        setTextColor(Palette.INK)
                        setPadding(0, c.dp(4), 0, 0)
                    })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(ImageView(c).apply {
                    setImageDrawable(c.sized(R.drawable.peri_ui_tool_game, c.dp(96)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    alive()
                }, LinearLayout.LayoutParams(c.dp(96), c.dp(96)))
            })
            if (best != null) addView(c.body(c.getString(R.string.quiz_best, best.first, best.second), 16f).apply {
                setTextColor(Palette.INK)
                typeface = Fonts.display(600)
                setPadding(0, c.dp(6), 0, 0)
            })
            addView(Pill(c, c.getString(R.string.quiz_play), R.drawable.ic_learn, primary = true).apply {
                setOnClickListener { open(Intent(c, QuizActivity::class.java)) }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, c.dp(64)).apply { topMargin = c.dp(14) })
        }
    }

    /** The six kinds of trap, two by two: the trick's panel and its name. */
    private fun traps(): View {
        val c = context
        val side = (resources.displayMetrics.widthPixels - c.dp(52)) / 2
        return GridLayout(c).apply {
            columnCount = 2
            TRAPS.forEachIndexed { i, (kind, name, picture) ->
                addView(LinearLayout(c).apply {
                    orientation = LinearLayout.VERTICAL
                    background = c.cardBackground()
                    setPadding(c.dp(10), c.dp(10), c.dp(10), c.dp(14))
                    isClickable = true
                    isFocusable = true
                    stateListAnimator = pressScale()
                    contentDescription = c.getString(name)
                    setOnClickListener { open(Intent(c, TrapActivity::class.java).putExtra(TrapActivity.EXTRA_KIND, kind)) }
                    addView(ImageView(c).apply {
                        setImageDrawable(c.sized(picture, side))
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        background = rounded(Palette.CREAM, c.dp(18).toFloat())
                        clipToOutline = true
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, side - c.dp(20)))
                    addView(TextView(c).apply {
                        text = c.getString(name)
                        sizeSp(17f)
                        typeface = Fonts.display(600)
                        setTextColor(Palette.INK)
                        setPadding(c.dp(4), c.dp(8), c.dp(4), 0)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    })
                }, GridLayout.LayoutParams(GridLayout.spec(i / 2), GridLayout.spec(i % 2, 1f)).apply {
                    width = 0
                    setMargins(if (i % 2 == 0) 0 else c.dp(6), if (i < 2) 0 else c.dp(12), if (i % 2 == 0) c.dp(6) else 0, 0)
                })
            }
        }
    }

    /** One card, a row per tool: its icon in a soft circle, what it is, a line on what it does, and a chevron. */
    private fun tools(): View {
        val c = context
        return c.actionCard(listOf(
            c.actionRow(R.drawable.ic_shield, c.getString(R.string.checkup_open), c.getString(R.string.tool_checkup_line)) { open(Intent(c, CheckupActivity::class.java)) },
            c.actionRow(R.drawable.ic_message, c.getString(R.string.chat_title), c.getString(R.string.tool_chat_line)) { open(ChatActivity.intent(c)) },
            c.actionRow(R.drawable.ic_warning, c.getString(R.string.tool_after_title), c.getString(R.string.tool_after_line)) { open(AftercareActivity.intent(c, null)) },
        ))
    }

    private fun tips(): View {
        val c = context
        return c.whiteCard().apply {
            listOf(R.string.tip_1, R.string.tip_2, R.string.tip_3, R.string.tip_4).forEachIndexed { i, tip ->
                addView(LinearLayout(c).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, if (i == 0) 0 else c.dp(12), 0, 0)
                    addView(TextView(c).apply {
                        text = java.text.NumberFormat.getIntegerInstance().format(i + 1)
                        sizeSp(18f)
                        typeface = Fonts.display(700)
                        setTextColor(Palette.INK)
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Palette.SUN)
                        }
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(c.dp(36), c.dp(36)))
                    addView(c.body(c.getString(tip), 18f).apply {
                        setTextColor(Palette.INK)
                        setPadding(c.dp(12), 0, 0, 0)
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                })
            }
        }
    }

    private companion object {
        /** Each kind of trap, its name and its second panel (the trick itself). */
        val TRAPS = listOf(
            Triple("parcel", R.string.trap_name_parcel, R.drawable.peri_trap_parcel_2),
            Triple("link", R.string.trap_name_link, R.drawable.peri_trap_link_2),
            Triple("code", R.string.trap_name_code, R.drawable.peri_trap_code_2),
            Triple("family", R.string.trap_name_family, R.drawable.peri_trap_family_2),
            Triple("remote", R.string.trap_name_remote, R.drawable.peri_trap_remote_2),
            Triple("prize", R.string.trap_name_prize, R.drawable.peri_trap_prize_2),
            Triple("callback", R.string.trap_name_callback, R.drawable.peri_trap_callback_2),
            Triple("payment", R.string.trap_name_payment, R.drawable.peri_trap_payment_2),
            Triple("official", R.string.trap_name_official, R.drawable.peri_trap_official_2),
            Triple("bill", R.string.trap_name_bill, R.drawable.peri_trap_bill_2),
            Triple("romance", R.string.trap_name_romance, R.drawable.peri_trap_romance_2),
        )
    }
}

/**
 * "Your family": who looks after the person together with Peri, whether they agreed a family word, whether the
 * guardians see Peri's news and whether a change is waiting. Changing it opens the family setup, behind the phone's
 * own lock.
 */
@SuppressLint("ViewConstructor")
internal class FamilyPage(context: Context, private val hub: PeriHub, private val open: (Intent) -> Unit) : Page(context) {
    private var people: LinearLayout? = null

    override fun build() {
        val c = context
        val store = FamilyStore(c)
        val config = store.load()
        list.addView(c.heroCard(c.getString(R.string.family_page_title), R.array.family_hello, R.drawable.peri_scene_family, Palette.LILAC_TINT, 0xFFCDBEF5.toInt(), Palette.LILAC_INK, Floaties.Decor.FAMILY))
        list.addView(pears(), c.below())
        val guardians = c.whiteCard().apply {
            addView(c.heading(c.getString(R.string.family_guardians_title), TextSize.BODY))
        }
        val rows = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        people = rows
        guardians.addView(rows)
        list.addView(guardians, c.below())
        showGuardians(config.guardians)

        list.addView(c.whiteCard().apply {
            addView(state(config.hasFamilyWord, R.string.family_word_yes, R.string.family_word_no))
            addView(state(hub.news.sharing, R.string.family_news_on, R.string.family_news_off).apply { setPadding(0, c.dp(12), 0, 0) })
            if (store.loadPending() != null) addView(state(null, R.string.family_pending_short, R.string.family_pending_short).apply { setPadding(0, c.dp(12), 0, 0) })
        }, c.below())

        list.addView(Pill(c, c.getString(R.string.family_change), R.drawable.ic_family, primary = true).apply {
            setOnClickListener { open(Intent(c, FamilyActivity::class.java)) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, c.dp(68)).apply { topMargin = c.dp(18) })
    }

    /**
     * The family's pears, like friends: this phone's first (right away), then everyone else's from the family's
     * server, each with what their Peri wears, their role and whether they need attention.
     */
    private fun pears(): View {
        val c = context
        val box = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        box.addView(c.label(c.getString(R.string.family_pears_title)), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val grid = GridLayout(c).apply { columnCount = 2 }
        box.addView(grid, c.below(10))
        showPears(grid, emptyList())
        if (!hub.seesFamilyPears) {
            box.addView(c.whiteCard().apply {
                addView(c.body(c.getString(R.string.family_pears_pair), 18f).apply { setTextColor(Palette.INK) })
                addView(Pill(c, c.getString(R.string.family_pears_connect), R.drawable.ic_settings, primary = false).apply {
                    setOnClickListener { open(Intent(c, SettingsActivity::class.java)) }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, c.dp(58)).apply { topMargin = c.dp(12) })
            }, c.below(12))
            return box
        }
        // No connection, or nobody else yet: Peri with an unplugged cable, or with a heart and a phone.
        val noteArt = ImageView(c).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        val noteText = c.body("", 16f).apply { setPadding(c.dp(10), 0, 0, 0) }
        val note = LinearLayout(c).apply {
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            addView(noteArt, LinearLayout.LayoutParams(c.dp(88), c.dp(88)))
            addView(noteText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        box.addView(note, c.below(8))
        fun show(text: Int, art: Int) {
            noteText.text = c.getString(text)
            noteArt.setImageDrawable(c.sized(art, c.dp(88)))
            note.visibility = View.VISIBLE
        }
        hub.familyPears { family ->
            val pears = family?.pears ?: return@familyPears show(R.string.family_pears_error, R.drawable.peri_ui_offline)
            val others = pears.filter { !it.self }
            showPears(grid, others)
            if (others.isEmpty()) show(R.string.family_pears_alone, R.drawable.peri_ui_empty_family)
        }
        return box
    }

    private fun showPears(grid: GridLayout, others: List<PearClient.Pear>) {
        val c = context
        grid.removeAllViews()
        val mine = PearClient.Pear("self", hub.pearName, Outfit.load(c).code(), if (FamilyStore(c).load().guardians.isNotEmpty()) "protected" else "guardian", "calm", 0L, true)
        (listOf(mine) + others).forEachIndexed { i, pear ->
            grid.addView(pearCard(pear), GridLayout.LayoutParams(GridLayout.spec(i / 2), GridLayout.spec(i % 2, 1f)).apply {
                width = 0
                setMargins(if (i % 2 == 0) 0 else c.dp(6), if (i < 2) 0 else c.dp(12), if (i % 2 == 0) c.dp(6) else 0, 0)
            })
        }
    }

    /** One pear: their Peri in her outfit (worried, on a coral card, if they need attention), name, role and state. */
    private fun pearCard(pear: PearClient.Pear): View {
        val c = context
        val alert = !pear.self && pear.state == "attention"
        val name = pear.name.ifEmpty { c.getString(R.string.pear_no_name) }
        val role = c.getString(if (pear.role == "protected") R.string.pear_role_protected else R.string.pear_role_guardian)
        val state = c.getString(if (alert) R.string.pear_attention else R.string.pear_calm)
        return LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = if (alert) c.tintedCard(Palette.CORAL_TINT, 0xFFEFB4A8.toInt()) else c.cardBackground()
            setPadding(c.dp(8), c.dp(8), c.dp(8), c.dp(14))
            contentDescription = listOfNotNull(c.getString(R.string.pear_you).takeIf { pear.self }, name, role, state).joinToString(". ")
            addView(PeriFace(c).apply {
                outfit = Outfit.of(pear.outfit)
                show(if (alert) Mood.WORRIED else Mood.CALM)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, c.dp(124)))
            addView(c.heading(name, 18f).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            })
            addView(TextView(c).apply {
                text = if (pear.self) c.getString(R.string.pear_you) else role
                sizeSp(13f)
                typeface = Fonts.display(600)
                setTextColor(if (pear.self) Palette.GREEN_DARK else Palette.LILAC_INK)
                background = rounded(if (pear.self) Palette.GREEN_TINT else Palette.LILAC_TINT, c.dp(12).toFloat())
                setPadding(c.dp(10), c.dp(3), c.dp(10), c.dp(4))
                gravity = Gravity.CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(4) })
            addView(c.body(state, 15f).apply {
                gravity = Gravity.CENTER
                setTextColor(if (alert) Palette.CORAL_INK else Palette.INK_SOFT)
                if (alert) typeface = Fonts.reading(bold = true)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(4) })
            if (alert && pear.kind == "call") addView(c.chipButton(c.getString(R.string.family_call_button)) {
                hub.requestHangUp(pear.id) { sent -> hub.say(c.getString(if (sent) R.string.family_call_sent else R.string.family_call_failed, name)) }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, c.dp(46)).apply { topMargin = c.dp(10) })
            if (pear.self) addView(LinearLayout(c).apply {
                addView(c.chipButton(c.getString(R.string.pear_dress)) { open(Intent(c, WardrobeActivity::class.java)) }, LinearLayout.LayoutParams(0, c.dp(46), 1f).apply { marginEnd = c.dp(3) })
                addView(c.chipButton(c.getString(R.string.pear_rename)) { askName() }, LinearLayout.LayoutParams(0, c.dp(46), 1f).apply { marginStart = c.dp(3) })
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(10) })
            // A pear in trouble breathes gently, so the eye goes there; never flashing.
            if (alert && c.animationsOn()) {
                android.animation.ObjectAnimator.ofPropertyValuesHolder(
                    this,
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.03f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.03f),
                ).apply {
                    duration = 900
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    repeatMode = android.animation.ValueAnimator.REVERSE
                    start()
                }
            }
        }
    }

    /** "What does your family call you?": a few ideas to tap, or any name, as the family's pears show it. */
    private fun askName() {
        val c = context
        val field = android.widget.EditText(c).apply {
            setText(hub.pearName)
            hint = c.getString(R.string.pear_name_hint)
            sizeSp(TextSize.BODY)
            setTextColor(Palette.INK)
            isSingleLine = true
            filters = arrayOf(android.text.InputFilter.LengthFilter(24))
            background = rounded(Palette.WHITE, c.dp(16).toFloat(), Palette.LINE_STRONG, c.dp(1.5f))
            setPadding(c.dp(16), c.dp(14), c.dp(16), c.dp(14))
        }
        val ideas = LinearLayout(c).apply {
            c.resources.getStringArray(R.array.pear_name_ideas).forEach { idea ->
                addView(TextView(c).apply {
                    text = idea
                    sizeSp(16f)
                    typeface = Fonts.display(600)
                    setTextColor(Palette.LILAC_INK)
                    background = pressable(rounded(Palette.LILAC_TINT, c.dp(16).toFloat()), Palette.RIPPLE_DARK)
                    setPadding(c.dp(14), c.dp(8), c.dp(14), c.dp(9))
                    setOnClickListener { field.setText(idea) }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { marginEnd = c.dp(8) })
            }
        }
        val content = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(c.dp(22), c.dp(8), c.dp(22), 0)
            addView(field)
            addView(android.widget.HorizontalScrollView(c).apply {
                isHorizontalScrollBarEnabled = false
                addView(ideas)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(12) })
        }
        android.app.AlertDialog.Builder(c)
            .setTitle(R.string.pear_name_title)
            .setView(content)
            .setPositiveButton(R.string.pear_name_save) { _, _ ->
                if (field.text.toString().isNotBlank()) {
                    hub.pearName = field.text.toString()
                    refresh()
                }
            }
            .setNegativeButton(R.string.pear_name_cancel, null)
            .show()
            .friendly()
    }

    /** The guardians' names come from the phone's contacts, read away from the screen's thread. */
    private fun showGuardians(ids: Set<String>) {
        val rows = people ?: return
        val c = context
        if (ids.isEmpty()) {
            rows.addView(c.body(c.getString(R.string.family_no_guardians), 18f).apply { setPadding(0, c.dp(8), 0, 0) })
            return
        }
        LOADER.execute {
            val names = runCatching { PhoneContacts.load(c).filter { it.id in ids }.map { it.name } }.getOrDefault(emptyList())
            post {
                if (people !== rows) return@post
                val shown = names.ifEmpty { List(ids.size) { c.getString(R.string.use_someone) } }
                shown.forEach { name ->
                    rows.addView(LinearLayout(c).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, c.dp(10), 0, 0)
                        addView(TextView(c).apply {
                            text = name.trim().take(1).uppercase()
                            sizeSp(19f)
                            typeface = Fonts.display(700)
                            setTextColor(Palette.LILAC_INK)
                            gravity = Gravity.CENTER
                            background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(Palette.LILAC_TINT)
                            }
                            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                        }, LinearLayout.LayoutParams(c.dp(44), c.dp(44)))
                        addView(c.body(name, 19f).apply {
                            setTextColor(Palette.INK)
                            setPadding(c.dp(12), 0, 0, 0)
                        })
                    })
                }
            }
        }
    }

    /** A line with a check when [on], a gentle warning when not; null is neither (something waiting). */
    private fun state(on: Boolean?, yes: Int, no: Int): View {
        val c = context
        return LinearLayout(c).apply {
            gravity = Gravity.CENTER_VERTICAL
            val (icon, face, ink) = when (on) {
                true -> Triple(R.drawable.ic_check, Palette.GREEN_TINT, Palette.GREEN_DARK)
                false -> Triple(R.drawable.ic_warning, Palette.SUN_TINT, Palette.AMBER_INK)
                null -> Triple(R.drawable.ic_family, Palette.LILAC_TINT, Palette.LILAC_INK)
            }
            addView(c.badge(icon, face, ink, 36), LinearLayout.LayoutParams(c.dp(36), c.dp(36)))
            addView(c.body(c.getString(if (on != false) yes else no), 18f).apply {
                setTextColor(Palette.INK)
                setPadding(c.dp(12), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private companion object {
        val LOADER = Executors.newSingleThreadExecutor { r -> Thread(r, "family-page").apply { isDaemon = true } }
    }
}
