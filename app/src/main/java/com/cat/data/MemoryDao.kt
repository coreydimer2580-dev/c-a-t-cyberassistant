package com.cat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoryDao {
    @Insert
    suspend fun insert(memory: MemoryEntity)

    @Query("SELECT * FROM memory ORDER BY createdAt DESC")
    suspend fun getAll(): List<MemoryEntity>

    @Query("DELETE FROM memory")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM memory")
    suspend fun count(): Int
}
