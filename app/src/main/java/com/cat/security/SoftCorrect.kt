package com.cat.security

/**
 * Fixes obvious English command typos before the assistant sees them.
 * Only the command word (and a following command word) is rewritten.
 * It does not run a shell and does not rewrite free sentences word by word.
 */
object SoftCorrect {
    private val words = mapOf(
        "hlp" to "help",
        "hlep" to "help",
        "hepl" to "help",
        "hep" to "help",
        "helpp" to "help",
        "halp" to "help",
        "heplp" to "help",
        "hrlp" to "help",
        "hel p" to "help",
        "remeber" to "remember",
        "rember" to "remember",
        "rememeber" to "remember",
        "remembr" to "remember",
        "remebmer" to "remember",
        "rememebr" to "remember",
        "remebber" to "remember",
        "remmember" to "remember",
        "recal" to "recall",
        "recalll" to "recall",
        "recell" to "recall",
        "racall" to "recall",
        "recaal" to "recall",
        "recal l" to "recall",
        "sumary" to "summarize",
        "sumarise" to "summarize",
        "summarise" to "summarize",
        "summery" to "summarize",
        "setings" to "settings",
        "settigns" to "settings",
        "tiem" to "time",
        "tmie" to "time",
        "tme" to "time",
        "tim" to "time",
        "clok" to "clock",
        "clocl" to "clock",
        "helo" to "hello",
        "helllo" to "hello",
        "whta" to "what",
        "waht" to "what",
        "ntoes" to "notes",
        "notse" to "notes",
        "memroy" to "memory",
        "memery" to "memory",
        "memor y" to "memory",
        "cler" to "clear",
        "cleer" to "clear",
        "claer" to "clear",
        "clera" to "clear",
        "clr" to "clear",
        "clearr" to "clear",
        "versoin" to "version",
        "verison" to "version",
        "verson" to "version",
        "versin" to "version",
        "verion" to "version",
        "versioin" to "version",
        "vesion" to "version",
        "termnal" to "terminal",
        "termianl" to "terminal",
        "staus" to "status",
        "statsu" to "status",
        "statis" to "status",
        "sttus" to "status",
        "statu" to "status",
        "satus" to "status",
        "statuss" to "status",
        "unlok" to "unlock",
        "unlcok" to "unlock",
        "unock" to "unlock",
        "unlokc" to "unlock",
        "ulock" to "unlock",
        "unloock" to "unlock",
        "comand" to "command",
        "commnd" to "command",
        "hlpe" to "help",
        "helb" to "help",
        "remmeber" to "remember",
        "remeberr" to "remember",
        "recal1" to "recall",
        "stattus" to "status",
        "ststus" to "status",
        "verisonn" to "version",
        "versiom" to "version",
        "unlck" to "unlock",
        "sumarize" to "summarize",
        "summarze" to "summarize",
        "summrize" to "summarize",
        "seting" to "settings",
        "helpppp" to "help",
        "remembrr" to "remember",
        "remmber" to "remember",
        "recaall" to "recall",
        "stauts" to "status",
        "staatus" to "status",
        "versioon" to "version",
        "verssion" to "version",
        "c1ear" to "clear",
        "clrrr" to "clear",
        "unllock" to "unlock",
        "unlokk" to "unlock",
        "sumarrize" to "summarize",
        "settngs" to "settings",
        "setttings" to "settings",
        "toools" to "tools",
        "todoo" to "todo"
    )

    /** Command words fuzzy repair may approach. Not general English. */
    private val fuzzyTargets = listOf(
        "help", "remember", "recall", "status", "version", "clear",
        "unlock", "summarize", "settings", "tools", "todo"
    )

    /** Near-miss English that must stay as typed. */
    private val fuzzyDeny = setOf(
        "clean", "clever", "clearly", "held", "helm", "heap", "hello",
        "states", "static", "statue", "versus", "verses", "unless",
        "summary", "summer", "start", "store", "story", "still",
        "total", "today", "toolshed"
    )

    private val commands = words.values.toSet()

    data class Result(val text: String, val changed: Boolean, val note: String?)

    fun apply(raw: String): Result {
        val original = raw.trim()
        if (original.isEmpty()) return Result(original, false, null)
        val slash = original.startsWith("/")
        val body = if (slash) original.drop(1) else original
        val head = body.substringBefore(' ')
        val rest = if (' ' in body) body.substringAfter(' ') else ""
        val key = head.lowercase()
        val mappedHead = words[key] ?: fuzzyCommand(key)
        val headIsCommand = mappedHead != null || key in commands
        if (!headIsCommand) return Result(original, false, null)

        val newHead = mappedHead ?: head
        val headChanged = mappedHead != null && mappedHead != key
        val (newRest, restChanged, restNote) = fixTail(rest)
        if (!headChanged && !restChanged) return Result(original, false, null)

        val rebuiltBody = if (newRest.isEmpty()) newHead else "$newHead $newRest"
        val text = if (slash) "/$rebuiltBody" else rebuiltBody
        val mark = if (slash) "/" else ""
        val note = buildString {
            append("auto-correct: ")
            if (headChanged) append("$mark$head → $mark$newHead")
            if (restChanged) {
                if (headChanged) append("; ")
                append(restNote)
            }
        }
        return Result(text = text, changed = true, note = note)
    }

    /** One-edit (or two on a long token) repair toward a command, same first letter. */
    private fun fuzzyCommand(token: String): String? {
        if (token.length < 4 || token in fuzzyDeny || token in commands) return null
        if (!token.all { it.isLetter() }) return null
        var best: String? = null
        var bestDistance = Int.MAX_VALUE
        for (target in fuzzyTargets) {
            if (token[0] != target[0]) continue
            if (kotlin.math.abs(token.length - target.length) > 2) continue
            val limit = if (token.length >= 8) 2 else 1
            if (bestDistance <= 1 && limit == 1) break
            val distance = editDistance(token, target, limit)
            if (distance in 1..limit && distance < bestDistance) {
                best = target
                bestDistance = distance
                if (bestDistance == 1) break
            }
        }
        return best
    }

    /** Levenshtein capped at [limit]; returns limit+1 when already worse. */
    private fun editDistance(a: String, b: String, limit: Int): Int {
        if (kotlin.math.abs(a.length - b.length) > limit) return limit + 1
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in a.indices) {
            curr[0] = i + 1
            var rowMin = curr[0]
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
                if (curr[j + 1] < rowMin) rowMin = curr[j + 1]
            }
            if (rowMin > limit) return limit + 1
            val swap = prev
            prev = curr
            curr = swap
        }
        return prev[b.length]
    }

    private fun fixTail(rest: String): Triple<String, Boolean, String> {
        if (rest.isEmpty()) return Triple(rest, false, "")
        val second = rest.substringBefore(' ')
        val tail = if (' ' in rest) rest.substringAfter(' ') else ""
        val mapped = words[second.lowercase()] ?: return Triple(rest, false, "")
        if (mapped == second.lowercase()) return Triple(rest, false, "")
        val rebuilt = if (tail.isEmpty()) mapped else "$mapped $tail"
        return Triple(rebuilt, true, "$second → $mapped")
    }
}
