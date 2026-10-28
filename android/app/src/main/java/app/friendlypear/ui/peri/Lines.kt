package app.friendlypear.ui.peri

import android.content.Context

/**
 * Peri's lines go round a shuffled deck per bank (a string array) and language, so nothing repeats until every
 * line of the bank has been said.
 */
internal object Lines {
    private val decks = HashMap<String, ArrayDeque<String>>()

    @Synchronized
    fun next(context: Context, array: Int): String {
        val key = "$array:${context.resources.configuration.locales[0]}"
        val deck = decks.getOrPut(key) { ArrayDeque() }
        if (deck.isEmpty()) deck.addAll(context.resources.getStringArray(array).toList().shuffled())
        return deck.removeFirstOrNull().orEmpty()
    }
}
