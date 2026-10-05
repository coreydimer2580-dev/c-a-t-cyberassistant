package com.cat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.model.MemoryStack
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

@Composable
fun MemoryScreen(stacks: List<MemoryStack>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Memory Stack Manager", color = NeonCyan, fontSize = 28.sp)
        Text(
            "These stacks are in-app labels. C@T does not read your phone files or history.",
            color = Color(0xFFBFE8FF)
        )

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
