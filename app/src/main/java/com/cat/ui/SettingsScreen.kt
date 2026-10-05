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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ProductGuide
import com.cat.ai.CopilotMode
import com.cat.ai.SensitiveFilter
import com.cat.data.CopilotPrefs
import com.cat.model.FeatureToggle
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.neonCard
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

@Composable
fun SettingsScreen(
    features: List<FeatureToggle> = emptyList(),
    wide: Boolean,
    onToggle: (Int) -> Unit = {},
    onOpenTools: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HowItFits()
        CopilotSettings()
        TerminalPinSettings()
        if (onOpenTools != null) {
            OutlinedButton(onClick = onOpenTools) {
                Text("More tools (/call · /sms · notes)", color = NeonCyan)
            }
        }
        FilterDemo()
        if (features.isNotEmpty()) {
            ToggleList(features, onToggle)
        }
    }
}


@Composable
private fun HowItFits() {
    Text("One path", color = NeonCyan, fontSize = 28.sp)
    Text(ProductGuide.PATH, color = NeonLime, fontSize = 16.sp)
    Text(ProductGuide.SPEECH, color = Color(0xFFBFE8FF))
    Text(ProductGuide.SAFE, color = NeonMagenta)
    ProductGuide.surfaces().forEach { surface ->
        Text(surface.name, color = NeonCyan, fontSize = 16.sp)
        Text(surface.line, color = Color(0xFFBFE8FF))
    }
}

@Composable
private fun TerminalPinSettings() {
    val app = LocalContext.current.applicationContext as CAtApplication
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    Text("Terminal lock", color = NeonCyan, fontSize = 22.sp)
    Text(
        "Terminal is its own screen. The PIN is stored as a salted hash in encrypted preferences. The transcript is encrypted apart from Chat. Change it here any time (4–8 digits).",
        color = Color(0xFFBFE8FF)
    )
    OutlinedTextField(
        value = current,
        onValueChange = { current = it.filter { ch -> ch.isDigit() }.take(8) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Current PIN") },
        visualTransformation = PasswordVisualTransformation()
    )
    OutlinedTextField(
        value = next,
        onValueChange = { next = it.filter { ch -> ch.isDigit() }.take(8) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("New PIN") },
        visualTransformation = PasswordVisualTransformation()
    )
    OutlinedTextField(
        value = confirm,
        onValueChange = { confirm = it.filter { ch -> ch.isDigit() }.take(8) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Confirm new PIN") },
        visualTransformation = PasswordVisualTransformation()
    )
    Button(onClick = {
        message = when (val result = app.terminalLock.changePin(current, next, confirm)) {
            com.cat.security.TerminalLock.Change.Ok -> {
                current = ""; next = ""; confirm = ""
                "PIN updated. Terminal is locked until you unlock it."
            }
            com.cat.security.TerminalLock.Change.BadFormat -> "Use 4 to 8 digits."
            com.cat.security.TerminalLock.Change.Mismatch -> "New PIN and confirm do not match."
            is com.cat.security.TerminalLock.Change.Wrong -> "Current PIN was wrong. ${result.left} tries left."
            is com.cat.security.TerminalLock.Change.Wait -> "Too many tries. Wait ${result.seconds}s."
        }
    }) { Text("Change PIN") }
    if (message.isNotEmpty()) Text(message, color = NeonLime)
}

@Composable
private fun FilterDemo() {
    val filter = remember { SensitiveFilter() }
    var draft by remember { mutableStateOf("") }
    var redacted by remember { mutableStateOf("") }
    Text("Privacy filter", color = NeonCyan, fontSize = 22.sp)
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
                .neonCard(
                    accent = if (feature.enabled) NeonLime else NeonMagenta,
                    shape = RoundedCornerShape(16.dp),
                    fill = Color(0xFF111821),
                    glow = 10.dp
                )
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

    Text("How C@T answers", color = NeonCyan, fontSize = 28.sp)
    Text(
        "Offline is the default and needs nothing. Try Cloud is optional and only works with your own endpoint.",
        color = Color(0xFFBFE8FF)
    )
    Button(
        onClick = {
            mode = CopilotMode.OFFLINE
            prefs.mode = CopilotMode.OFFLINE
            saved = "Offline only. No cloud."
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Use Offline only", fontSize = 18.sp) }
    Button(
        onClick = {
            mode = CopilotMode.AUTO
            prefs.mode = CopilotMode.AUTO
            saved = if (!prefs.cloudReady) {
                "No cloud saved, so replies stay offline. That's fine — Offline works with none."
            } else {
                "Try Cloud is on. C@T uses your endpoint, and quietly answers offline if it can't reach it."
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Try Cloud (optional)", fontSize = 18.sp) }
    Text(
        if (mode == CopilotMode.OFFLINE) "Now: offline only." else if (prefs.cloudReady) "Now: try your cloud, offline fallback." else "Now: offline (no cloud saved).",
        color = NeonLime
    )
    Text(
        "Online evolve is a Chat switch. It runs once when you send, never while the app sleeps, and it never updates this app. Autopilot speech is Chat only — Terminal does not speak.",
        color = NeonCyan
    )
    Text(
        "Notes stay on this phone until you clear them. You set True, False, or Unsure.",
        color = Color(0xFFBFE8FF)
    )
    var showCloud by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showCloud = !showCloud }, modifier = Modifier.fillMaxWidth()) {
        Text(if (showCloud) "Hide cloud address" else "Cloud address (optional)")
    }
    if (!showCloud) {
        if (saved.isNotEmpty()) Text(saved, color = NeonLime)
        return
    }
    OutlinedTextField(
        value = baseUrl,
        onValueChange = { baseUrl = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Base URL") },
        placeholder = { Text(CopilotPrefs.URL_HINT) }
    )
    Text(
        "Needs your own free endpoint (e.g. a Groq or OpenRouter key you add). Offline works with none. C@T ships with no key.",
        color = Color(0xFFBFE8FF),
        fontSize = 12.sp
    )
    OutlinedTextField(
        value = apiKey,
        onValueChange = { apiKey = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("API key (optional)") },
        visualTransformation = PasswordVisualTransformation()
    )
    OutlinedTextField(
        value = model,
        onValueChange = { model = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Model") }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            prefs.apiKey = apiKey
            prefs.model = model
            saved = when {
                baseUrl.isBlank() -> { prefs.baseUrl = ""; "No cloud address. Offline only." }
                CopilotPrefs.isLocalOnlyUrl(baseUrl) -> {
                    prefs.baseUrl = ""
                    baseUrl = ""
                    "That address only works inside an emulator, not on a real phone. Not saved — Offline stays on."
                }
                !CopilotPrefs.looksLikeUrl(baseUrl) -> "That doesn't look like a web address (https://…). Not saved."
                else -> { prefs.baseUrl = baseUrl; "Saved. C@T quietly answers offline if it can't reach it." }
            }
        }) { Text("Save") }
        OutlinedButton(onClick = {
            baseUrl = ""
            apiKey = ""
            prefs.baseUrl = ""
            prefs.apiKey = ""
            mode = CopilotMode.OFFLINE
            prefs.mode = CopilotMode.OFFLINE
            saved = "Cloud cleared. Offline only."
        }) { Text("Clear cloud") }
    }
    if (saved.isNotEmpty()) {
        Text(saved, color = NeonLime)
    }
}
