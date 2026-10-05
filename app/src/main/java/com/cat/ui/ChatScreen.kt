package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ai.CopilotMode
import com.cat.data.ChatMessage
import com.cat.tools.PhoneIntents
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(onBack: () -> Unit, wide: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as CAtApplication
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(app.prefs.mode) }

    fun apply(turn: com.cat.data.CopilotRepository.Turn) {
        messages = turn.messages
        notice = turn.notice
        val phone = turn.phone
        val dial = phone?.dial
        val number = phone?.number
        if (dial != null && number != null) {
            val intent = if (dial) PhoneIntents.dialIntent(number) else PhoneIntents.smsIntent(number, phone.body)
            val problem = PhoneIntents.launch(context, intent)
            if (problem != null) notice = problem
        }
    }

    LaunchedEffect(Unit) {
        messages = app.copilot.history()
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) { Text("Back") }
                Text("C@T", color = NeonCyan, fontSize = 28.sp)
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 8.dp).size(22.dp),
                        color = NeonCyan,
                        strokeWidth = 2.dp
                    )
                }
            }
            Text(
                "Offline default · Australia/Perth · en-AU · does not wait on Wi-Fi",
                color = Color(0xFFBFE8FF),
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopilotMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = {
                            mode = item
                            app.prefs.mode = item
                        },
                        label = { Text(item.label) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("/help", "/recall", "/tools", "/time").forEach { command ->
                    OutlinedButton(onClick = { draft = command }) { Text(command) }
                }
            }
            if (!notice.isNullOrBlank()) {
                Text(
                    text = notice.orEmpty(),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A1030), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    Bubble(message)
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Message C@T") },
                enabled = !busy
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val text = draft
                        draft = ""
                        busy = true
                        scope.launch {
                            val turn = runCatching { app.copilot.send(text) }.getOrElse {
                                app.copilot.history().let { history ->
                                    com.cat.data.CopilotRepository.Turn(
                                        history,
                                        it.message ?: "Send failed"
                                    )
                                }
                            }
                            apply(turn)
                            busy = false
                        }
                    },
                    enabled = !busy && draft.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Send") }
                OutlinedButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            val turn = runCatching { app.copilot.regenerate() }.getOrElse {
                                com.cat.data.CopilotRepository.Turn(messages, it.message ?: "Regenerate failed")
                            }
                            apply(turn)
                            busy = false
                        }
                    },
                    enabled = !busy && messages.any { it.role == "user" }
                ) { Text("Regen") }
                OutlinedButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            val turn = app.copilot.clear()
                            apply(turn)
                            busy = false
                        }
                    },
                    enabled = !busy && messages.isNotEmpty()
                ) { Text("Clear") }
            }
        }
        if (wide) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .weight(0.7f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Copilot", color = NeonCyan, fontSize = 22.sp)
                Text("Mode: ${mode.label}", color = NeonLime)
                Text(
                    "Offline answers on this phone and does not wait for Wi-Fi. Cloud and Auto use the network only when it is available, then fall back offline. /call and /sms open your own dialer or SMS app.",
                    color = Color(0xFFBFE8FF)
                )
                Text(
                    "Secrets in what you type are redacted before they are saved or sent.",
                    color = NeonMagenta
                )
            }
        }
    }
}

@Composable
private fun Bubble(message: ChatMessage) {
    val fromUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(
                    if (fromUser) Color(0xFF10242A) else Color(0xFF10160F),
                    RoundedCornerShape(16.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (fromUser) "You" else "C@T",
                color = if (fromUser) NeonCyan else NeonLime,
                fontSize = 12.sp
            )
            Text(message.content, color = Color(0xFFEAFBFF))
            if (message.filtered) {
                Text("redacted before save", color = NeonMagenta, fontSize = 11.sp)
            }
        }
    }
}
