package com.cat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ai.SensitiveFilter
import com.cat.data.MemoryEntity
import com.cat.data.RouteLogEntity
import com.cat.tools.AuPhone
import com.cat.tools.LocalTools
import com.cat.tools.PhoneIntents
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(wide: Boolean) {
    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Intro()
                RouteLogCard()
                PhoneCard()
                NotesCard()
                ChecklistCard()
                ClockCard()
                TimerCard()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScrubberCard()
                StrengthCard()
                GeneratorCard()
                ConverterCard()
                Base64Card()
                JsonCard()
                HashCard()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Intro()
            RouteLogCard()
            PhoneCard()
            NotesCard()
            ChecklistCard()
            ClockCard()
            TimerCard()
            ScrubberCard()
            StrengthCard()
            GeneratorCard()
            ConverterCard()
            Base64Card()
            JsonCard()
            HashCard()
        }
    }
}

@Composable
private fun Intro() {
    Text("Tools", color = NeonCyan, fontSize = 28.sp)
    Text(
        "Free and on this phone. Australia/Perth, en-AU. Nothing here needs Wi-Fi or a paid API.",
        color = Color(0xFFBFE8FF)
    )
}

@Composable
private fun PhoneCard() {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    CardColumn {
        Text("Call or text", color = NeonCyan, fontSize = 22.sp)
        Text(
            "Opens your phone's dialer (ACTION_DIAL) or SMS app (ACTION_SENDTO). C@T does not place the call or send the text, and this is not a separate network.",
            color = Color(0xFFBFE8FF)
        )
        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Normal phone number") },
            placeholder = { Text("0412345678") }
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SMS draft (optional)") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val intent = PhoneIntents.dialIntent(number)
                status = PhoneIntents.launch(context, intent)
                    ?: "Dialer opened. You still confirm the call there."
            }) { Text("Call") }
            Button(onClick = {
                val intent = PhoneIntents.smsIntent(number, draft)
                status = PhoneIntents.launch(context, intent)
                    ?: "SMS app opened with a draft. You still send it there."
            }) { Text("Text") }
        }
        if (AuPhone.parse(number) == null && number.isNotBlank()) {
            Text("Use a normal number. Star and hash codes are blocked.", color = NeonMagenta)
        }
        if (status.isNotEmpty()) Text(status, color = NeonLime)
    }
}


@Composable
private fun RouteLogCard() {
    val app = LocalContext.current.applicationContext as CAtApplication
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf<List<RouteLogEntity>>(emptyList()) }
    var label by remember { mutableStateOf("") }
    var fromPlace by remember { mutableStateOf("") }
    var toPlace by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { lines = app.database.routeLogDao().newestFirst() }
    CardColumn {
        Text("Route log", color = NeonCyan, fontSize = 22.sp)
        Text(
            "Typed lines only, stored on this phone. Time is Australia/Perth when you add a line. No Wi-Fi scan, Bluetooth, radio, or background discovery.",
            color = Color(0xFFBFE8FF)
        )
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Label") }
        )
        OutlinedTextField(
            value = fromPlace,
            onValueChange = { fromPlace = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("From") }
        )
        OutlinedTextField(
            value = toPlace,
            onValueChange = { toPlace = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("To") }
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val nextLabel = label.trim()
                val nextFrom = fromPlace.trim()
                val nextTo = toPlace.trim()
                val nextNote = note.trim()
                if (nextLabel.isEmpty() && nextFrom.isEmpty() && nextTo.isEmpty() && nextNote.isEmpty()) {
                    return@Button
                }
                label = ""
                fromPlace = ""
                toPlace = ""
                note = ""
                scope.launch {
                    app.database.routeLogDao().insert(
                        RouteLogEntity(
                            label = nextLabel,
                            fromPlace = nextFrom,
                            toPlace = nextTo,
                            note = nextNote,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    lines = app.database.routeLogDao().newestFirst()
                }
            }) { Text("Add line") }
            if (lines.isNotEmpty()) {
                OutlinedButton(onClick = {
                    scope.launch {
                        app.database.routeLogDao().clearAll()
                        lines = emptyList()
                    }
                }) { Text("Clear all") }
            }
        }
        lines.forEach { line ->
            val stamp = LocalTools.formatZone(line.createdAt, LocalTools.PERTH_ZONE)
            val title = line.label.ifBlank { "Untitled" }
            Text("$title · $stamp", color = NeonCyan)
            Text("${line.fromPlace.ifBlank { "—" }} → ${line.toPlace.ifBlank { "—" }}", color = Color(0xFFEAFBFF))
            if (line.note.isNotBlank()) Text(line.note, color = Color(0xFFBFE8FF))
            OutlinedButton(onClick = {
                scope.launch {
                    app.database.routeLogDao().deleteById(line.id)
                    lines = app.database.routeLogDao().newestFirst()
                }
            }) { Text("Delete") }
        }
    }
}

@Composable
private fun NotesCard() {
    val app = LocalContext.current.applicationContext as CAtApplication
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<MemoryEntity>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { notes = app.database.memoryDao().getAll() }
    CardColumn {
        Text("Notes", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Local note") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val text = draft.trim()
                if (text.isEmpty()) return@Button
                draft = ""
                scope.launch {
                    app.database.memoryDao().insert(
                        MemoryEntity(content = text, category = "note", createdAt = System.currentTimeMillis())
                    )
                    notes = app.database.memoryDao().getAll()
                }
            }) { Text("Save") }
            OutlinedButton(onClick = {
                scope.launch {
                    app.database.memoryDao().clearAll()
                    notes = emptyList()
                }
            }) { Text("Clear") }
        }
        notes.take(8).forEach { note ->
            Text("• [${com.cat.data.TruthTag.normalize(note.truthTag)}] ${note.content}", color = Color(0xFFEAFBFF))
        }
    }
}

@Composable
private fun ChecklistCard() {
    val app = LocalContext.current.applicationContext as CAtApplication
    var items by remember { mutableStateOf(app.prefs.loadTodos()) }
    var draft by remember { mutableStateOf("") }
    fun persist(next: List<Pair<Boolean, String>>) {
        items = next
        app.prefs.saveTodos(next)
    }
    CardColumn {
        Text("Checklist", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("To-do") }
        )
        Button(onClick = {
            val text = draft.trim()
            if (text.isEmpty()) return@Button
            draft = ""
            persist(items + (false to text))
        }) { Text("Add") }
        items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = item.first,
                    onClick = {
                        persist(items.mapIndexed { i, pair ->
                            if (i == index) !pair.first to pair.second else pair
                        })
                    },
                    label = { Text(if (item.first) "done" else "open") }
                )
                Text(item.second, color = Color(0xFFEAFBFF), modifier = Modifier.weight(1f))
            }
        }
        if (items.isNotEmpty()) {
            OutlinedButton(onClick = { persist(emptyList()) }) { Text("Clear list") }
        }
    }
}

@Composable
private fun ClockCard() {
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000)
        }
    }
    CardColumn {
        Text("World clock", color = NeonCyan, fontSize = 22.sp)
        Text("Home: Australia/Perth. Locale en-AU. Computed on device.", color = NeonLime)
        LocalTools.clockReport(tick).lineSequence().forEach { line ->
            Text(line, color = if (line.startsWith("Perth")) NeonCyan else Color(0xFFEAFBFF))
        }
    }
}

@Composable
private fun TimerCard() {
    var seconds by remember { mutableIntStateOf(60) }
    var remaining by remember { mutableIntStateOf(60) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
            if (remaining <= 0) running = false
        }
    }
    CardColumn {
        Text("Timer", color = NeonCyan, fontSize = 22.sp)
        Text(formatTimer(remaining), color = NeonLime, fontSize = 32.sp)
        OutlinedTextField(
            value = seconds.toString(),
            onValueChange = { raw ->
                val n = raw.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0
                seconds = n.coerceIn(0, 86400)
                if (!running) remaining = seconds
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Seconds") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (remaining <= 0) remaining = seconds.coerceAtLeast(1)
                running = true
            }) { Text(if (running) "Running" else "Start") }
            OutlinedButton(onClick = { running = false }) { Text("Pause") }
            OutlinedButton(onClick = {
                running = false
                remaining = seconds
            }) { Text("Reset") }
        }
    }
}

@Composable
private fun ScrubberCard() {
    val context = LocalContext.current
    val filter = remember { SensitiveFilter() }
    var source by remember { mutableStateOf("") }
    var redacted by remember { mutableStateOf("") }
    CardColumn {
        Text("Clipboard scrubber", color = NeonCyan, fontSize = 22.sp)
        Text("Reads only the clipboard when you tap, then redacts it in this app.", color = Color(0xFFBFE8FF))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                source = readClipboard(context)
                redacted = filter.sanitize(source)
            }) { Text("Read clipboard") }
            OutlinedButton(onClick = {
                if (redacted.isNotEmpty()) copyText(context, redacted)
            }) { Text("Copy redacted") }
        }
        if (source.isNotEmpty()) Text(source, color = Color(0xFFBFE8FF))
        if (redacted.isNotEmpty()) Text(redacted, color = NeonLime)
    }
}

@Composable
private fun StrengthCard() {
    var phrase by remember { mutableStateOf("") }
    val strength = LocalTools.passphraseStrength(phrase)
    CardColumn {
        Text("Passphrase strength", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phrase") }
        )
        Text("${strength.score}/4 ${strength.label}", color = NeonLime)
    }
}

@Composable
private fun GeneratorCard() {
    val context = LocalContext.current
    var length by remember { mutableIntStateOf(16) }
    var symbols by remember { mutableStateOf(true) }
    var generated by remember { mutableStateOf("") }
    CardColumn {
        Text("Passphrase generator", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = length.toString(),
            onValueChange = { raw ->
                length = raw.filter { it.isDigit() }.take(2).toIntOrNull()?.coerceIn(8, 64) ?: 8
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Length 8–64") }
        )
        FilterChip(selected = symbols, onClick = { symbols = !symbols }, label = { Text("Symbols") })
        Button(onClick = { generated = LocalTools.generatePassphrase(length, symbols) }) { Text("Generate") }
        if (generated.isNotEmpty()) {
            Text(generated, color = NeonLime)
            OutlinedButton(onClick = { copyText(context, generated) }) { Text("Copy") }
        }
    }
}

@Composable
private fun ConverterCard() {
    var raw by remember { mutableStateOf("10") }
    var from by remember { mutableStateOf("km") }
    var to by remember { mutableStateOf("mi") }
    val value = raw.toDoubleOrNull()
    val result = value?.let { LocalTools.convert(it, from, to) }
    CardColumn {
        Text("Unit converter", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Value") }
        )
        Text("From", color = Color(0xFFBFE8FF))
        ChipRow(listOf("km", "m", "mi", "ft", "kg", "lb", "C", "F"), from) { from = it }
        Text("To", color = Color(0xFFBFE8FF))
        ChipRow(listOf("km", "m", "mi", "ft", "kg", "lb", "C", "F"), to) { to = it }
        val line = if (value == null || result == null) {
            "Pick matching units (length, mass, or temperature)."
        } else {
            "${LocalTools.formatAmount(value)} $from = ${LocalTools.formatAmount(result)} $to"
        }
        Text(line, color = NeonLime)
    }
}

@Composable
private fun Base64Card() {
    var raw by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    CardColumn {
        Text("Base64", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { output = LocalTools.base64Encode(raw) }) { Text("Encode") }
            OutlinedButton(onClick = {
                output = runCatching { LocalTools.base64Decode(raw) }.getOrElse { "Could not decode." }
            }) { Text("Decode") }
        }
        if (output.isNotEmpty()) Text(output, color = NeonLime)
    }
}

@Composable
private fun JsonCard() {
    var raw by remember { mutableStateOf("{\"city\":\"Perth\"}") }
    var output by remember { mutableStateOf("") }
    CardColumn {
        Text("JSON pretty", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("JSON") }
        )
        Button(onClick = {
            output = runCatching { LocalTools.prettyJson(raw) }.getOrElse { "Could not pretty-print that JSON." }
        }) { Text("Pretty") }
        if (output.isNotEmpty()) Text(output, color = NeonLime)
    }
}

@Composable
private fun HashCard() {
    var raw by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    CardColumn {
        Text("SHA-256", color = NeonCyan, fontSize = 22.sp)
        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text") }
        )
        Button(onClick = { output = LocalTools.sha256(raw) }) { Text("Hash") }
        if (output.isNotEmpty()) Text(output, color = NeonLime)
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onPick: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.take(4).forEach { option ->
            FilterChip(selected = selected == option, onClick = { onPick(option) }, label = { Text(option) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.drop(4).forEach { option ->
            FilterChip(selected = selected == option, onClick = { onPick(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun CardColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111821), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

private fun formatTimer(total: Int): String {
    val safe = total.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return "%d:%02d".format(m, s)
}

private fun readClipboard(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return ""
    if (clip.itemCount == 0) return ""
    return clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("C@T", text))
}
