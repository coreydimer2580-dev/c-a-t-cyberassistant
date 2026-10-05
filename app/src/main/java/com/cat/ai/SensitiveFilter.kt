package com.cat.ai

/**
 * Redacts common sensitive patterns from text the user pastes in.
 * This does not read the device, files, or notification history.
 */
class SensitiveFilter {
    private val patterns = listOf(
        Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
        Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
        Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"),
        Regex("\\b(?:\\+?\\d{1,3}[-.\\s]?)?(?:\\(?\\d{2,4}\\)?[-.\\s]?)\\d{3}[-.\\s]?\\d{4}\\b"),
        Regex("(?i)\\b(password|secret|token|api_key|private_key|confidential)\\b")
    )

    fun sanitize(input: String): String {
        var output = input
        for (regex in patterns) {
            output = regex.replace(output, "[FILTERED]")
        }
        return output
    }
}
