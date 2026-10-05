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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ai.CopilotMode
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
                CopilotSettings()
                CopilotSettings()
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

@Composable
private fun CopilotSettings() {
    val app = LocalContext.current.applicationContext as CAtApplication
    val prefs = app.prefs
    var mode by remember { mutableStateOf(prefs.mode) }
    var baseUrl by remember { mutableStateOf(prefs.baseUrl) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var model by remember { mutableStateOf(prefs.model) }
    var saved by remember { mutableStateOf("") }

    Text("Copilot", color = NeonCyan, fontSize = 28.sp)
    Text(
        if (prefs.encrypted) {
            "API key is stored in encrypted preferences on this device."
        } else {
            "Encryption unavailable. The API key is in plain app preferences. Do not use a valuable key."
        },
        color = if (prefs.encrypted) NeonLime else NeonMagenta
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CopilotMode.entries.forEach { item ->
            FilterChip(
                selected = mode == item,
                onClick = {
                    mode = item
                    prefs.mode = item
                    saved = "Mode ${item.label}"
                },
                label = { Text(item.label) }
            )
        }
    }
    Text(
        "Offline stays on device. Cloud calls your URL. Auto tries cloud, then offline.",
        color = Color(0xFFBFE8FF)
    )
    OutlinedTextField(
        value = baseUrl,
        onValueChange = { baseUrl = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Base URL") },
        placeholder = { Text("https://api.openai.com/v1") }
    )
    OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("API key") },
        visualTransformation = PasswordVisualTransformation()
    )
    OutlinedTextField(
        value = model,
        onValueChange = { model = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Model") }
    )
    Button(onClick = {
        prefs.baseUrl = baseUrl
        prefs.apiKey = apiKey
        prefs.model = model
        saved = "Cloud settings saved"
    }) { Text("Save cloud settings") }
    if (saved.isNotEmpty()) {
        Text(saved, color = NeonLime)
    }
}
