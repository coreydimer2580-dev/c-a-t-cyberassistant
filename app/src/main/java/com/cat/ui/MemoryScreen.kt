package com.cat.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.cat.data.MemoryEntity
import com.cat.model.MemoryStack
import com.cat.ui.theme.NeonCyan
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
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var flashId by remember { mutableLongStateOf(-1L) }
    val newest = notes.firstOrNull()?.id
    LaunchedEffect(newest) {
        val id = newest ?: return@LaunchedEffect
        flashId = id
        delay(1600)
        if (flashId == id) flashId = -1L
    }

    if (wide) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NotesPane(
                modifier = Modifier.weight(1.3f),
                notes = notes,
                flashId = flashId,
                draft = draft,
                status = status,
                onDraft = { draft = it },
                onAdd = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) scope.launch {
                        runCatching {
                            dao.insert(
                                MemoryEntity(
                                    content = text,
                                    category = "note",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            draft = ""
                            status = "Saved on this phone. No expiry."
                        }.onFailure {
                            status = it.message ?: "Save failed"
                            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onClear = {
                    scope.launch {
                        runCatching {
                            dao.clearAll()
                            status = "Cleared"
                        }.onFailure { status = it.message ?: "Clear failed" }
                    }
                }
            )
            StackPane(stacks, Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            NotesPane(
                modifier = Modifier.weight(1.4f),
                notes = notes,
                flashId = flashId,
                draft = draft,
                status = status,
                onDraft = { draft = it },
                onAdd = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) scope.launch {
                        runCatching {
                            dao.insert(
                                MemoryEntity(
                                    content = text,
                                    category = "note",
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            draft = ""
                            status = "Saved on this phone. No expiry."
                        }.onFailure {
                            status = it.message ?: "Save failed"
                            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onClear = {
                    scope.launch {
                        runCatching {
                            dao.clearAll()
                            status = "Cleared"
                        }.onFailure { status = it.message ?: "Clear failed" }
                    }
                }
            )
            StackPane(stacks, Modifier.weight(0.9f).padding(top = 8.dp))
        }
    }
}

@Composable
private fun NotesPane(
    modifier: Modifier,
    notes: List<MemoryEntity>,
    flashId: Long,
    draft: String,
    status: String,
    onDraft: (String) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Local notes", color = NeonCyan, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text(
            "Live from Room on this phone. Kept until you clear them. No expiry. C@T does not read your files.",
            color = Color(0xFFBFE8FF)
        )
        OutlinedTextField(
            value = draft,
            onValueChange = onDraft,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("New note") }
        )
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
            items(notes, key = { it.id }) { note ->
                val hot = note.id == flashId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (hot) Color(0xFF14210A) else Color(0xFF111821), RoundedCornerShape(16.dp))
                        .border(1.dp, if (hot) NeonLime else NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(note.content, color = if (hot) NeonLime else Color(0xFFEAFBFF), fontWeight = FontWeight.Bold)
                    Text(note.category, color = NeonMagenta)
                    Text(
                        DateFormat.getDateTimeInstance().format(Date(note.createdAt)),
                        color = Color(0xFFBFE8FF)
                    )
                }
            }
        }
    }
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
                    .background(Color(0xFF111821), RoundedCornerShape(16.dp))
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
