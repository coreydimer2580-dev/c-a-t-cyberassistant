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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.model.FeatureToggle
import com.cat.model.WorldState
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

@Composable
fun DashboardScreen(
    features: List<FeatureToggle>,
    worlds: List<WorldState>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "C@T",
            color = NeonCyan,
            fontSize = 42.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D14), RoundedCornerShape(24.dp))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(NeonCyan, NeonMagenta, NeonLime)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cyber AI Assistant", color = NeonCyan, fontSize = 24.sp)
                Text("Offline-first shell • Private • On this device", color = Color(0xFFBFE8FF))
                Text(
                    "Local notes, UI modules, and a paste-in privacy filter. No device scan.",
                    color = NeonLime
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton("Launch", NeonCyan, Color.Black)
            ActionButton("Search", NeonMagenta, Color.White)
            ActionButton("Memory", NeonLime, Color.Black)
        }

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
    textColor: Color
) {
    Button(
        onClick = {},
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor
        )
    ) {
        Text(label)
    }
}
