package com.cat.ai

enum class CopilotMode(val label: String, val storage: String) {
    OFFLINE("Offline", "offline"),
    CLOUD("Cloud", "cloud"),
    AUTO("Auto", "auto");

    companion object {
        fun fromStorage(value: String?): CopilotMode {
            return entries.firstOrNull { it.storage == value } ?: OFFLINE
        }
    }
}
