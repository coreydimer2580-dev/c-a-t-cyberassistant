package com.cat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A note kept in the on-device Room database.
 * Nothing here expires. It stays until the user clears it.
 */
@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String,
    val createdAt: Long
)
