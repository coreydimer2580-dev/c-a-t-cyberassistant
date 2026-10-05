package com.cat

import android.app.Application
import androidx.room.Room
import com.cat.data.AppDatabase

class CAtApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cat-memory.db"
        ).build()
    }
}
