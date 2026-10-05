package com.cat.security

/**
 * Fixes a few obvious English command typos before the assistant sees them.
 * It does not run a shell and does not rewrite free sentences word by word.
 */
object SoftCorrect {
    private val words = mapOf(
        "hlp" to "help",
        "hlep" to "help",
        "hepl" to "help",
        "hep" to "help",
        "remeber" to "remember",
        "rember" to "remember",
        "rememeber" to "remember",
        "recal" to "recall",
        "recalll" to "recall",
        "sumary" to "summarize",
        "sumarise" to "summarize",
        "summarise" to "summarize",
        "setings" to "settings",
        "settigns" to "settings",
        "tiem" to "time",
        "clok" to "clock",
        "clocl" to "clock",
        "helo" to "hello",
        "helllo" to "hello",
        "whta" to "what",
        "waht" to "what",
        "ntoes" to "notes",
        "notse" to "notes",
        "memroy" to "memory",
        "memories" to "memories",
        "cler" to "clear",
        "cleer" to "clear",
        "versoin" to "version",
        "verison" to "version",
        "termnal" to "terminal",
        "termianl" to "terminal"
    )

    data class Result(val text: String, val changed: Boolean, val note: String?)

    fun apply(raw: String): Result {
        val original = raw.trim()
        if (original.isEmpty()) return Result(original, false, null)
        val slash = original.startsWith("/")
        val body = if (slash) original.drop(1) else original
        val head = body.substringBefore(' ')
        val rest = if (' ' in body) body.substringAfter(' ') else ""
        val key = head.lowercase()
        val fixed = words[key] ?: return Result(original, false, null)
        if (fixed == key) return Result(original, false, null)
        val rebuiltBody = if (rest.isEmpty()) fixed else "$fixed $rest"
        val text = if (slash) "/$rebuiltBody" else rebuiltBody
        return Result(
            text = text,
            changed = true,
            note = "auto-correct: ${if (slash) "/" else ""}$head → ${if (slash) "/" else ""}$fixed"
        )
    }
}
