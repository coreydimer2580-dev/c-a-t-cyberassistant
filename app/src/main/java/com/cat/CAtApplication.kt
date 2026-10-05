package com.cat

import android.app.Application
import androidx.room.Room
import com.cat.data.AppDatabase
import com.cat.data.CopilotPrefs
import com.cat.data.CopilotRepository

class CAtApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var prefs: CopilotPrefs
        private set
    lateinit var copilot: CopilotRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cat-memory.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
        prefs = CopilotPrefs(applicationContext)
        copilot = CopilotRepository(database, prefs)
    }
}
