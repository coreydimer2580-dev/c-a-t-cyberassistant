package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.ai.SensitiveFilter
import com.cat.model.FeatureToggle
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

@Composable
fun SettingsScreen(
    features: List<FeatureToggle>,
    wide: Boolean,
    onToggle: (Int) -> Unit
) {
    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterDemo()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleList(features, onToggle)
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
            FilterDemo()
            ToggleList(features, onToggle)
        }
    }
}

@Composable
private fun FilterDemo() {
    val filter = remember { SensitiveFilter() }
    var draft by remember { mutableStateOf("") }
    var redacted by remember { mutableStateOf("") }
    Text("Settings and privacy", color = NeonCyan, fontSize = 28.sp)
    Text(
        "Paste text to redact emails, SSNs, card numbers, phone numbers, and secret keywords. Only this field is read.",
        color = Color(0xFFBFE8FF)
    )
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Text to filter") }
    )
    Button(onClick = { redacted = filter.sanitize(draft) }) {
        Text("Redact")
    }
    if (redacted.isNotEmpty()) {
        Text(redacted, color = NeonLime)
    }
}

@Composable
private fun ToggleList(features: List<FeatureToggle>, onToggle: (Int) -> Unit) {
    Text("Modules", color = NeonCyan, fontSize = 22.sp)
    features.forEachIndexed { index, feature ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111821), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(feature.name, color = Color(0xFFEAFBFF), modifier = Modifier.weight(1f))
            Switch(
                checked = feature.enabled,
                onCheckedChange = { onToggle(index) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = NeonLime,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = NeonMagenta
                )
            )
        }
    }
}
