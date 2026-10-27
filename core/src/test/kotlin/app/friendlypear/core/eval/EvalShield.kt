package app.friendlypear.core.eval

import app.friendlypear.core.model.Channel
import app.friendlypear.core.model.Contact
import app.friendlypear.core.model.IncomingMessage
import app.friendlypear.core.shield.Shield
import app.friendlypear.core.shield.ShieldContext
import java.io.File

/**
 * Runs the on-phone rules alone over an evaluation set (TSV: id, label, lang, channel, sender, known, call, text)
 * and writes one line per message: id, level, rules score, hard rule and signals. No network, no model.
 *
 *   cd core; ./gradlew evalShield --args="../eval/prueba-20-20/mensajes.tsv ../eval/prueba-20-20/resultados/reglas.tsv"
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: evalShield <messages.tsv> <out.tsv>" }
    val shield = Shield()
    val rows = File(args[0]).readLines(Charsets.UTF_8).drop(1).filter { it.isNotBlank() }
    val out = File(args[1]).apply { parentFile?.mkdirs() }
    out.bufferedWriter(Charsets.UTF_8).use { w ->
        w.write("id\tlevel\tscore\thardRule\tsignals\n")
        for (row in rows) {
            val c = row.split('\t')
            require(c.size == 8) { "bad row: ${c.firstOrNull()}" }
            val (id, _, _, channel, sender, known, call) = c
            val contacts = if (known == "1") listOf(Contact("eval", sender, emptyList())) else emptyList()
            val v = shield.evaluate(IncomingMessage(Channel.valueOf(channel), sender, c[7], 0), ShieldContext(contacts, call == "1"))
            w.write("$id\t${v.level}\t${"%.2f".format(java.util.Locale.US, v.rulesScore)}\t${v.hardRule ?: ""}\t${v.signals.sorted().joinToString(",")}\n")
        }
    }
    println("${rows.size} messages → ${out.path}")
}

private operator fun <T> List<T>.component6() = this[5]
private operator fun <T> List<T>.component7() = this[6]
