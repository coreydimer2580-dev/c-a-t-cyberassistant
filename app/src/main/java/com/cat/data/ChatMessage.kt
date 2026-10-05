package com.cat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val content: String,
    val createdAt: Long,
    val filtered: Boolean,
    /** AiPersona.id for assistant turns; empty for user or legacy rows. */
    val personaId: String = ""
)
