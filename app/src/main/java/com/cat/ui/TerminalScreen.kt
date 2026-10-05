package com.cat.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import com.cat.ai.CopilotMode
import com.cat.data.TerminalLine
import com.cat.security.SoftCorrect
import com.cat.security.TerminalLock
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val commandChips = listOf("/help", "/recall", "/status")

private val welcomeExamples = listOf(
    "help" to "what can you do",
    "remember tea is at 4" to "save a note",
    "where is tea" to "use memory",
    "status" to "version and lock"
)

@Composable
fun TerminalScreen(app: CAtApplication, wide: Boolean = false) {
    var unlocked by remember { mutableStateOf(app.terminalLock.unlocked) }
    var privateOn by remember { mutableStateOf(app.prefs.terminalPrivate) }
    PrivateRecents(privateOn)
    if (!unlocked) {
        TerminalLockGate(app, privateOn, onPrivate = {
            privateOn = it
            app.prefs.terminalPrivate = it
        }) { unlocked = true }
    } else {
        TerminalConsole(app, privateOn, wide, onPrivate = {
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
        PressButton(onClick = {
            message = submitPin(app, pin, onUnlocked)
            if (!app.terminalLock.unlocked) pin = ""
        }, enabled = wait == 0) { Text("Unlock") }
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

@Composable
private fun PrivateBadge() {
    Box(
        modifier = Modifier
            .border(1.5.dp, NeonMagenta, RoundedCornerShape(4.dp))
            .background(NeonMagenta.copy(alpha = 0.22f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            "PRIVATE",
            color = NeonMagenta,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun PressButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonCyan,
            contentColor = Color.Black,
            disabledContainerColor = Color(0xFF1A3336),
            disabledContentColor = Color(0xFF6A8A96)
        )
    ) { content() }
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
        Tap an example chip, or type help, remember <fact>, recall, time, status, version.
        clear asks first. /clear yes wipes this vault only. Chat stays.
        Voice defaults to Analyst. Typos on those commands are repaired.
        Replies rank saved memories that share your words and say how many were used.
        Evolve path: YOU, SoftCorrect, MEMORY, REPLY, then an Evolve note when one is saved. Trace only — this app does not update itself.
        Private ON stays offline, blocks cloud, and hides this screen in recents.
        Slang you type can land in an Evolve note. Speech stays on Chat, not here. No mic.
        Chat stays a separate tab. PIN lock stays on this transcript.
    """.trimIndent()
    return listOf(TerminalLine("sys", text, now))
}

@Composable
private fun TerminalConsole(
    app: CAtApplication,
    privateOn: Boolean,
    wide: Boolean,
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
    var railSteps by remember { mutableStateOf(algorithmSteps(privateOn)) }
    var railActive by remember { mutableStateOf(-1) }
    var stepLabel by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var evolveLine by remember { mutableStateOf<String?>(null) }
    var askAuto by remember { mutableStateOf(false) }
    var autoNote by remember { mutableStateOf<String?>(null) }
    var modeLabel by remember { mutableStateOf(app.prefs.mode.label) }

    suspend fun refreshFace() {
        val snap = runCatching { app.copilot.terminalFace() }.getOrNull()
        face = if (snap == null) {
            "v${BuildConfig.VERSION_NAME}"
        } else if (snap.privateMode) {
            "private · offline · cloud blocked · v${BuildConfig.VERSION_NAME} · memories ${snap.memoryCount} · ${snap.voice}"
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
        draft = ""
        busy = true
        val steps = algorithmSteps(privateOn)
        railSteps = steps
        scope.launch {
            suspend fun light(name: String) {
                railActive = steps.indexOf(name)
                stepLabel = name
                delay(120)
            }
            light("YOU")
            val fixed = SoftCorrect.apply(raw)
            light("SoftCorrect")
            val now = System.currentTimeMillis()
            val prior = lines.filter { it.role == "you" || it.role == "cat" }
                .map { (if (it.role == "you") "user" else "assistant") to it.text }
            val next = lines.toMutableList()
            if (fixed.changed && fixed.note != null) {
                next += TerminalLine("sys", fixed.note, now)
            }
            next += TerminalLine("you", fixed.text, now + 1)
            lines = next
            light("MEMORY")
            if ("PRIVATE" in steps) light("PRIVATE")
            val answer = runCatching {
                app.copilot.answerTerminal(
                    fixed.text,
                    prior,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
            }.getOrElse {
                com.cat.data.CopilotRepository.TerminalAnswer(
                    "Couldn't answer that one. Try again.",
                    null
                )
            }
            railActive = steps.indexOf("REPLY").let { if (it < 0) steps.lastIndex else it }
            stepLabel = "REPLY"
            val catLine = TerminalLine("cat", answer.reply, System.currentTimeMillis())
            lines = if (answer.clearVault) {
                welcomeLines() + TerminalLine("you", fixed.text, now + 1) + catLine
            } else {
                next + catLine
            }
            confirmClear = !answer.clearVault &&
                answer.reply.contains("/clear yes", ignoreCase = true)
            notice = answer.notice
            app.terminalVault.save(lines)
            refreshFace()
            if (!answer.evolveNote.isNullOrBlank()) {
                evolveLine = answer.evolveNote
                light("EVOLVE")
                delay(640)
            } else {
                evolveLine = null
                delay(360)
            }
            stepLabel = null
            railActive = -1
            busy = false
        }
    }

    val transcript: @Composable (Modifier) -> Unit = { modifier ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .background(Color(0xFF0A120C), RoundedCornerShape(8.dp))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
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
    }

    val header: @Composable () -> Unit = {
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
            if (privateOn) PrivateBadge()
            OutlinedButton(onClick = {
                if (privateOn) {
                    askAuto = true
                } else {
                    app.prefs.mode = CopilotMode.AUTO
                    modeLabel = CopilotMode.AUTO.label
                    autoNote = "Auto is on. Cloud only if an address is saved. Otherwise this stays offline."
                }
            }) { Text(if (modeLabel == "Auto") "Auto on" else "Auto", color = NeonCyan) }
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
        EvolvePathPanel(
            steps = if (busy) railSteps else algorithmSteps(privateOn),
            active = railActive,
            evolveNote = evolveLine,
            wide = wide
        )
        if (!autoNote.isNullOrBlank()) {
            Text(
                autoNote!!,
                color = NeonLime,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
        Text(
            "Encrypted apart from Chat. English commands. Analyst voice by default.",
            color = Color(0xFFB8E0C8),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        PrivateSwitch(privateOn, onPrivate)
    }

    val composer: @Composable () -> Unit = {
        if (!notice.isNullOrBlank()) {
            Text(notice!!, color = NeonMagenta, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        if (confirmClear) {
            Text(
                "Confirm: this wipes the Terminal vault only.",
                color = NeonMagenta,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            PressButton(onClick = { send("/clear yes") }, enabled = !busy) {
                Text("Clear vault")
            }
        }
        val onlyBanner = lines.size <= 1
        if (onlyBanner) {
            Text(
                "Try an English example",
                color = NeonCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                welcomeExamples.take(2).forEach { (cmd, hint) ->
                    OutlinedButton(
                        onClick = { send(cmd) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$cmd · $hint", fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 2)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                welcomeExamples.drop(2).forEach { (cmd, hint) ->
                    OutlinedButton(
                        onClick = { send(cmd) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("$cmd · $hint", fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 2)
                    }
                }
            }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (privateOn) {
                Text("🔒", color = NeonMagenta, fontSize = 18.sp)
            }
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                label = { Text(if (privateOn) "C@T> 🔒 Private" else "C@T>") },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = NeonLime
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                singleLine = true
            )
            PressButton(onClick = { send() }, enabled = !busy) { Text("Run") }
        }
    }

    if (askAuto) {
        AlertDialog(
            onDismissRequest = { askAuto = false },
            title = { Text("Use Auto while Private is on?") },
            text = {
                Text("Auto tries a saved cloud address, then stays offline. Private blocks cloud. Turn Private off, or keep it and stay offline.")
            },
            confirmButton = {
                TextButton(onClick = {
                    app.prefs.mode = CopilotMode.AUTO
                    modeLabel = CopilotMode.AUTO.label
                    onPrivate(false)
                    autoNote = "Auto is on. Private is off. Cloud only if an address is saved."
                    askAuto = false
                }) { Text("Turn Private off") }
            },
            dismissButton = {
                TextButton(onClick = {
                    app.prefs.mode = CopilotMode.AUTO
                    modeLabel = CopilotMode.AUTO.label
                    autoNote = "Auto is on. Private stays on, so replies stay offline."
                    askAuto = false
                }) { Text("Keep Private") }
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050605))
    ) {
        AlgorithmScanBackdrop(Modifier.matchParentSize())
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlgorithmRailVertical(
                    steps = if (busy) railSteps else algorithmSteps(privateOn),
                    active = railActive,
                    modifier = Modifier
                        .width(148.dp)
                        .fillMaxHeight()
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    header()
                    transcript(Modifier.weight(1f).fillMaxWidth())
                    composer()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                header()
                transcript(Modifier.weight(1f).fillMaxWidth())
                composer()
            }
        }
    }
}
