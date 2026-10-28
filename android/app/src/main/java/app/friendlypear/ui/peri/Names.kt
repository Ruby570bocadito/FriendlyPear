package app.friendlypear.ui.peri

import android.content.Context
import app.friendlypear.R

/** "Ana and Papá", "Ana, Papá and Pablo", in the app's language. */
fun namesAnd(context: Context, names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> context.getString(R.string.names_and, names.dropLast(1).joinToString(", "), names.last())
}
