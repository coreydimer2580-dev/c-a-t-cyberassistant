package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.data.CopilotPrefs
import com.cat.data.CopilotRepository
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

/**
 * v1.15 Online tab (v1.18: Use Gemini = key-only preset). ChatGPT-style chat that never asks for a login or key.
 * No cloud saved -> Offline English AI answers (same brain as Chat).
 * Cloud saved (your URL + key) -> labelled "Optional cloud", with offline fallback.
 */
@Composable
fun OnlineScreen(wide: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as CAtApplication
    val repo = app.copilot
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(repo.onlineHistory()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var evolveStatus by remember { mutableStateOf<String?>(null) }
    var streamId by remember { mutableLongStateOf(-1L) }
    var cloudReady by remember { mutableStateOf(repo.onlineCloudReady) }
    var cloudHost by remember { mutableStateOf(repo.onlineCloudHost) }
    var showCloud by remember { mutableStateOf(false) }
    var network by remember { mutableStateOf(isOnline(context)) }
    var askNew by remember { mutableStateOf(false) }
    if (askNew) {
        ConfirmClearDialog(
            title = "Start a new Online chat?",
            body = "This clears the Online conversation on this phone. Chat, Terminal, and Memory stay.",
            confirmLabel = "Clear",
            onConfirm = {
                messages = repo.clearOnline().messages
                notice = null
                evolveStatus = null
            },
            onDismiss = { askNew = false }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            network = isOnline(context)
            delay(2500)
        }
    }

    LaunchedEffect(messages.size, busy) {
        val count = messages.size + (if (messages.isEmpty() && !busy) 1 else 0) + (if (busy) 1 else 0)
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun send(textOverride: String? = null) {
        val text = textOverride ?: draft
        if (busy || text.isBlank()) return
        if (textOverride == null) draft = ""
        busy = true
        notice = null
        scope.launch {
            val turn = runCatching { repo.sendOnline(text) }.getOrElse {
                CopilotRepository.OnlineTurn(repo.onlineHistory(), "Couldn't answer that one. Try again.")
            }
            messages = turn.messages
            notice = turn.notice
            evolveStatus = turn.evolveStatus
            val last = turn.messages.lastOrNull()
            streamId = if (last?.role == "assistant") last.id else -1L
            busy = false
        }
    }

    fun refreshCloud() {
        cloudReady = repo.onlineCloudReady
        cloudHost = repo.onlineCloudHost
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showCloud = !showCloud }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    "C@T Online " + (if (showCloud) "▴" else "▾"),
                    color = Paper,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    when {
                        cloudReady && network -> "Optional cloud · $cloudHost"
                        cloudReady -> "Optional cloud · no network, offline answers"
                        else -> "Offline English AI · no login, no key"
                    },
                    color = if (cloudReady && network) NeonCyan else Mist,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "✎ New",
                color = Paper,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF1E2228))
                    .clickable(enabled = !busy) {
                        if (messages.isEmpty()) {
                            messages = repo.clearOnline().messages
                        } else {
                            askNew = true
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        if (showCloud) {
            OptionalCloudCard(
                cloudReady = cloudReady,
                onChanged = { refreshCloud() },
                onClose = { showCloud = false }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (messages.isEmpty() && !busy) {
                item {
                    OnlineEmpty(cloudReady = cloudReady, onPick = { send(it) })
                }
            }
            items(messages, key = { it.id }) { message ->
                OnlineBubble(
                    message = message,
                    stream = message.id == streamId && message.role == "assistant"
                )
            }
            if (busy) {
                item { ThinkingBubble(if (cloudReady && network) "Cloud" else "C@T") }
            }
        }

        if (!evolveStatus.isNullOrBlank()) {
            Text(
                evolveStatus.orEmpty(),
                color = NeonLime,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }
        if (!notice.isNullOrBlank()) {
            Text(
                notice.orEmpty(),
                color = Mist,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }

        Composer(
            draft = draft,
            enabled = !busy,
            onDraft = { draft = it },
            onSend = { send() },
            placeholder = if (cloudReady) "Message C@T Online" else "Message C@T"
        )
        Text(
            if (cloudReady) {
                "Optional cloud uses your own endpoint. If it can't answer, C@T replies offline. It can make mistakes."
            } else {
                "No login or key needed. Answers come from C@T on this phone. It can make mistakes."
            },
            color = Color(0xFF6B7380),
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OnlineEmpty(cloudReady: Boolean, onPick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CatWordmark(size = 40.sp)
        Text("What can I help with?", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            if (cloudReady) "Optional cloud is on. Offline answers if it can't reach it."
            else "Works now. No login, no key. Offline English AI answers.",
            color = Mist,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        listOf(
            "Explain this simply",
            "Help me write a message",
            "Give me 3 ideas for the weekend",
            "Plan my day"
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
private fun OnlineBubble(message: CopilotRepository.OnlineMessage, stream: Boolean) {
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

    if (message.role == "user") {
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

    val clipboard = LocalClipboardManager.current
    var copied by remember(message.id) { mutableStateOf(false) }
    val cloud = message.source == "cloud"
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (cloud) NeonCyan else NeonLime),
            contentAlignment = Alignment.Center
        ) {
            Text("@", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (cloud) "C@T · Optional cloud" else "C@T · Offline",
                color = Mist,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = if (live) "$shown ●" else shown, color = Paper, fontSize = 15.sp, lineHeight = 22.sp)
            if (!live) {
                ActionText(if (copied) "Copied" else "Copy") {
                    clipboard.setText(AnnotatedString(full))
                    copied = true
                }
            }
        }
    }
}

/** Optional. Never required. Paste your own URL + key; emulator/loopback addresses are refused. */
@Composable
private fun OptionalCloudCard(cloudReady: Boolean, onChanged: () -> Unit, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as CAtApplication
    val prefs = app.prefs
    var url by remember { mutableStateOf(prefs.baseUrl) }
    var key by remember { mutableStateOf(prefs.apiKey) }
    var model by remember { mutableStateOf(prefs.model) }
    var status by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Paper,
        unfocusedTextColor = Paper,
        focusedBorderColor = NeonCyan,
        unfocusedBorderColor = Color(0xFF3A3F47),
        focusedLabelColor = NeonCyan,
        unfocusedLabelColor = Mist,
        cursorColor = NeonLime
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neonCard(accent = NeonCyan, shape = RoundedCornerShape(14.dp), fill = Panel, glow = 12.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (cloudReady) "Optional cloud · on" else "Optional cloud · off",
            color = NeonCyan,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
        Text(
            "You don't need this. Online already answers with Offline English AI, no login or key. " +
                "Tap Use Gemini and paste only your own Gemini API key, or paste any OpenAI-compatible URL + key.",
            color = Mist,
            fontSize = 11.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Use Gemini", NeonCyan) {
                url = CopilotPrefs.GEMINI_BASE_URL
                if (!model.trim().lowercase().startsWith("gemini")) model = CopilotPrefs.GEMINI_MODEL
                status = "Gemini address filled. Paste your own key from Google AI Studio, then Save."
            }
            if (CopilotPrefs.isGeminiUrl(url)) {
                Text("Gemini · key only", color = NeonLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            singleLine = true,
            label = { Text("Base URL (optional)") },
            placeholder = { Text(CopilotPrefs.URL_HINT, color = Color(0xFF5A626E)) },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            singleLine = true,
            label = { Text(if (CopilotPrefs.isGeminiUrl(url)) "Gemini API key (yours)" else "API key (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            singleLine = true,
            label = { Text("Model") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill("Save", NeonLime) {
                val cleanUrl = url.trim()
                status = when {
                    CopilotPrefs.isGeminiUrl(cleanUrl) && key.isBlank() -> {
                        "Paste your Gemini API key first. Nothing changed — Offline keeps answering."
                    }
                    cleanUrl.isBlank() || key.isBlank() -> {
                        "Needs both a URL and a key. Nothing changed — Offline keeps answering."
                    }
                    CopilotPrefs.isLocalOnlyUrl(cleanUrl) -> {
                        url = ""
                        "That address only works in an emulator, not on a phone. Not saved — Offline keeps answering."
                    }
                    !CopilotPrefs.looksLikeUrl(cleanUrl) -> {
                        "That doesn't look like a web address (https://…). Not saved."
                    }
                    else -> {
                        val cleanModel = CopilotPrefs.modelFor(cleanUrl, model)
                        prefs.baseUrl = cleanUrl
                        prefs.apiKey = key
                        prefs.model = cleanModel
                        model = cleanModel
                        if (CopilotPrefs.isGeminiUrl(cleanUrl)) {
                            "Saved. Online uses Gemini with your key and answers offline if it can't reach it."
                        } else {
                            "Saved. Online uses your cloud and answers offline if it can't reach it."
                        }
                    }
                }
                onChanged()
            }
            Pill("Remove cloud", NeonMagenta) {
                url = ""
                key = ""
                prefs.baseUrl = ""
                prefs.apiKey = ""
                status = "Cloud removed. Offline English AI answers."
                onChanged()
            }
            Pill("Close", Mist, onClick = onClose)
        }
        if (status.isNotBlank()) {
            Text(status, color = NeonLime, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Pill(label: String, accent: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = accent,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF121820))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
