package com.cat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessage)

    @Query("SELECT * FROM chat_message ORDER BY createdAt ASC, id ASC")
    suspend fun getAll(): List<ChatMessage>

    @Query("DELETE FROM chat_message")
    suspend fun clearAll()

    @Query("DELETE FROM chat_message WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM chat_message WHERE role = 'assistant' ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun latestAssistant(): ChatMessage?
}
