package com.cat.ui

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

private val commandChips = listOf("/help", "/recall", "/status")

@Composable
fun TerminalScreen(app: CAtApplication) {
    var unlocked by remember { mutableStateOf(app.terminalLock.unlocked) }
    var privateOn by remember { mutableStateOf(app.prefs.terminalPrivate) }
    PrivateRecents(privateOn)
    if (!unlocked) {
        TerminalLockGate(app, privateOn, onPrivate = {
            privateOn = it
            app.prefs.terminalPrivate = it
        }) { unlocked = true }
    } else {
        TerminalConsole(app, privateOn, onPrivate = {
            privateOn = it
            app.prefs.terminalPrivate = it
        }) { unlocked = false }
    }
}

@Composable
private fun PrivateRecents(privateOn: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(privateOn, activity) {
        val window = activity?.window
        if (privateOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

@Composable
private fun TerminalLockGate(
    app: CAtApplication,
    privateOn: Boolean,
    onPrivate: (Boolean) -> Unit,
    onUnlocked: () -> Unit
) {
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
        PrivateSwitch(privateOn, onPrivate)
        Text(message, color = NeonMagenta, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PrivateSwitch(privateOn: Boolean, onPrivate: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Switch(checked = privateOn, onCheckedChange = onPrivate)
        Text(
            if (privateOn) "Private on · offline only · recents blanked" else "Private off",
            color = if (privateOn) NeonMagenta else Color(0xFF8FB8A0),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
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

private fun welcomeLines(): List<TerminalLine> {
    val now = System.currentTimeMillis()
    val text = """
        C@T ${BuildConfig.VERSION_NAME} · English Terminal · not a shell · build ${BuildConfig.VERSION_CODE}
        Commands: help, remember <fact>, recall, time, status, version, clear, unlock
        Voice defaults to Analyst. Typos on those commands are repaired.
        Each reply uses saved memories plus this encrypted vault.
        Private ON stays offline, skips cloud, and hides this screen in recents.
        Chat stays a separate tab. PIN lock stays on this transcript.
    """.trimIndent()
    return listOf(TerminalLine("sys", text, now))
}

@Composable
private fun TerminalConsole(
    app: CAtApplication,
    privateOn: Boolean,
    onPrivate: (Boolean) -> Unit,
    onLock: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var lines by remember { mutableStateOf<List<TerminalLine>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var face by remember { mutableStateOf("…") }

    suspend fun refreshFace() {
        val snap = runCatching { app.copilot.terminalFace() }.getOrNull()
        face = if (snap == null) {
            "v${BuildConfig.VERSION_NAME}"
        } else if (snap.privateMode) {
            "private · offline · v${BuildConfig.VERSION_NAME} · memories ${snap.memoryCount} · ${snap.voice}"
        } else {
            val net = if (snap.online) "online" else "offline"
            "$net · v${BuildConfig.VERSION_NAME} · memories ${snap.memoryCount} · ${snap.voice}"
        }
    }

    LaunchedEffect(Unit) {
        val loaded = app.terminalVault.load()
        lines = if (loaded.isEmpty()) {
            val banner = welcomeLines()
            app.terminalVault.save(banner)
            banner
        } else {
            loaded
        }
        refreshFace()
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    fun send(rawOverride: String? = null) {
        val raw = rawOverride ?: draft
        if (busy || raw.isBlank()) return
        if (rawOverride == null) draft = ""
        else draft = ""
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
                app.copilot.answerTerminal(
                    fixed.text,
                    prior,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
            }.getOrElse {
                com.cat.data.CopilotRepository.TerminalAnswer(
                    it.message ?: "Terminal reply failed.",
                    null
                )
            }
            val catLine = TerminalLine("cat", answer.reply, System.currentTimeMillis())
            lines = if (answer.clearVault) {
                welcomeLines() + TerminalLine("you", fixed.text, now + 1) + catLine
            } else {
                next + catLine
            }
            notice = answer.notice
            app.terminalVault.save(lines)
            refreshFace()
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            face,
            color = if (privateOn) NeonMagenta else NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Text(
            "Encrypted apart from Chat. English commands. Analyst voice by default.",
            color = Color(0xFF8FB8A0),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        PrivateSwitch(privateOn, onPrivate)
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
            commandChips.forEach { chip ->
                OutlinedButton(
                    onClick = { send(chip) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) { Text(chip, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
            }
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
