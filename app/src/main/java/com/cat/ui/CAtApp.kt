package com.cat.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cat.model.FeatureToggle
import com.cat.model.MemoryStack
import com.cat.model.VoiceProfile
import com.cat.model.WorldState
import com.cat.ui.theme.CATTheme
import com.cat.ui.theme.NeonCyan

private object Routes {
    const val DASHBOARD = "dashboard"
    const val MEMORY = "memory"
    const val VOICE = "voice"
    const val WORLD = "world"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun CAtApp() {
    val activity = LocalContext.current as? Activity
    val wide = if (activity != null) {
        calculateWindowSizeClass(activity).widthSizeClass != WindowWidthSizeClass.Compact
    } else {
        false
    }

    val tabs = listOf("Dashboard", "Memory", "Voice", "World", "Settings")
    val routes = listOf(
        Routes.DASHBOARD,
        Routes.MEMORY,
        Routes.VOICE,
        Routes.WORLD,
        Routes.SETTINGS
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val onChat = backStack?.destination?.route == Routes.CHAT

    val features = remember {
        mutableStateListOf(
            FeatureToggle("Offline notes", true),
            FeatureToggle("Voice profiles", true),
            FeatureToggle("Memory stacks", true),
            FeatureToggle("Private mode", true),
            FeatureToggle("Holographic UI", true),
            FeatureToggle("World simulator", true),
            FeatureToggle("Satellite view", false),
            FeatureToggle("Game world", false),
            FeatureToggle("Emulator", false),
            FeatureToggle("Backend sync", false)
        )
    }

    val stacks = remember {
        listOf(
            MemoryStack("Core Stack", 82, true),
            MemoryStack("Voice Stack", 60, true),
            MemoryStack("Safety Stack", 74, true),
            MemoryStack("World Stack", 55, true),
            MemoryStack("Simulation Stack", 44, true),
            MemoryStack("Knowledge Stack", 68, true),
            MemoryStack("Scenes Stack", 52, true),
            MemoryStack("Progress Stack", 61, true)
        )
    }

    val voices = remember {
        listOf(
            VoiceProfile("Nova", "Warm", 87, true),
            VoiceProfile("Cipher", "Neutral", 92, false),
            VoiceProfile("Echo", "Calm", 78, false)
        )
    }

    val worlds = remember {
        listOf(
            WorldState("London Echo", 72, "Simulation"),
            WorldState("Grid Pulse", 61, "Predictive"),
            WorldState("Holo Drift", 48, "Creative")
        )
    }

    fun openTab(index: Int) {
        selectedTab = index
        navController.navigate(routes[index]) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CATTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (!onChat) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF0C0F16),
                        contentColor = NeonCyan,
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { openTab(index) },
                                text = { Text(title) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.DASHBOARD,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            features = features,
                            worlds = worlds,
                            wide = wide,
                            onLaunch = { navController.navigate(Routes.CHAT) },
                            onOpenMemory = { openTab(1) }
                        )
                    }
                    composable(Routes.MEMORY) { MemoryScreen(stacks, wide) }
                    composable(Routes.VOICE) { VoiceScreen(voices) }
                    composable(Routes.WORLD) { WorldScreen(worlds) }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            features = features,
                            wide = wide,
                            onToggle = { index ->
                                val current = features[index]
                                features[index] = current.copy(enabled = !current.enabled)
                            }
                        )
                    }
                    composable(Routes.CHAT) {
                        ChatScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
