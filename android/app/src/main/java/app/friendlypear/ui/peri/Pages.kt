package app.friendlypear.ui.peri

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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

/** A section's first card: its title, a line from Peri and one of her poses, in the section's colour. */
internal fun Context.heroCard(title: String, line: String, pose: Int, face: Int, lip: Int, ink: Int): View = FrameLayout(this).apply {
    clipChildren = false
    clipToPadding = false
    val text = LinearLayout(this@heroCard).apply {
        orientation = LinearLayout.VERTICAL
        background = tintedCard(face, lip)
        setPadding(dp(20), dp(18), dp(124), dp(22))
        minimumHeight = dp(128)
        addView(heading(title).apply { setTextColor(ink) })
        addView(body(line, 17f).apply {
            setTextColor(Palette.INK)
            setPadding(0, dp(6), 0, 0)
        })
    }
    addView(text, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
    // Peri peeks over the top edge of her card.
    addView(ImageView(this@heroCard).apply {
        setImageDrawable(sized(pose, dp(124)))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        popIn(160)
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

private fun Context.below(top: Int = 14) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }

/** A section's scroll, padded by Peri's screen to clear the bars. */
@SuppressLint("ViewConstructor") // Built in code only, never from XML.
internal abstract class Page(context: Context) : ScrollView(context) {
    protected val list = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
    }

    init {
        clipToPadding = false
        isVerticalScrollBarEnabled = false
        addView(list)
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
    private class Line(val at: Long, val icon: Int, val face: Int, val ink: Int, val text: String)

    override fun build() {
        val c = context
        list.addView(c.heroCard(c.getString(R.string.diary_title), Lines.next(c, R.array.diary_hello), R.drawable.peri_calm_glasses, Palette.SUN_TINT, Palette.SUN_LIP, Palette.AMBER_INK))
        list.addView(stats(), c.below())
        val lines = lines()
        if (lines.isEmpty()) {
            list.addView(c.whiteCard().apply {
                gravity = Gravity.CENTER_HORIZONTAL
                addView(ImageView(c).apply {
                    setImageDrawable(c.sized(R.drawable.peri_calm_thumbsup, c.dp(120)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(c.dp(120), c.dp(120)))
                addView(c.body(c.getString(R.string.diary_empty)).apply {
                    setTextColor(Palette.INK)
                    gravity = Gravity.CENTER
                })
            }, c.below())
            return
        }
        val card = c.whiteCard()
        var day = -1
        val time = DateFormat.getTimeInstance(DateFormat.SHORT)
        lines.forEach { line ->
            val key = dayKey(line.at)
            if (key != day) {
                day = key
                card.addView(c.heading(dayName(line.at), 18f).apply {
                    setTextColor(Palette.AMBER_INK)
                    setPadding(0, if (card.childCount == 0) 0 else c.dp(16), 0, c.dp(6))
                })
            }
            card.addView(LinearLayout(c).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, c.dp(7), 0, c.dp(7))
                addView(c.badge(line.icon, line.face, line.ink), LinearLayout.LayoutParams(c.dp(40), c.dp(40)))
                addView(c.body(line.text, 18f).apply {
                    setTextColor(Palette.INK)
                    setPadding(c.dp(12), 0, c.dp(8), 0)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(c.body(time.format(line.at), 15f))
            })
        }
        list.addView(card, c.below())
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

    private fun lines(): List<Line> {
        val c = context
        val diary = hub.diary.all().mapNotNull { e ->
            when (e.kind) {
                Diary.Kind.SCAM_STOPPED -> Line(e.at, R.drawable.ic_shield, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(SCAMS[e.detail] ?: R.string.diary_scam_link))
                Diary.Kind.DOUBT_CHECKED -> Line(e.at, R.drawable.ic_message, Palette.SUN_TINT, Palette.AMBER_INK, c.getString(R.string.diary_doubt))
                Diary.Kind.PAUSE_LEFT -> Line(e.at, R.drawable.ic_bank, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_pause_left))
                Diary.Kind.FAMILY_CALLED -> Line(e.at, R.drawable.ic_family, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_family_called))
                Diary.Kind.FAMILY_ASKED -> Line(e.at, R.drawable.ic_family, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_family_asked))
                Diary.Kind.WEB_SILENCED -> Line(e.at, R.drawable.ic_notifications, Palette.SKY_TINT, Palette.SKY_INK, c.getString(R.string.diary_web))
                Diary.Kind.QUIZ -> Line(e.at, R.drawable.ic_learn, Palette.SKY_TINT, Palette.SKY_INK, c.getString(R.string.diary_quiz, e.detail))
            }
        }
        val uses = hub.uses.all().map { e ->
            val who = e.who ?: c.getString(R.string.use_someone)
            when (e.kind) {
                UseLog.Kind.CALL -> Line(e.at, R.drawable.ic_call, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_call, who))
                UseLog.Kind.TEXT -> Line(e.at, R.drawable.ic_message, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_text, who))
                UseLog.Kind.LOCATION -> Line(e.at, R.drawable.ic_location, Palette.LILAC_TINT, Palette.LILAC_INK, c.getString(R.string.diary_location))
                UseLog.Kind.PICTURE -> Line(e.at, R.drawable.ic_image_search, Palette.SKY_TINT, Palette.SKY_INK, c.getString(R.string.diary_picture))
                UseLog.Kind.VOICE -> Line(e.at, R.drawable.ic_mic, Palette.GREEN_TINT, Palette.GREEN_DARK, c.getString(R.string.diary_voice))
                UseLog.Kind.REPORT -> Line(e.at, R.drawable.ic_message, Palette.CORAL_TINT, Palette.CORAL_INK, c.getString(R.string.diary_report))
            }
        }
        return (diary + uses).sortedByDescending { it.at }.take(MAX_LINES)
    }

    private fun dayKey(at: Long): Int = Calendar.getInstance().apply { timeInMillis = at }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) }

    private fun dayName(at: Long): String {
        val today = dayKey(System.currentTimeMillis())
        val yesterday = dayKey(System.currentTimeMillis() - DAY_MS)
        return when (dayKey(at)) {
            today -> context.getString(R.string.diary_today)
            yesterday -> context.getString(R.string.diary_yesterday)
            else -> DateFormat.getInstanceForSkeleton("EEEEdMMMM").format(at).replaceFirstChar { it.uppercase() }
        }
    }

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val MAX_LINES = 40
        val SCAMS = mapOf(
            "parcel" to R.string.diary_scam_parcel,
            "link" to R.string.diary_scam_link,
            "code" to R.string.diary_scam_code,
            "family" to R.string.diary_scam_family,
            "remote" to R.string.diary_scam_remote,
            "prize" to R.string.diary_scam_prize,
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
        list.addView(c.heroCard(c.getString(R.string.learn_title), Lines.next(c, R.array.learn_hello), R.drawable.peri_calm_detective, Palette.SKY_TINT, 0xFFA9D5F2.toInt(), Palette.SKY_INK))
        list.addView(quiz(), c.below())
        list.addView(c.label(c.getString(R.string.learn_traps_title)), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(20) })
        list.addView(traps(), c.below(10))
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
                    setImageDrawable(c.sized(R.drawable.peri_calm_idea, c.dp(96)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
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
        list.addView(c.heroCard(c.getString(R.string.family_page_title), Lines.next(c, R.array.family_hello), R.drawable.peri_family, Palette.LILAC_TINT, 0xFFCDBEF5.toInt(), Palette.LILAC_INK))
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
