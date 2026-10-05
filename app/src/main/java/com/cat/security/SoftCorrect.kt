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
        "commnd" to "command"
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
        val mappedHead = words[key]
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
