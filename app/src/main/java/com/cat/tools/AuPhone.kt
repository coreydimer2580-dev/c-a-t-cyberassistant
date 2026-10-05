package com.cat.tools

/**
 * Normal phone numbers only. Star and hash codes are rejected.
 * This does not send SMS or place calls.
 */
object AuPhone {
    data class PhoneCommand(
        val reply: String,
        val dial: Boolean?,
        val number: String?,
        val body: String?
    )

    fun parse(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.any { it == '*' || it == '#' || it == ';' || it == ',' }) return null
        if (trimmed.any { it.isLetter() }) return null
        val plus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length !in 6..15) return null
        if (digits.all { it == '0' }) return null
        return if (plus) "+$digits" else digits
    }

    /**
     * Slash commands only: /call, /dial, /sms, /text, /msg.
     * Returns null when the text is not one of those commands.
     */
    fun interpret(raw: String): PhoneCommand? {
        val text = raw.trim()
        if (!text.startsWith("/")) return null
        val body = text.drop(1).trim()
        if (body.isEmpty()) return null
        val cmd = body.substringBefore(' ').lowercase()
        val rest = if (' ' in body) body.substringAfter(' ').trim() else ""
        val dial = when (cmd) {
            "call", "dial" -> true
            "sms", "text", "msg" -> false
            else -> return null
        }
        if (rest.isBlank()) {
            val example = if (dial) "/call 0412345678" else "/sms 0412345678 on my way"
            return PhoneCommand(
                reply = "Add a normal phone number, for example $example. C@T only opens your phone's own app. It does not call or text by itself.",
                dial = null,
                number = null,
                body = null
            )
        }
        val numberRaw: String
        val smsBody: String?
        if (dial) {
            numberRaw = rest
            smsBody = null
        } else {
            val split = splitSms(rest)
            if (split == null) {
                return PhoneCommand(
                    reply = "That doesn't look like a normal phone number. Example: /sms 0412345678 see you soon.",
                    dial = null,
                    number = null,
                    body = null
                )
            }
            numberRaw = split.first
            smsBody = split.second.ifBlank { null }
        }
        val number = parse(numberRaw)
        if (number == null) {
            return PhoneCommand(
                reply = "That doesn't look like a normal phone number. Use digits such as 0412345678 or +61412345678. Star and hash codes are blocked. C@T does not text or call by itself.",
                dial = null,
                number = null,
                body = null
            )
        }
        val reply = if (dial) {
            "Opening your phone's dialer. C@T does not place the call. Your own dialer and carrier do that if you confirm."
        } else {
            "Opening your phone's SMS app with a draft. C@T does not send the message. You still confirm it in your messaging app. This is not a separate network."
        }
        return PhoneCommand(reply, dial, number, smsBody)
    }

    fun splitSms(rest: String): Pair<String, String>? {
        val trimmed = rest.trim()
        val match = Regex("^([+0-9][0-9\\s().-]*)").find(trimmed) ?: return null
        var chunk = match.groupValues[1].trim()
        val body = trimmed.removePrefix(match.groupValues[1]).trim()
        while (chunk.isNotEmpty() && !chunk.last().isDigit()) {
            chunk = chunk.dropLast(1).trimEnd()
        }
        if (parse(chunk) == null) return null
        return chunk to body
    }
}
