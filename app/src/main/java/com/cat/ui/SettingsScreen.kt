package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SettingsScreen(features: List<FeatureToggle>) {
    val filter = remember { SensitiveFilter() }
    var draft by remember { mutableStateOf("") }
    var redacted by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings and privacy", color = NeonCyan, fontSize = 28.sp)
        Text(
            "Paste text below to redact emails, SSNs, card numbers, phone numbers, and secret keywords. This only sees what you type.",
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

        features.forEach { feature ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111821), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(feature.name, color = Color(0xFFEAFBFF))
                    Text(
                        if (feature.enabled) "Enabled" else "Disabled",
                        color = if (feature.enabled) NeonLime else NeonMagenta
                    )
                }
            }
        }
    }
}
