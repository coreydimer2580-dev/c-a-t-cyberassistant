package com.cat.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ai.AiPersona
import com.cat.ai.CopilotMode
import com.cat.ProductGuide
import com.cat.ai.SpeechHelper
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
fun ChatScreen(wide: Boolean, onOpenWheel: (() -> Unit)? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as CAtApplication
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(app.prefs.mode) }
    var persona by remember { mutableStateOf(app.prefs.persona) }
    var group by remember { mutableStateOf(app.prefs.groupSolver) }
    var autopilot by remember { mutableStateOf(app.prefs.autopilot) }
    var onlineEvolve by remember { mutableStateOf(app.prefs.onlineEvolve) }
    var online by remember { mutableStateOf(isOnline(context)) }
    var streamId by remember { mutableLongStateOf(-1L) }
    var followUp by remember { mutableStateOf<String?>(null) }
    var evolveStatus by remember { mutableStateOf<String?>(null) }
    // One auto follow-up speech per user message; then wait.
    var autoFollowSpent by remember { mutableStateOf(false) }
    val memories by app.database.memoryDao().observeAll().collectAsState(initial = emptyList())
    val speech = remember { SpeechHelper(context) }

    DisposableEffect(Unit) {
        onDispose { speech.shutdown() }
    }

    LaunchedEffect(Unit) {
        messages = app.copilot.history()
        while (true) {
            online = isOnline(context)
            delay(2500)
        }
    }

    fun apply(turn: com.cat.data.CopilotRepository.Turn, animate: Boolean, fromUserSend: Boolean) {
        messages = turn.messages
        notice = turn.notice
        evolveStatus = turn.evolveStatus
        followUp = turn.followUp
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
        persona = app.prefs.persona
        mode = app.prefs.mode
        if (fromUserSend && autopilot && last?.role == "assistant") {
            speech.speak(last.content, flush = true)
            autoFollowSpent = false
            val chip = turn.followUp
            if (!chip.isNullOrBlank()) {
                // One spoken follow-up per user message, then wait.
                speech.speak(chip, flush = false)
                autoFollowSpent = true
            }
        }
    }

    fun send(textOverride: String? = null) {
        val text = textOverride ?: draft
        if (busy || text.isBlank()) return
        if (textOverride == null) draft = ""
        followUp = null
        autoFollowSpent = false
        busy = true
        scope.launch {
            val turn = runCatching { app.copilot.send(text) }.getOrElse {
                com.cat.data.CopilotRepository.Turn(
                    app.copilot.history(),
                    "Couldn't answer that one. Try again."
                )
            }
            apply(turn, animate = true, fromUserSend = true)
            busy = false
        }
    }

    LaunchedEffect(messages.size, busy) {
        val count = messages.size + if (busy) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun regen() {
        busy = true
        scope.launch {
            val turn = runCatching { app.copilot.regenerate() }.getOrElse {
                com.cat.data.CopilotRepository.Turn(messages, "Couldn't regenerate. Try again.")
            }
            apply(turn, animate = true, fromUserSend = false)
            busy = false
        }
    }

    fun newChat() {
        busy = true
        speech.stop()
        followUp = null
        evolveStatus = null
        notice = null
        scope.launch {
            apply(app.copilot.clear(), animate = false, fromUserSend = false)
            busy = false
        }
    }

    // Offline Auto = speak replies + offline group solve + evolve on send. No cloud needed.
    val autoAll = autopilot && group
    var showOptions by remember { mutableStateOf(false) }
    val lastAssistantId = messages.lastOrNull { it.role == "assistant" }?.id

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChatTopBar(
            persona = persona,
            mode = mode,
            online = online,
            autoAll = autoAll,
            busy = busy,
            onAuto = {
                val on = !autoAll
                autopilot = on
                app.prefs.autopilot = on
                group = on
                app.prefs.groupSolver = on
                if (!on) speech.stop()
                notice = if (on) "Auto on · talks, solves with offline voices, evolves on send." else null
            },
            onOptions = { showOptions = !showOptions },
            optionsOpen = showOptions,
            onNewChat = { newChat() }
        )

        if (showOptions) {
            SystemStrip(
                online = online,
                mode = mode,
                persona = persona,
                group = group,
                autopilot = autopilot,
                onlineEvolve = onlineEvolve,
                evolveStatus = evolveStatus,
                onMode = {
                    mode = it
                    app.prefs.mode = it
                    when (it) {
                        CopilotMode.OFFLINE -> {
                            persona = AiPersona.OFFLINE_CAT
                            app.prefs.persona = AiPersona.OFFLINE_CAT
                        }
                        CopilotMode.CLOUD -> {
                            persona = AiPersona.CLOUD_GPT
                            app.prefs.persona = AiPersona.CLOUD_GPT
                        }
                        CopilotMode.AUTO -> {
                            persona = AiPersona.AUTO
                            app.prefs.persona = AiPersona.AUTO
                        }
                    }
                    if (it != CopilotMode.OFFLINE && !app.prefs.cloudReady) {
                        notice = "No cloud saved, so replies stay offline. Add your own endpoint in Settings if you want one."
                    }
                },
                onGroup = {
                    group = it
                    app.prefs.groupSolver = it
                },
                onAutopilot = {
                    autopilot = it
                    app.prefs.autopilot = it
                    if (!it) speech.stop()
                },
                onOnlineEvolve = {
                    onlineEvolve = it
                    app.prefs.onlineEvolve = it
                },
                onOpenWheel = onOpenWheel
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (messages.isEmpty() && !busy) {
                        item { EmptyChatHint(onPick = { send(it) }) }
                    }
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(
                            message = message,
                            stream = message.id == streamId && message.role == "assistant",
                            showActions = message.id == lastAssistantId && !busy,
                            onSpeak = { speech.speak(message.content, flush = true) },
                            onRegen = { regen() }
                        )
                    }
                    if (busy) {
                        item { ThinkingBubble(if (group) "Group" else persona.shortLabel) }
                    }
                }

                if (!notice.isNullOrBlank()) {
                    Text(
                        text = notice.orEmpty(),
                        color = Mist,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }

                if (!followUp.isNullOrBlank()) {
                    Text(
                        text = followUp.orEmpty(),
                        color = Paper,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2228))
                            .clickable(enabled = !busy) {
                                val q = followUp.orEmpty()
                                followUp = null
                                send(q)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                Composer(draft = draft, enabled = !busy, onDraft = { draft = it }, onSend = { send() })
                Text(
                    "C@T runs offline on this phone. It can make mistakes.",
                    color = Color(0xFF6B7380),
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (wide) {
                Column(
                    modifier = Modifier.widthIn(max = 300.dp).weight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Live AI", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        buildString {
                            append(persona.label)
                            append(if (group) " · Group" else " · Single")
                            append(if (online) " · Online" else " · Offline")
                            if (autopilot) append(" · Talk")
                        },
                        color = Mist,
                        fontSize = 12.sp
                    )
                    if (!evolveStatus.isNullOrBlank()) {
                        Text(evolveStatus.orEmpty(), color = NeonLime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    LiveMemoryRail(memories)
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    persona: AiPersona,
    mode: CopilotMode,
    online: Boolean,
    autoAll: Boolean,
    busy: Boolean,
    onAuto: () -> Unit,
    onOptions: () -> Unit,
    optionsOpen: Boolean,
    onNewChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOptions)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                "C@T " + (if (optionsOpen) "▴" else "▾"),
                color = Paper,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                persona.shortLabel + " · " + mode.label + (if (online) "" else " · no network"),
                color = Mist,
                fontSize = 11.sp
            )
        }
        Text(
            text = if (autoAll) "Auto ON" else "Auto",
            color = if (autoAll) Color.Black else Paper,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (autoAll) NeonLime else Color(0xFF1E2228))
                .clickable(onClick = onAuto)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Text(
            text = "✎ New",
            color = Paper,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF1E2228))
                .clickable(enabled = !busy, onClick = onNewChat)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SystemStrip(
    online: Boolean,
    mode: CopilotMode,
    persona: AiPersona,
    group: Boolean,
    autopilot: Boolean,
    onlineEvolve: Boolean,
    evolveStatus: String?,
    onMode: (CopilotMode) -> Unit,
    onGroup: (Boolean) -> Unit,
    onAutopilot: (Boolean) -> Unit,
    onOnlineEvolve: (Boolean) -> Unit,
    onOpenWheel: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neonCard(accent = NeonCyan, shape = RoundedCornerShape(14.dp), fill = Panel, glow = 12.dp)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(if (online) "Online" else "Offline", if (online) NeonLime else NeonMagenta)
            StatusChip(persona.shortLabel, NeonCyan)
            if (!evolveStatus.isNullOrBlank()) {
                StatusChip(evolveStatus.take(28), NeonLime)
            }
            StatusChip(
                label = if (autopilot) "Speak ON · Chat" else "Speak · Chat",
                accent = if (autopilot) NeonLime else Mist,
                selected = autopilot,
                onClick = { onAutopilot(!autopilot) }
            )
            StatusChip(
                label = if (onlineEvolve) "Online evolve ON" else "Online evolve",
                accent = if (onlineEvolve) NeonCyan else Mist,
                selected = onlineEvolve,
                onClick = { onOnlineEvolve(!onlineEvolve) }
            )
            StatusChip(
                label = if (group) "Group ON" else "Group",
                accent = if (group) NeonMagenta else Mist,
                selected = group,
                onClick = { onGroup(!group) }
            )
            if (onOpenWheel != null) {
                StatusChip("Wheel", NeonCyan, onClick = onOpenWheel)
            }
        }
        Text(
            "Auto = talk + offline group solve + evolve on send. Cloud is optional and only used if you saved your own endpoint.",
            color = Color(0xFF8FB8A0),
            fontSize = 11.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CopilotMode.entries.forEach { item ->
                StatusChip(
                    label = item.label,
                    accent = if (mode == item) NeonLime else Mist,
                    selected = mode == item,
                    onClick = { onMode(item) }
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    accent: Color,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = label,
        color = if (selected) Color.Black else accent,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accent else Color(0xFF121820))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun EmptyChatHint(onPick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CatWordmark(size = 40.sp)
        Text("What can I help with?", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Works offline. No account, no key needed.", color = Mist, fontSize = 12.sp)
        listOf(
            "Plan my day",
            "Give me options for dinner",
            "Explain this simply",
            "/remember tea is at 4"
        ).forEach { prompt ->
            Text(
                text = prompt,
                color = Paper,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF171A1F))
                    .clickable { onPick(prompt) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
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
            .neonCard(accent = NeonMagenta, shape = RoundedCornerShape(16.dp), fill = Panel, glow = 14.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Memory", color = NeonMagenta, fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text("On this phone. Feeds the AI.", color = Mist, fontSize = 11.sp)
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
    val canSend = enabled && draft.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xFF1E2228))
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraft,
            enabled = enabled,
            textStyle = TextStyle(color = Paper, fontSize = 16.sp),
            cursorBrush = SolidColor(NeonLime),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            maxLines = 6,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text("Message C@T", color = Color(0xFF7A828E), fontSize = 16.sp)
                    }
                    inner()
                }
            }
        )
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (canSend) Paper else Color(0xFF3A3F47))
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Text("↑", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    stream: Boolean,
    showActions: Boolean,
    onSpeak: () -> Unit,
    onRegen: () -> Unit
) {
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
    val clipboard = LocalClipboardManager.current
    var copied by remember(message.id) { mutableStateOf(false) }

    if (fromUser) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = shown,
                color = Paper,
                fontSize = 15.sp,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2A2F37))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
        return
    }

    val who = when {
        message.personaId == "group" -> "C@T · Group"
        message.personaId.isNotBlank() -> "C@T · " + AiPersona.fromId(message.personaId).shortLabel
        else -> "C@T"
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (message.personaId == "group") NeonMagenta else NeonLime),
            contentAlignment = Alignment.Center
        ) {
            Text("@", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(who, color = Mist, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (live) "$shown ●" else shown,
                color = Paper,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            if (message.filtered) {
                Text("Private details redacted before save", color = NeonMagenta, fontSize = 11.sp)
            }
            if (showActions && !live) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ActionText(if (copied) "Copied" else "Copy") {
                        clipboard.setText(AnnotatedString(full))
                        copied = true
                    }
                    ActionText("Speak", onSpeak)
                    ActionText("Regenerate", onRegen)
                }
            }
        }
    }
}

@Composable
private fun ActionText(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color(0xFF9AA3AE),
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun ThinkingBubble(who: String) {
    val transition = rememberInfiniteTransition(label = "think")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "think-alpha"
    )
    Text(
        text = "$who is thinking ● ● ●",
        color = Mist.copy(alpha = alpha),
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 38.dp)
    )
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
