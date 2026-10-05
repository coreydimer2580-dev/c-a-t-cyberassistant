package com.cat.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun Modifier.neonCard(
    accent: Color = NeonCyan,
    shape: Shape = RoundedCornerShape(18.dp),
    fill: Color = Color(0xFF070910),
    glow: Dp = 16.dp
): Modifier = this
    .shadow(
        elevation = glow,
        shape = shape,
        clip = false,
        ambientColor = accent,
        spotColor = accent
    )
    .background(fill, shape)
    .border(
        width = 1.5.dp,
        brush = Brush.linearGradient(listOf(accent, NeonMagenta, NeonLime)),
        shape = shape
    )

@Composable
fun CatWordmark(size: TextUnit = 56.sp, modifier: Modifier = Modifier) {
    Box(modifier) {
        Text(
            text = "C@T",
            color = NeonMagenta.copy(alpha = 0.75f),
            fontSize = size,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.offset(x = 3.dp, y = 3.dp)
        )
        Text(
            text = "C@T",
            color = NeonCyan,
            fontSize = size,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = NeonCyan,
                    blurRadius = 28f
                )
            )
        )
    }
}
