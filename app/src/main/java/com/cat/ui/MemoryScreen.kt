package com.cat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.CAtApplication
import com.cat.ProductGuide
import com.cat.data.MemoryEntity
import com.cat.data.MemoryStorage
import com.cat.data.TruthTag
import com.cat.model.MemoryStack
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.neonCard
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(stacks: List<MemoryStack>, wide: Boolean) {
    val context = LocalContext.current
    val dao = (context.applicationContext as CAtApplication).database.memoryDao()
    val scope = rememberCoroutineScope()
    val notes by dao.observeAll().collectAsState(initial = emptyList())
    val storage = remember(notes.size, notes.sumOf { it.content.length }) {
        MemoryStorage.probe(context)
    }
    var draft by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf(TruthTag.UNSURE) }
    var status by remember { mutableStateOf("") }
    var flashId by remember { mutableLongStateOf(-1L) }
    val newest = notes.firstOrNull()?.id
    LaunchedEffect(newest) {
        val id = newest ?: return@LaunchedEffect
        flashId = id
        delay(1600)
        if (flashId == id) flashId = -1L
    }
    val onAdd: () -> Unit = {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            scope.launch {
                runCatching {
                    dao.insert(
                        MemoryEntity(
                            content = text,
                            category = "note",
                            createdAt = System.currentTimeMillis(),
                            truthTag = TruthTag.normalize(tag)
                        )
                    )
                    draft = ""
                    status = "C@T hard-saved on this phone. No expiry. Tag: ${TruthTag.normalize(tag)}."
                }.onFailure {
                    status = it.message ?: "Save failed"
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                }
            }
        }
        Unit
    }
    val onClear: () -> Unit = {
        scope.launch {
            runCatching {
                dao.clearAll()
                status = "Cleared"
            }.onFailure { status = it.message ?: "Clear failed" }
        }
        Unit
    }
    val onRetag: (MemoryEntity) -> Unit = { note ->
        scope.launch {
            val current = TruthTag.normalize(note.truthTag)
            val next = TruthTag.ALL[(TruthTag.ALL.indexOf(current) + 1) % TruthTag.ALL.size]
            runCatching { dao.updateTruthTag(note.id, next) }
                .onFailure { status = it.message ?: "Tag update failed" }
        }
        Unit
    }

    // AI memory only — theater stacks demoted unless explicitly provided.
    NotesPane(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        notes = notes,
        storage = storage,
        flashId = flashId,
        draft = draft,
        tag = tag,
        status = status,
        onDraft = { draft = it },
        onTag = { tag = it },
        onAdd = onAdd,
        onClear = onClear,
        onRetag = onRetag
    )
}

@Composable
private fun NotesPane(
    modifier: Modifier,
    notes: List<MemoryEntity>,
    storage: MemoryStorage.Snapshot,
    flashId: Long,
    draft: String,
    tag: String,
    status: String,
    onDraft: (String) -> Unit,
    onTag: (String) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onRetag: (MemoryEntity) -> Unit
) {
    val styleIds = notes.filter { note ->
        val body = note.content.trim()
        body.startsWith("best so far:", ignoreCase = true) ||
            body.contains("style:", ignoreCase = true)
    }.map { it.id }.toSet()
    val styleNotes = notes.filter { it.id in styleIds }
    val rest = notes.filter { it.id !in styleIds }
    val evolving = rest.filter { it.category == "learn" }
    val hard = rest.filter { it.category != "learn" }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("C@T hard save", color = NeonCyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(ProductGuide.PATH, color = NeonLime, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(storage.label, color = NeonMagenta, fontWeight = FontWeight.Bold)
        storage.warning?.let { warning ->
            Text(warning, color = NeonLime, fontWeight = FontWeight.Bold)
        }
        Text(
            "Live from Room on this phone. Kept until you clear them. You pick True, False, or Unsure. C@T does not decide and is not a lie detector.",
            color = Color(0xFFBFE8FF)
        )
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New note") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TruthTag.ALL.forEach { choice ->
                FilterChip(
                    selected = TruthTag.normalize(tag) == choice,
                    onClick = { onTag(choice) },
                    label = { Text(choice) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAdd) { Text("Add") }
            Button(onClick = onClear) { Text("Clear") }
        }
        if (status.isNotEmpty()) {
            Text(status, color = NeonLime)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("How you talk", color = NeonLime, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    "Evolve notes that kept words you actually typed. Not invented slang. Not a self-update.",
                    color = Color(0xFFBFE8FF),
                    fontSize = 13.sp
                )
            }
            if (styleNotes.isEmpty()) {
                item { Text("No style notes yet. Type the way you talk in Terminal or Chat.", color = Color(0xFFBFE8FF)) }
            }
            items(styleNotes, key = { "style-${it.id}" }) { note ->
                NoteCard(note, note.id == flashId, onRetag)
            }
            item {
                Text("Evolving memory", color = NeonMagenta, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(
                    "Short notes C@T keeps when you say remember or use a confidence word. Tagged Unsure until you tap the badge.",
                    color = Color(0xFFBFE8FF),
                    fontSize = 13.sp
                )
            }
            if (evolving.isEmpty()) {
                item { Text("No evolving notes yet.", color = Color(0xFFBFE8FF)) }
            }
            items(evolving, key = { "learn-${it.id}" }) { note ->
                NoteCard(note, note.id == flashId, onRetag)
            }
            item { Text("All hard-saved notes", color = NeonCyan, fontWeight = FontWeight.Bold) }
            items(hard, key = { "hard-${it.id}" }) { note ->
                NoteCard(note, note.id == flashId, onRetag)
            }
        }
    }
}

@Composable
private fun NoteCard(note: MemoryEntity, hot: Boolean, onRetag: (MemoryEntity) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neonCard(
                accent = if (hot) NeonLime else NeonCyan,
                shape = RoundedCornerShape(16.dp),
                fill = if (hot) Color(0xFF14210A) else Color(0xFF111821),
                glow = if (hot) 18.dp else 12.dp
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagBadge(note.truthTag) { onRetag(note) }
            Text(note.category, color = NeonMagenta)
        }
        Text(note.content, color = if (hot) NeonLime else Color(0xFFEAFBFF), fontWeight = FontWeight.Bold)
        Text(
            DateFormat.getDateTimeInstance().format(Date(note.createdAt)),
            color = Color(0xFFBFE8FF)
        )
    }
}

@Composable
private fun TagBadge(tag: String, onClick: () -> Unit) {
    val label = TruthTag.normalize(tag)
    val color = when (label) {
        TruthTag.TRUE -> NeonLime
        TruthTag.FALSE -> NeonMagenta
        else -> NeonCyan
    }
    Text(
        text = label,
        color = Color.Black,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        modifier = Modifier
            .background(color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun StackPane(stacks: List<MemoryStack>, modifier: Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Stack labels", color = NeonCyan, fontSize = 22.sp)
        stacks.forEach { stack ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonCard(accent = NeonMagenta, shape = RoundedCornerShape(16.dp), fill = Color(0xFF111821), glow = 12.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Text(stack.name, color = Color(0xFFEAFBFF))
                    Text("${stack.usage}% used", color = NeonLime)
                    Text(if (stack.enabled) "Active" else "Disabled", color = NeonMagenta)
                }
            }
        }
    }
}
