package com.cat.tools

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

/**
 * On-device helpers. Nothing here uses the network.
 */
object LocalTools {
    val AU_LOCALE: Locale = Locale.forLanguageTag("en-AU")
    const val PERTH_ZONE = "Australia/Perth"

    val AU_ZONES = listOf(
        "Australia/Perth",
        "Australia/Adelaide",
        "Australia/Darwin",
        "Australia/Brisbane",
        "Australia/Sydney",
        "Australia/Melbourne",
        "Australia/Hobart"
    )

    data class Strength(val score: Int, val label: String)

    fun clockReport(epochMillis: Long): String {
        val lines = AU_ZONES.map { zone ->
            val city = zone.substringAfter('/')
            "$city: ${formatZone(epochMillis, zone)}"
        }
        return "Australia clocks (en-AU). Home is Perth. No network used.\n" + lines.joinToString("\n")
    }

    fun formatZone(epochMillis: Long, zoneId: String): String {
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of(zoneId))
        val fmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy, h:mm:ss a z", AU_LOCALE)
        return zdt.format(fmt)
    }

    fun passphraseStrength(value: String): Strength {
        if (value.isEmpty()) return Strength(0, "Empty")
        var score = 0
        if (value.length >= 8) score++
        if (value.length >= 12) score++
        if (value.any { it.isLowerCase() } && value.any { it.isUpperCase() }) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        score = score.coerceAtMost(4)
        val label = when (score) {
            0, 1 -> "Weak"
            2 -> "Fair"
            3 -> "Good"
            else -> "Strong"
        }
        return Strength(score, label)
    }

    fun generatePassphrase(length: Int, symbols: Boolean = true, random: SecureRandom = SecureRandom()): String {
        val len = length.coerceIn(8, 64)
        val alphabet = buildString {
            append("abcdefghijkmnopqrstuvwxyz")
            append("ABCDEFGHJKLMNPQRSTUVWXYZ")
            append("23456789")
            if (symbols) append("!@#%*?")
        }
        return buildString(len) {
            repeat(len) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
    }

    fun convert(value: Double, from: String, to: String): Double? {
        val a = from.trim().lowercase(AU_LOCALE)
        val b = to.trim().lowercase(AU_LOCALE)
        val length = mapOf(
            "km" to 1000.0, "m" to 1.0, "cm" to 0.01, "mm" to 0.001,
            "mi" to 1609.344, "mile" to 1609.344, "miles" to 1609.344,
            "ft" to 0.3048, "foot" to 0.3048, "in" to 0.0254
        )
        val mass = mapOf(
            "kg" to 1.0, "g" to 0.001,
            "lb" to 0.45359237, "lbs" to 0.45359237,
            "oz" to 0.028349523125
        )
        if (a in length && b in length) {
            return value * length.getValue(a) / length.getValue(b)
        }
        if (a in mass && b in mass) {
            return value * mass.getValue(a) / mass.getValue(b)
        }
        val celsius = toCelsius(value, a) ?: return null
        return fromCelsius(celsius, b)
    }

    fun formatAmount(n: Double): String {
        if (n.isNaN() || n.isInfinite()) return "—"
        val raw = String.format(AU_LOCALE, "%.4f", n).trimEnd('0').trimEnd('.')
        return raw.ifEmpty { "0" }
    }

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun base64Encode(text: String): String {
        return Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
    }

    fun base64Decode(text: String): String {
        return String(Base64.getDecoder().decode(text.trim()), Charsets.UTF_8)
    }

    fun prettyJson(raw: String): String {
        val src = raw.trim()
        if (src.isEmpty() || (src.first() != '{' && src.first() != '[')) {
            error("JSON must start with { or [")
        }
        val out = StringBuilder()
        var indent = 0
        var inString = false
        var escape = false
        var i = 0
        fun newline() {
            out.append('\n')
            repeat(indent) { out.append("  ") }
        }
        while (i < src.length) {
            val c = src[i]
            if (inString) {
                out.append(c)
                if (escape) escape = false
                else if (c == '\\') escape = true
                else if (c == '"') inString = false
                i++
                continue
            }
            when (c) {
                ' ', '\n', '\r', '\t' -> Unit
                '"' -> {
                    inString = true
                    out.append(c)
                }
                '{', '[' -> {
                    out.append(c)
                    indent++
                    var j = i + 1
                    while (j < src.length && src[j].isWhitespace()) j++
                    val next = src.getOrNull(j)
                    if (next != '}' && next != ']') newline()
                }
                '}', ']' -> {
                    indent = (indent - 1).coerceAtLeast(0)
                    newline()
                    out.append(c)
                }
                ',' -> {
                    out.append(c)
                    newline()
                }
                ':' -> out.append(": ")
                else -> out.append(c)
            }
            i++
        }
        if (inString || indent != 0) error("Could not pretty-print that JSON.")
        return out.toString()
    }

    private fun toCelsius(value: Double, unit: String): Double? = when (unit) {
        "c", "celsius" -> value
        "f", "fahrenheit" -> (value - 32.0) * 5.0 / 9.0
        "k", "kelvin" -> value - 273.15
        else -> null
    }

    private fun fromCelsius(celsius: Double, unit: String): Double? = when (unit) {
        "c", "celsius" -> celsius
        "f", "fahrenheit" -> celsius * 9.0 / 5.0 + 32.0
        "k", "kelvin" -> celsius + 273.15
        else -> null
    }
}
