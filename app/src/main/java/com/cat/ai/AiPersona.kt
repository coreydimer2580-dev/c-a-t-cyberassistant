package com.cat.ai

/**
 * Wheel spokes: mode personas (Offline / Cloud / Auto) plus named style personas.
 * Named personas always work offline; cloud is optional when mode allows it.
 */
enum class AiPersona(
    val id: String,
    val label: String,
    val shortLabel: String,
    val styleHint: String,
    val linkedMode: CopilotMode? = null
) {
    OFFLINE_CAT(
        id = "offline",
        label = "Offline C@T",
        shortLabel = "C@T",
        styleHint = "Direct local answers. Australia/Perth, en-AU. No Wi-Fi needed.",
        linkedMode = CopilotMode.OFFLINE
    ),
    CLOUD_GPT(
        id = "cloud",
        label = "Cloud GPT",
        shortLabel = "Cloud",
        styleHint = "OpenAI-compatible cloud when a base URL is set. Falls back offline.",
        linkedMode = CopilotMode.CLOUD
    ),
    AUTO(
        id = "auto",
        label = "Auto",
        shortLabel = "Auto",
        styleHint = "Try cloud when online, otherwise use Offline C@T.",
        linkedMode = CopilotMode.AUTO
    ),
    ANALYST(
        id = "analyst",
        label = "Analyst",
        shortLabel = "Analyst",
        styleHint = "Break the question into facts, options, and trade-offs."
    ),
    CODER(
        id = "coder",
        label = "Coder",
        shortLabel = "Coder",
        styleHint = "Practical steps, concise, code-minded. Prefer actionable lists."
    ),
    COACH(
        id = "coach",
        label = "Coach",
        shortLabel = "Coach",
        styleHint = "Encouraging, next-action focused. Keep momentum."
    ),
    CREATIVE(
        id = "creative",
        label = "Creative",
        shortLabel = "Creative",
        styleHint = "Ideas, metaphors, and fresh angles without fluff."
    );

    val isModePersona: Boolean get() = linkedMode != null

    companion object {
        fun fromId(value: String?): AiPersona {
            return entries.firstOrNull { it.id == value } ?: OFFLINE_CAT
        }

        /** Personas used in group-solver rounds (always offline-capable). */
        val groupRoster: List<AiPersona> = listOf(ANALYST, CODER, COACH)

        fun wheelOrder(): List<AiPersona> = entries.toList()
    }
}
