package com.cat

/**
 * One copy of what C@T is, shared by Terminal, Chat, Online, Wheel, Memory, and Settings.
 * Visual and help text only. It does not update the app or listen in the background.
 */
object ProductGuide {
    const val VERSION_LABEL = "v1.16 · 10 upgrades"
    const val PATH = "YOU → SoftCorrect → MEMORY → REPLY → Evolve"
    const val SPEECH = "Autopilot speech is optional and lives on Chat only. Terminal stays quiet. No mic."
    const val SAFE = "No shell, no packages, no background mic, no 24/7 crawl, no self-update, no other-app scan."

    data class Surface(val name: String, val line: String)

    fun surfaces(): List<Surface> = listOf(
        Surface("Terminal", "Home. PIN gate, Private, AES vault, one-tap Auto, todo chips, Perth clock, evolve path on each send."),
        Surface("Chat", "Replies here. Optional speech and optional online evolve only when you send."),
        Surface("Online", "ChatGPT-style chat. No login or key: Offline English AI answers. Optional cloud only if you add your own URL + key."),
        Surface("Wheel", "Who answers: Offline, Cloud, Auto, Analyst, Coder, Coach, Creative, or Group."),
        Surface("Memory", "Notes stay on this phone. Swipe to tag True, False, or Unsure. Export as PDF via the share sheet."),
        Surface("Settings", "Use Offline only, or Try Cloud if saved. Neon accent, easy PIN change, backup reminder.")
    )
}
