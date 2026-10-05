package com.cat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: MemoryEntity)

    /** Newest first. No SQL LIMIT and no note-count cap. Rows stay until the user deletes them. */
    @Query("SELECT * FROM memory ORDER BY createdAt DESC, id DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("SELECT * FROM memory ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("DELETE FROM memory")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM memory")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM memory")
    fun observeCount(): Flow<Int>

    @Query("UPDATE memory SET truthTag = :truthTag WHERE id = :id")
    suspend fun updateTruthTag(id: Long, truthTag: String)
}
