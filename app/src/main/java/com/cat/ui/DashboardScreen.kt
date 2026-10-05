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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.data.MemoryEntity
import com.cat.data.MemoryStorage
import com.cat.model.FeatureToggle
import com.cat.model.WorldState
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import com.cat.ui.theme.Panel

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
    val app = context.applicationContext as CAtApplication
    val memories by app.database.memoryDao().observeAll().collectAsState(initial = emptyList())
    var modeLabel by remember { mutableStateOf("Offline") }
    var snippet by remember { mutableStateOf("No replies yet") }
    LaunchedEffect(refreshKey) {
        modeLabel = app.prefs.mode.label
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
                StatusCard(modeLabel, snippet, memories)
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
            StatusCard(modeLabel, snippet, memories)
            Hero()
            Actions(onLaunch, onOpenMemory, onOpenTools)
            ModuleList(features)
            WorldList(worlds)
        }
    }
}

@Composable
private fun StatusCard(modeLabel: String, snippet: String, memories: List<MemoryEntity>) {
    val context = LocalContext.current
    val storage = remember(memories.size, memories.sumOf { it.content.length }) {
        MemoryStorage.probe(context)
    }
    NeonCard {
        Text("Copilot", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Mode: $modeLabel", color = NeonLime, fontWeight = FontWeight.Bold)
        Text("Locale en-AU · Australia/Perth", color = Color(0xFFBFE8FF))
        Text(storage.label, color = NeonMagenta, fontWeight = FontWeight.Bold)
        storage.warning?.let { warning ->
            Text(warning, color = NeonLime, fontWeight = FontWeight.Bold)
        }
        Text("No expiry. Note count is not capped.", color = Color(0xFFBFE8FF))
        Text("Last reply: $snippet", color = Color(0xFFEAFBFF))
        Text("Live memory", color = NeonCyan, fontWeight = FontWeight.Black)
        if (memories.isEmpty()) {
            Text("Nothing saved yet. Chat /remember keeps a note on this phone.", color = Color(0xFFBFE8FF))
        } else {
            memories.take(5).forEach { note ->
                Text("• [${com.cat.data.TruthTag.normalize(note.truthTag)}] ${note.content}", color = NeonLime, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun NeonCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(20.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(NeonCyan, NeonMagenta, NeonLime)),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) { content() }
}

@Composable
private fun Hero() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "C@T", color = NeonCyan, fontSize = 42.sp, fontWeight = FontWeight.Black)
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
                Text("Cyber AI Assistant", color = NeonCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
        Text("Modules", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
        Text("Worlds", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
        worlds.forEach { world ->
            NeonCard {
                Text(world.name, color = NeonCyan, fontWeight = FontWeight.Bold)
                Text("${world.progress}% progress", color = Color(0xFFBFE8FF))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(99.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((world.progress.coerceIn(0, 100)) / 100f)
                            .height(6.dp)
                            .background(NeonLime, RoundedCornerShape(99.dp))
                    )
                }
                Text(world.mode, color = NeonLime, fontWeight = FontWeight.Bold)
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
