package com.cat.data

/**
 * Terminal lines only. Never written through ChatDao.
 */
data class TerminalLine(
    val role: String,
    val text: String,
    val at: Long
)

object TerminalTranscript {
    private const val SEP = "\u001F"
    private const val MAX = 200

    fun encode(lines: List<TerminalLine>): String {
        return lines.takeLast(MAX).joinToString("\n") { line ->
            val role = line.role.replace('\n', ' ').take(16)
            val text = line.text.replace("\\", "\\\\").replace("\n", "\\n").take(4000)
            "$role$SEP${line.at}$SEP$text"
        }
    }

    fun decode(raw: String): List<TerminalLine> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { row ->
            val parts = row.split(SEP, limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val at = parts[1].toLongOrNull() ?: return@mapNotNull null
            val text = parts[2].replace("\\n", "\n").replace("\\\\", "\\")
            TerminalLine(parts[0], text, at)
        }.take(MAX).toList()
    }
}
