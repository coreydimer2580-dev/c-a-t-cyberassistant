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
import androidx.compose.foundation.layout.Column
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
                color = NeonCyan.copy(alpha = 0.18f),
                start = Offset(0f, y),
                end = Offset(size.width, y + size.width * slope),
                strokeWidth = 1.6f
            )
            y += gap
        }
        val back = (1f - phase) * gap
        var x = -size.height * 0.35f + back
        while (x < size.width + size.height) {
            drawLine(
                color = NeonMagenta.copy(alpha = 0.10f),
                start = Offset(x, 0f),
                end = Offset(x - size.height * 0.55f, size.height),
                strokeWidth = 1.1f
            )
            x += gap * 1.7f
        }
    }
}

fun algorithmSteps(privateOn: Boolean): List<String> {
    val head = listOf("YOU", "SoftCorrect", "MEMORY")
    val tail = listOf("REPLY", "EVOLVE")
    return if (privateOn) head + "PRIVATE" + tail else head + tail
}

/** One-line design copy for the step that is lit. Visual only. */
fun evolveStepDetail(step: String?, saved: String?): String {
    return when (step) {
        "YOU" -> "YOU — your words. Nothing else runs until you send."
        "SoftCorrect" -> "SoftCorrect — command typos get a light fix. Sentences stay yours."
        "MEMORY" -> "MEMORY — saved notes that share your words are ranked, then used."
        "PRIVATE" -> "PRIVATE — this screen stays on the phone. Cloud stays off."
        "REPLY" -> "REPLY — an English answer, then it waits for you."
        "EVOLVE" -> if (saved.isNullOrBlank()) {
            "Evolve note — a short note, including phrases you actually use. Not a self-update."
        } else {
            "Evolve note — $saved"
        }
        else -> saved?.let { "Evolve note — $it" }
            ?: "YOU → SoftCorrect → MEMORY → REPLY → Evolve note. On screen only. This app does not update itself."
    }
}

private fun stepColors(index: Int, active: Int): Pair<Color, Color> {
    val hot = index == active
    val done = active >= 0 && index < active
    val fg = when {
        hot -> NeonCyan
        done -> NeonLime
        else -> Color(0xFF4A6B58)
    }
    val bg = when {
        hot -> NeonCyan.copy(alpha = 0.28f)
        done -> NeonLime.copy(alpha = 0.12f)
        else -> Color(0xFF07110C)
    }
    return fg to bg
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
            val (fg, bg) = stepColors(index, active)
            val hot = index == active
            val done = active >= 0 && index < active
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(7.dp)
                        .height(2.dp)
                        .background(if (done || hot) fg.copy(alpha = 0.95f) else Color(0xFF1E3328))
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.2.dp, fg, RoundedCornerShape(3.dp))
                    .background(bg, RoundedCornerShape(3.dp))
                    .padding(vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = fg,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (hot) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 8.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Vertical rail for Fold / wide Terminal — sits beside the transcript. */
@Composable
fun AlgorithmRailVertical(steps: List<String>, active: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.2.dp, NeonCyan.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .background(Color(0xFF06100B), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "PATH",
            color = NeonLime,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        steps.forEachIndexed { index, label ->
            val (fg, bg) = stepColors(index, active)
            val hot = index == active
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, fg, RoundedCornerShape(4.dp))
                    .background(bg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${index + 1}. $label",
                    color = fg,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (hot) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/** Compact evolve-path design. Lights the same steps as the rail. */
@Composable
fun EvolvePathPanel(
    steps: List<String>,
    active: Int,
    evolveNote: String?,
    wide: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .background(Color(0xFF06100B), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Evolve path",
            color = NeonLime,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(
            "YOU → SoftCorrect → MEMORY → REPLY → Evolve note",
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        if (!wide) {
            AlgorithmRail(steps = steps, active = active)
        }
        val step = steps.getOrNull(active)
        Text(
            evolveStepDetail(step, evolveNote),
            color = if (step == "EVOLVE" || !evolveNote.isNullOrBlank() && step == null) NeonLime else Color(0xFFB8E0C8),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}
