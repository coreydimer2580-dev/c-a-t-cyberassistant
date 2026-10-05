package com.cat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ai.AiPersona
import com.cat.ui.theme.Mist
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import com.cat.ui.theme.Panel
import com.cat.ui.theme.Paper
import com.cat.ui.theme.neonCard

@Composable
fun WheelScreen(wide: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as CAtApplication
    var persona by remember { mutableStateOf(app.prefs.persona) }
    var group by remember { mutableStateOf(app.prefs.groupSolver) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("AI Wheel", color = NeonCyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            "Spin to pick who answers next. Offline C@T always works. Cloud needs a free endpoint in Settings.",
            color = Mist,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        AiWheel(
            selected = persona,
            onSelect = {
                persona = it
                app.prefs.persona = it
            },
            size = if (wide) 300.dp else 260.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .neonCard(accent = NeonMagenta, shape = RoundedCornerShape(16.dp), fill = Panel, glow = 14.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Group solver", color = NeonMagenta, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(
                    "2–3 AIs answer in parallel, then one merged verdict. Works offline.",
                    color = Mist,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = group,
                onCheckedChange = {
                    group = it
                    app.prefs.groupSolver = it
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = NeonLime,
                    uncheckedThumbColor = Paper,
                    uncheckedTrackColor = Color(0xFF333333)
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .neonCard(accent = NeonLime, shape = RoundedCornerShape(16.dp), fill = Panel, glow = 12.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Active", color = NeonLime, fontWeight = FontWeight.Black)
            Text(persona.label, color = Paper, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(persona.styleHint, color = Mist, fontSize = 13.sp)
            if (group) {
                val roster = AiPersona.groupRoster.joinToString(" · ") { it.shortLabel }
                Text("Group roster: $roster (+ Cloud if mode allows)", color = NeonCyan, fontSize = 12.sp)
            }
        }
    }
}
