package com.cat.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta

/** Diagonal scan field. Visual only — it does not run or rewrite code. */
@Composable
fun AlgorithmScanBackdrop(modifier: Modifier = Modifier) {
    val shift = rememberInfiniteTransition(label = "algo-scan")
    val phase by shift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan-phase"
    )
    Canvas(modifier) {
        val gap = 28.dp.toPx()
        val drift = phase * gap
        val slope = 0.42f
        var y = -size.width * slope + drift
        val limit = size.height + size.width * slope
        while (y < limit) {
            drawLine(
                color = NeonCyan.copy(alpha = 0.16f),
                start = Offset(0f, y),
                end = Offset(size.width, y + size.width * slope),
                strokeWidth = 1.4f
            )
            y += gap
        }
        val back = (1f - phase) * gap
        var x = -size.height * 0.35f + back
        while (x < size.width + size.height) {
            drawLine(
                color = NeonMagenta.copy(alpha = 0.08f),
                start = Offset(x, 0f),
                end = Offset(x - size.height * 0.55f, size.height),
                strokeWidth = 1f
            )
            x += gap * 1.7f
        }
    }
}

fun algorithmSteps(privateOn: Boolean): List<String> {
    return if (privateOn) {
        listOf("INPUT", "CORRECT", "MEMORY", "PRIVATE", "REPLY")
    } else {
        listOf("INPUT", "CORRECT", "MEMORY", "REPLY")
    }
}

/** Neon step chips. [active] is the chip lighting now; earlier chips stay lit. */
@Composable
fun AlgorithmRail(steps: List<String>, active: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val hot = index == active
            val done = active >= 0 && index < active
            val fg = when {
                hot -> NeonCyan
                done -> NeonLime
                else -> Color(0xFF3D5A4A)
            }
            val bg = when {
                hot -> NeonCyan.copy(alpha = 0.24f)
                done -> NeonLime.copy(alpha = 0.10f)
                else -> Color(0xFF07110C)
            }
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(7.dp)
                        .height(2.dp)
                        .background(if (done || hot) fg.copy(alpha = 0.9f) else Color(0xFF1E3328))
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, fg, RoundedCornerShape(3.dp))
                    .background(bg, RoundedCornerShape(3.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = fg,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (hot) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}
