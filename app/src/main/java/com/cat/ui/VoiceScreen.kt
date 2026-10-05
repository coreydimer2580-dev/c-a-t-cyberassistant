package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.model.VoiceProfile
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.neonCard
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

@Composable
fun VoiceScreen(profiles: List<VoiceProfile>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Voice Profiles", color = NeonCyan, fontSize = 28.sp)
        Text(
            "Profile presets only. The microphone is not used in this build.",
            color = Color(0xFFBFE8FF)
        )

        profiles.forEach { profile ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonCard(accent = NeonMagenta, shape = RoundedCornerShape(16.dp), fill = Color(0xFF111821), glow = 14.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Text(profile.name, color = Color(0xFFEAFBFF))
                    Text("Tone: ${profile.tone}", color = Color(0xFFBFE8FF))
                    Text("Speed: ${profile.speed}", color = NeonLime)
                    Text(
                        if (profile.enabled) "Selected" else "Standby",
                        color = NeonMagenta
                    )
                }
            }
        }
    }
}
