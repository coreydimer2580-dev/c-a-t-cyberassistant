package com.cat

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.Room
import com.cat.data.AppDatabase
import com.cat.data.CopilotPrefs
import com.cat.data.CopilotRepository
import java.util.Locale

class CAtApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var prefs: CopilotPrefs
        private set
    lateinit var copilot: CopilotRepository
        private set

    override fun attachBaseContext(base: Context) {
        val locale = Locale.forLanguageTag("en-AU")
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cat-memory.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        prefs = CopilotPrefs(applicationContext)
        if (prefs.mode != com.cat.ai.CopilotMode.OFFLINE &&
            prefs.mode != com.cat.ai.CopilotMode.CLOUD &&
            prefs.mode != com.cat.ai.CopilotMode.AUTO
        ) {
            prefs.mode = com.cat.ai.CopilotMode.OFFLINE
        }
        copilot = CopilotRepository(database, prefs, networkAvailable = { hasNetwork() })
    }

    private fun hasNetwork(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
