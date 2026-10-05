package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.model.FeatureToggle
import com.cat.model.WorldState
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

@Composable
fun DashboardScreen(
    features: List<FeatureToggle>,
    worlds: List<WorldState>,
    wide: Boolean,
    refreshKey: Int,
    onLaunch: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenTools: () -> Unit
) {
    val context = LocalContext.current
    var modeLabel by remember { mutableStateOf("Offline") }
    var snippet by remember { mutableStateOf("No replies yet") }
    var memoryCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshKey) {
        val app = context.applicationContext as CAtApplication
        modeLabel = app.prefs.mode.label
        memoryCount = app.database.memoryDao().count()
        val last = app.database.chatDao().latestAssistant()?.content?.replace("\n", " ")
        snippet = last?.take(90)?.ifBlank { null } ?: "No replies yet"
    }
    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusCard(modeLabel, snippet, memoryCount)
                Hero()
                Actions(onLaunch, onOpenMemory, onOpenTools)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModuleList(features)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WorldList(worlds)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(modeLabel, snippet, memoryCount)
            Hero()
            Actions(onLaunch, onOpenMemory, onOpenTools)
            ModuleList(features)
            WorldList(worlds)
        }
    }
}

@Composable
private fun StatusCard(modeLabel: String, snippet: String, memoryCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10161F), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Copilot", color = NeonCyan, fontSize = 20.sp)
        Text("Mode: $modeLabel", color = NeonLime)
        Text("Locale en-AU · Australia/Perth", color = Color(0xFFBFE8FF))
        Text("Memory notes: $memoryCount", color = Color(0xFFBFE8FF))
        Text("Last reply: $snippet", color = Color(0xFFEAFBFF))
    }
}

@Composable
private fun Hero() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "C@T", color = NeonCyan, fontSize = 42.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D14), RoundedCornerShape(24.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonMagenta, NeonLime)),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cyber AI Assistant", color = NeonCyan, fontSize = 24.sp)
                Text("Australia/Perth • en-AU • Offline copilot by default", color = Color(0xFFBFE8FF))
                Text(
                    "Launch opens chat. Wi-Fi is not required. Cloud is optional and falls back offline.",
                    color = NeonLime
                )
            }
        }
    }
}

@Composable
private fun Actions(onLaunch: () -> Unit, onOpenMemory: () -> Unit, onOpenTools: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("Launch", NeonCyan, Color.Black, onLaunch)
        ActionButton("Tools", NeonMagenta, Color.White, onOpenTools)
        ActionButton("Memory", NeonLime, Color.Black, onOpenMemory)
    }
}

@Composable
private fun ModuleList(features: List<FeatureToggle>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Modules", color = NeonCyan, fontSize = 20.sp)
        features.forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111821), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(feature.name, color = Color(0xFFEAFBFF))
                Text(
                    if (feature.enabled) "ON" else "OFF",
                    color = if (feature.enabled) NeonLime else NeonMagenta
                )
            }
        }
    }
}

@Composable
private fun WorldList(worlds: List<WorldState>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Worlds", color = NeonCyan, fontSize = 20.sp)
        worlds.forEach { world ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10161F), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(world.name, color = NeonCyan)
                    Text("${world.progress}% progress", color = Color(0xFFBFE8FF))
                    Text(world.mode, color = NeonLime)
                }
            }
        }
    }
}

@Composable
private fun RowScope.ActionButton(
    label: String,
    color: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor
        )
    ) {
        Text(label)
    }
}
