package com.cat.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import com.cat.ui.theme.Panel

private object Routes {
    const val DASHBOARD = "dashboard"
    const val MEMORY = "memory"
    const val VOICE = "voice"
    const val WORLD = "world"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
    const val TOOLS = "tools"
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

    val tabs = listOf("Dashboard", "Memory", "Tools", "Voice", "World", "Settings")
    val routes = listOf(
        Routes.DASHBOARD,
        Routes.MEMORY,
        Routes.TOOLS,
        Routes.VOICE,
        Routes.WORLD,
        Routes.SETTINGS
    )
    var selectedTab by remember { mutableIntStateOf(0) }
    var dashEpoch by remember { mutableIntStateOf(0) }
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
        if (index == 0) dashEpoch += 1
        selectedTab = index
        navController.navigate(routes[index]) {
            popUpTo(Routes.DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openChat() {
        navController.navigate(Routes.CHAT) { launchSingleTop = true }
    }

    CATTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            if (wide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NeonSidebar(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onChat = onChat,
                        onSelect = { openTab(it) },
                        onChatClick = { openChat() }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        AppNav(
                            navController = navController,
                            features = features,
                            stacks = stacks,
                            voices = voices,
                            worlds = worlds,
                            wide = true,
                            dashEpoch = dashEpoch,
                            onOpenTab = { openTab(it) },
                            onLaunch = { openChat() },
                            onToggle = { index ->
                                val current = features[index]
                                features[index] = current.copy(enabled = !current.enabled)
                            },
                            onChatBack = {
                                dashEpoch += 1
                                navController.popBackStack()
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    if (!onChat) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Panel,
                            contentColor = NeonCyan,
                            edgePadding = 0.dp,
                            indicator = { positions ->
                                if (selectedTab < positions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(positions[selectedTab]),
                                        color = NeonCyan
                                    )
                                }
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { openTab(index) },
                                    selectedContentColor = NeonCyan,
                                    unselectedContentColor = Color(0xFF6A8A96),
                                    text = {
                                        Text(
                                            title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                )
                            }
                        }
                    }
                    AppNav(
                        navController = navController,
                        features = features,
                        stacks = stacks,
                        voices = voices,
                        worlds = worlds,
                        wide = false,
                        dashEpoch = dashEpoch,
                        onOpenTab = { openTab(it) },
                        onLaunch = { openChat() },
                        onToggle = { index ->
                            val current = features[index]
                            features[index] = current.copy(enabled = !current.enabled)
                        },
                        onChatBack = {
                            dashEpoch += 1
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NeonSidebar(
    tabs: List<String>,
    selectedTab: Int,
    onChat: Boolean,
    onSelect: (Int) -> Unit,
    onChatClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(188.dp)
            .fillMaxHeight()
            .background(Panel)
            .padding(14.dp)
    ) {
        Text("C@T", color = NeonCyan, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("v1.4 · AU offline", color = NeonLime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("No network scan", color = NeonMagenta, fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp))
        tabs.forEachIndexed { index, title ->
            SideItem(
                label = title,
                selected = !onChat && selectedTab == index,
                accent = NeonCyan,
                onClick = { onSelect(index) }
            )
        }
        SideItem(
            label = "Chat",
            selected = onChat,
            accent = NeonMagenta,
            onClick = onChatClick
        )
    }
}

@Composable
private fun SideItem(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text = label,
        color = if (selected) Color.Black else accent,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        modifier = Modifier
            .padding(vertical = 3.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun ColumnScope.AppNav(
    navController: androidx.navigation.NavHostController,
    features: List<FeatureToggle>,
    stacks: List<MemoryStack>,
    voices: List<VoiceProfile>,
    worlds: List<WorldState>,
    wide: Boolean,
    dashEpoch: Int,
    onOpenTab: (Int) -> Unit,
    onLaunch: () -> Unit,
    onToggle: (Int) -> Unit,
    onChatBack: () -> Unit
) {
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
                refreshKey = dashEpoch,
                onLaunch = onLaunch,
                onOpenMemory = { onOpenTab(1) },
                onOpenTools = { onOpenTab(2) }
            )
        }
        composable(Routes.MEMORY) { MemoryScreen(stacks, wide) }
        composable(Routes.TOOLS) { ToolsScreen(wide) }
        composable(Routes.VOICE) { VoiceScreen(voices) }
        composable(Routes.WORLD) { WorldScreen(worlds) }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                features = features,
                wide = wide,
                onToggle = onToggle
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(wide = wide, onBack = onChatBack)
        }
    }
}
