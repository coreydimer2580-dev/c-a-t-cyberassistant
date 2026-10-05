package com.cat.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MemoryEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
