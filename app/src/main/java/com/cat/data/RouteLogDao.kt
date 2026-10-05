package com.cat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RouteLogDao {
    @Insert
    suspend fun insert(entry: RouteLogEntity): Long

    @Query("SELECT * FROM route_log ORDER BY createdAt DESC, id DESC")
    suspend fun newestFirst(): List<RouteLogEntity>

    @Query("DELETE FROM route_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM route_log")
    suspend fun clearAll()
}
