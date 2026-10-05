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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.BuildConfig
import com.cat.CAtApplication
import com.cat.data.TerminalLine
import com.cat.security.SoftCorrect
import com.cat.security.TerminalLock
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TerminalScreen(app: CAtApplication) {
    var unlocked by remember { mutableStateOf(app.terminalLock.unlocked) }
    if (!unlocked) {
        TerminalLockGate(app) { unlocked = true }
    } else {
        TerminalConsole(app) { unlocked = false }
    }
}

@Composable
private fun TerminalLockGate(app: CAtApplication, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Terminal is locked. Enter the PIN.") }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }
    val wait = app.terminalLock.cooldownSeconds()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Terminal 🔐", color = NeonCyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            "C@T ${BuildConfig.VERSION_NAME} · separate from Chat · encrypted on this phone",
            color = NeonLime,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
        Text(
            "English AI only. Not a system shell. No self-update.",
            color = Color(0xFFBFE8FF),
            fontFamily = FontFamily.Monospace
        )
        if (tick >= 0 && wait > 0) {
            Text("Too many tries. Wait ${wait}s.", color = NeonMagenta, fontFamily = FontFamily.Monospace)
        }
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                message = submitPin(app, pin, onUnlocked)
                if (!app.terminalLock.unlocked) pin = ""
            }),
            singleLine = true
        )
        Button(
            onClick = {
                message = submitPin(app, pin, onUnlocked)
                if (!app.terminalLock.unlocked) pin = ""
            },
            enabled = wait == 0
        ) { Text("Unlock") }
        Text(message, color = NeonMagenta, fontFamily = FontFamily.Monospace)
    }
}

private fun submitPin(app: CAtApplication, pin: String, onUnlocked: () -> Unit): String {
    return when (val result = app.terminalLock.tryUnlock(pin)) {
        TerminalLock.Unlock.Ok -> {
            onUnlocked()
            "Unlocked for this session."
        }
        TerminalLock.Unlock.BadFormat -> "PIN is 4 to 8 digits."
        is TerminalLock.Unlock.Wrong -> "Wrong PIN. ${result.left} tries left."
        is TerminalLock.Unlock.Wait -> "Locked for ${result.seconds}s."
    }
}

@Composable
private fun TerminalConsole(app: CAtApplication, onLock: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var lines by remember { mutableStateOf<List<TerminalLine>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val loaded = app.terminalVault.load()
        lines = if (loaded.isEmpty()) {
            val banner = listOf(
                TerminalLine(
                    "sys",
                    "C@T ${BuildConfig.VERSION_NAME} · English terminal · not a shell · build ${BuildConfig.VERSION_CODE}",
                    System.currentTimeMillis()
                )
            )
            app.terminalVault.save(banner)
            banner
        } else {
            loaded
        }
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    fun send() {
        val raw = draft
        if (busy || raw.isBlank()) return
        draft = ""
        busy = true
        scope.launch {
            val fixed = SoftCorrect.apply(raw)
            val now = System.currentTimeMillis()
            val prior = lines.filter { it.role == "you" || it.role == "cat" }
                .map { (if (it.role == "you") "user" else "assistant") to it.text }
            val next = lines.toMutableList()
            if (fixed.changed && fixed.note != null) {
                next += TerminalLine("sys", fixed.note, now)
            }
            next += TerminalLine("you", fixed.text, now + 1)
            val answer = runCatching {
                app.copilot.answerTerminal(fixed.text, prior)
            }.getOrElse {
                com.cat.data.CopilotRepository.TerminalAnswer(
                    it.message ?: "Terminal reply failed.",
                    null
                )
            }
            next += TerminalLine("cat", answer.reply, System.currentTimeMillis())
            lines = next
            notice = answer.notice
            app.terminalVault.save(lines)
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050605))
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "C@T> v${BuildConfig.VERSION_NAME}",
                color = NeonLime,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = {
                app.terminalLock.lock()
                onLock()
            }) { Text("Lock", color = NeonMagenta) }
        }
        Text(
            "Encrypted apart from Chat. English commands. Offline, or cloud if you set one.",
            color = Color(0xFF8FB8A0),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0A120C), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(lines) { line ->
                val color = when (line.role) {
                    "you" -> NeonCyan
                    "sys" -> NeonMagenta
                    else -> NeonLime
                }
                val prefix = when (line.role) {
                    "you" -> "you>"
                    "sys" -> "sys>"
                    else -> "C@T>"
                }
                Text(
                    "$prefix ${line.text}",
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
            if (busy) {
                item {
                    Text("C@T> …", color = NeonLime, fontFamily = FontFamily.Monospace)
                }
            }
        }
        if (!notice.isNullOrBlank()) {
            Text(notice!!, color = NeonMagenta, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                label = { Text("C@T>") },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = NeonLime
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                singleLine = true
            )
            Button(onClick = { send() }, enabled = !busy) { Text("Run") }
        }
    }
}
