package com.cat.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ai.CopilotMode
import com.cat.data.ChatMessage
import com.cat.data.MemoryEntity
import com.cat.tools.PhoneIntents
import com.cat.ui.theme.CatWordmark
import com.cat.ui.theme.Mist
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import com.cat.ui.theme.Panel
import com.cat.ui.theme.Paper
import com.cat.ui.theme.neonCard
import kotlinx.coroutines.delay
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
    var streamId by remember { mutableLongStateOf(-1L) }
    val memories by app.database.memoryDao().observeAll().collectAsState(initial = emptyList())

    fun apply(turn: com.cat.data.CopilotRepository.Turn, animate: Boolean) {
        messages = turn.messages
        notice = turn.notice
        val last = turn.messages.lastOrNull()
        streamId = if (animate && last?.role == "assistant") last.id else -1L
        val phone = turn.phone
        val dial = phone?.dial
        val number = phone?.number
        if (dial != null && number != null) {
            val intent = if (dial) PhoneIntents.dialIntent(number) else PhoneIntents.smsIntent(number, phone.body)
            val problem = PhoneIntents.launch(context, intent)
            if (problem != null) notice = problem
        }
    }

    fun send() {
        val text = draft
        if (busy || text.isBlank()) return
        draft = ""
        busy = true
        scope.launch {
            val turn = runCatching { app.copilot.send(text) }.getOrElse {
                com.cat.data.CopilotRepository.Turn(
                    app.copilot.history(),
                    it.message ?: "Send failed"
                )
            }
            apply(turn, animate = true)
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        messages = app.copilot.history()
    }
    LaunchedEffect(messages.size, busy) {
        val count = messages.size + if (busy) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Row(
        modifier = Modifier.fillMaxSize(),
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
                TextButton(onClick = onBack) { Text("Back", color = NeonMagenta, fontWeight = FontWeight.Bold) }
                CatWordmark(size = 40.sp)
                Text(mode.label, color = NeonLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
                "Offline default · Australia/Perth · en-AU · memory stays on this phone",
                color = Mist,
                fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopilotMode.entries.forEach { item ->
                    TextButton(onClick = {
                        mode = item
                        app.prefs.mode = item
                    }) {
                        Text(
                            item.label,
                            color = if (mode == item) Color.Black else NeonCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (mode == item) NeonCyan else Color(0xFF102228))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (!wide) {
                LiveMemoryRail(memories)
            }
            if (!notice.isNullOrBlank()) {
                Text(
                    text = notice.orEmpty(),
                    color = Paper,
                    modifier = Modifier
                        .fillMaxWidth()
                        .neonCard(accent = NeonMagenta, shape = RoundedCornerShape(12.dp), fill = Color(0xFF3A1030), glow = 12.dp)
                        .padding(10.dp)
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    Bubble(message, stream = message.id == streamId && message.role == "assistant")
                }
                if (busy) {
                    item { ThinkingBubble() }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("/help", "/remember ", "/recall", "/tools").forEach { command ->
                    TextButton(onClick = { draft = command }) {
                        Text(command.trim(), color = NeonLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
            Composer(draft = draft, enabled = !busy, onDraft = { draft = it }, onSend = { send() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            val turn = runCatching { app.copilot.regenerate() }.getOrElse {
                                com.cat.data.CopilotRepository.Turn(messages, it.message ?: "Regenerate failed")
                            }
                            apply(turn, animate = true)
                            busy = false
                        }
                    },
                    enabled = !busy && messages.any { it.role == "user" }
                ) { Text("Regen", color = NeonCyan) }
                OutlinedButton(
                    onClick = {
                        busy = true
                        scope.launch {
                            apply(app.copilot.clear(), animate = false)
                            busy = false
                        }
                    },
                    enabled = !busy && messages.isNotEmpty()
                ) { Text("Clear", color = NeonMagenta) }
            }
        }
        if (wide) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .weight(0.72f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Copilot", color = NeonCyan, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    "Offline answers on this phone and do not wait for Wi-Fi. /call and /sms open your own dialer or SMS app.",
                    color = Mist
                )
                LiveMemoryRail(memories)
            }
        }
    }
}

@Composable
private fun LiveMemoryRail(memories: List<MemoryEntity>) {
    var flashId by remember { mutableLongStateOf(-1L) }
    val newest = memories.firstOrNull()?.id
    LaunchedEffect(newest) {
        val id = newest ?: return@LaunchedEffect
        flashId = id
        delay(1600)
        if (flashId == id) flashId = -1L
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neonCard(accent = NeonMagenta, shape = RoundedCornerShape(16.dp), fill = Panel, glow = 16.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("C@T hard save", color = NeonMagenta, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text("On this phone. No expiry. Tags are yours.", color = Mist, fontSize = 11.sp)
        if (memories.isEmpty()) {
            Text("Nothing yet. Try /remember tea is at 4", color = Paper, fontSize = 13.sp)
        } else {
            memories.take(8).forEach { note ->
                val hot = note.id == flashId
                Text(
                    text = "[${com.cat.data.TruthTag.normalize(note.truthTag)}] ${note.content}",
                    color = if (hot) Color.Black else Paper,
                    fontWeight = if (hot) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (hot) NeonLime else Color(0xFF12141C))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun Composer(draft: String, enabled: Boolean, onDraft: (String) -> Unit, onSend: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neonCard(accent = NeonCyan, shape = shape, fill = Color(0xFF050508), glow = 18.dp)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            enabled = enabled,
            textStyle = TextStyle(color = Paper, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(NeonCyan),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 4,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text("Ask C@T", color = Color(0xFF6A8A96))
                    }
                    inner()
                }
            }
        )
        TextButton(onClick = onSend, enabled = enabled && draft.isNotBlank()) {
            Text(
                "Send",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (enabled && draft.isNotBlank()) NeonCyan else Color(0xFF1C3036))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun Bubble(message: ChatMessage, stream: Boolean) {
    val fromUser = message.role == "user"
    val full = message.content
    var shown by remember(message.id) { mutableStateOf(if (stream) "" else full) }
    LaunchedEffect(message.id, stream, full) {
        if (!stream) {
            shown = full
            return@LaunchedEffect
        }
        val step = (full.length / 36).coerceIn(1, 8)
        var i = 0
        while (i < full.length) {
            i = (i + step).coerceAtMost(full.length)
            shown = full.take(i)
            delay(16)
        }
    }
    val live = stream && shown.length < full.length
    val shape = if (fromUser) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp)
    }
    val accent = if (fromUser) NeonCyan else NeonLime
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .neonCard(
                    accent = accent,
                    shape = shape,
                    fill = if (fromUser) Color(0xFF042028) else Color(0xFF101808),
                    glow = 14.dp
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (fromUser) "YOU" else "C@T",
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Text(
                text = if (live) "$shown▍" else shown,
                color = Paper,
                fontWeight = FontWeight.Medium
            )
            if (message.filtered) {
                Text("redacted before save", color = NeonMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    val transition = rememberInfiniteTransition(label = "think")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "think-alpha"
    )
    Text(
        text = "C@T  ● ● ●",
        color = NeonCyan.copy(alpha = alpha),
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .neonCard(
                accent = NeonLime,
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
                fill = Color(0xFF101808),
                glow = 12.dp
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    )
}
