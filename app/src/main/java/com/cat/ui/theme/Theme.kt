package com.cat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00F5FF)
val NeonMagenta = Color(0xFFFF00FF)
val NeonLime = Color(0xFF7CFF00)

private val CATColors = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonMagenta,
    tertiary = NeonLime,
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onPrimary = Color(0xFF000000),
    onBackground = NeonCyan,
    onSurface = NeonCyan
)

@Composable
fun CATTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CATColors,
        content = content
    )
}
