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
import com.cat.ui.theme.CATTheme
import com.cat.ui.theme.CatWordmark
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import com.cat.ui.theme.Panel
import com.cat.ui.theme.neonCard

private object Routes {
    const val CHAT = "chat"
    const val TERMINAL = "terminal"
    const val WHEEL = "wheel"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
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

    val tabs = listOf("Chat", "Terminal", "Wheel", "Memory", "Settings")
    val routes = listOf(Routes.CHAT, Routes.TERMINAL, Routes.WHEEL, Routes.MEMORY, Routes.SETTINGS)
    var selectedTab by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Routes.CHAT
    val onTools = route == Routes.TOOLS

    fun openTab(index: Int) {
        selectedTab = index
        navController.navigate(routes[index]) {
            popUpTo(Routes.CHAT) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    androidx.compose.runtime.LaunchedEffect(route) {
        val tabIndex = when (route) {
            Routes.CHAT -> 0
            Routes.TERMINAL -> 1
            Routes.WHEEL -> 2
            Routes.MEMORY -> 3
            Routes.SETTINGS, Routes.TOOLS -> 4
            else -> selectedTab
        }
        if (tabIndex != selectedTab) selectedTab = tabIndex
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
                        onSelect = { openTab(it) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        AppNav(
                            navController = navController,
                            wide = true,
                            onOpenWheel = { openTab(2) },
                            onOpenTools = {
                                navController.navigate(Routes.TOOLS) { launchSingleTop = true }
                            },
                            onToolsBack = { openTab(4) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    if (!onTools) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            CatWordmark(size = 40.sp)
                            Text(
                                "v1.8 · Locked terminal",
                                color = NeonLime,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
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
                        wide = false,
                        onOpenWheel = { openTab(2) },
                        onOpenTools = {
                            navController.navigate(Routes.TOOLS) { launchSingleTop = true }
                        },
                        onToolsBack = { openTab(4) }
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
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .width(188.dp)
            .fillMaxHeight()
            .background(Panel)
            .padding(14.dp)
    ) {
        CatWordmark(size = 40.sp)
        Text("v1.8 · Locked terminal", color = NeonLime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("Terminal · Wheel · Memory", color = NeonMagenta, fontSize = 11.sp, modifier = Modifier.padding(bottom = 16.dp))
        tabs.forEachIndexed { index, title ->
            SideItem(
                label = title,
                selected = selectedTab == index,
                accent = when (index) {
                    0 -> NeonCyan
                    1 -> NeonLime
                    2 -> NeonMagenta
                    else -> NeonLime
                },
                onClick = { onSelect(index) }
            )
        }
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
            .then(if (selected) Modifier.neonCard(accent = accent, shape = shape, fill = accent, glow = 10.dp) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

@Composable
private fun ColumnScope.AppNav(
    navController: androidx.navigation.NavHostController,
    wide: Boolean,
    onOpenWheel: () -> Unit,
    onOpenTools: () -> Unit,
    onToolsBack: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT,
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        composable(Routes.CHAT) {
            ChatScreen(wide = wide, onOpenWheel = onOpenWheel)
        }
        composable(Routes.TERMINAL) {
            val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.cat.CAtApplication
            TerminalScreen(app)
        }
        composable(Routes.WHEEL) {
            WheelScreen(wide = wide)
        }
        composable(Routes.MEMORY) {
            MemoryScreen(stacks = emptyList(), wide = wide)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                features = emptyList(),
                wide = wide,
                onToggle = {},
                onOpenTools = onOpenTools
            )
        }
        composable(Routes.TOOLS) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "← Settings",
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onToolsBack)
                        .padding(bottom = 8.dp)
                )
                ToolsScreen(wide)
            }
        }
    }
}
