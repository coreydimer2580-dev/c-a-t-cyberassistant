package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.ai.SensitiveFilter
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime

private data class ChatLine(val fromUser: Boolean, val text: String)

@Composable
fun ChatScreen(onBack: () -> Unit) {
    val filter = remember { SensitiveFilter() }
    val lines = remember { mutableStateListOf<ChatLine>() }
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Text(
                "Assistant",
                color = NeonCyan,
                fontSize = 28.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Text(
            "Typed text stays in this screen. Sensitive patterns are redacted before the reply.",
            color = Color(0xFFBFE8FF)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(lines) { line ->
                Text(
                    text = line.text,
                    color = if (line.fromUser) Color(0xFFEAFBFF) else NeonLime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (line.fromUser) Color(0xFF111821) else Color(0xFF0D1A14),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                )
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Message") }
        )
        Button(
            onClick = {
                val filtered = filter.sanitize(draft.trim())
                if (filtered.isNotEmpty()) {
                    lines.add(ChatLine(true, filtered))
                    lines.add(ChatLine(false, "C@T online — filtered your input\n" + filtered))
                    draft = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }
    }
}
