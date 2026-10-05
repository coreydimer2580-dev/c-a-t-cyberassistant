package com.cat.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cat.ai.AiPersona
import com.cat.ui.theme.Mist
import com.cat.ui.theme.NeonCyan
import com.cat.ui.theme.NeonLime
import com.cat.ui.theme.NeonMagenta
import com.cat.ui.theme.neonCard
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Circular AI persona wheel. Drag to rotate; snaps to nearest spoke.
 */
@Composable
fun AiWheel(
    selected: AiPersona,
    onSelect: (AiPersona) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp
) {
    val personas = remember { AiPersona.wheelOrder() }
    val slice = 360f / personas.size
    var rotation by remember(selected) {
        mutableFloatStateOf(-personas.indexOf(selected) * slice)
    }
    var dragging by remember { mutableStateOf(false) }

    fun snapTo(index: Int) {
        val i = ((index % personas.size) + personas.size) % personas.size
        rotation = -i * slice
        onSelect(personas[i])
    }

    fun indexFromRotation(rot: Float): Int {
        val normalized = ((-rot / slice) % personas.size + personas.size) % personas.size
        return floor(normalized + 0.5f).toInt() % personas.size
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .neonCard(
                    accent = NeonCyan,
                    shape = RoundedCornerShape(999.dp),
                    fill = Color(0xFF05060A),
                    glow = 20.dp
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(size - 16.dp)
                    .pointerInput(personas) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = {
                                dragging = false
                                snapTo(indexFromRotation(rotation))
                            },
                            onDragCancel = {
                                dragging = false
                                snapTo(indexFromRotation(rotation))
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
                                // Approximate angular drag from horizontal motion
                                rotation += dragAmount.x * 0.35f
                            }
                        )
                    }
            ) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val radius = this.size.minDimension / 2f * 0.92f
                val colors = listOf(NeonCyan, NeonMagenta, NeonLime, NeonCyan, NeonMagenta, NeonLime, NeonCyan)

                personas.forEachIndexed { index, persona ->
                    val mid = Math.toRadians((rotation + index * slice + slice / 2 - 90).toDouble())
                    val isSelected = persona == selected
                    val accent = colors[index % colors.size]
                    drawArc(
                        color = accent.copy(alpha = if (isSelected) 0.55f else 0.18f),
                        startAngle = rotation + index * slice - 90f,
                        sweepAngle = slice - 2f,
                        useCenter = true,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                    drawArc(
                        color = accent,
                        startAngle = rotation + index * slice - 90f,
                        sweepAngle = slice - 2f,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = if (isSelected) 6f else 2.5f, cap = StrokeCap.Round)
                    )
                    val labelR = radius * 0.62f
                    val lx = cx + (cos(mid) * labelR).toFloat()
                    val ly = cy + (sin(mid) * labelR).toFloat()
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = if (isSelected) 34f else 26f
                            color = if (isSelected) {
                                android.graphics.Color.WHITE
                            } else {
                                android.graphics.Color.argb(200, 191, 232, 255)
                            }
                            isFakeBoldText = isSelected
                        }
                        drawText(persona.shortLabel, lx, ly + 10f, paint)
                    }
                }
                // Hub
                drawCircle(color = Color(0xFF0A0C12), radius = radius * 0.28f, center = Offset(cx, cy))
                drawCircle(
                    color = NeonCyan,
                    radius = radius * 0.28f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f)
                )
                // Pointer at top
                drawLine(
                    color = NeonLime,
                    start = Offset(cx, cy - radius + 4f),
                    end = Offset(cx, cy - radius + 28f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }
        Text(
            text = selected.label,
            color = NeonCyan,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = selected.styleHint,
            color = Mist,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        if (!dragging) {
            Text("Drag the wheel · snaps to AI", color = NeonLime, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
