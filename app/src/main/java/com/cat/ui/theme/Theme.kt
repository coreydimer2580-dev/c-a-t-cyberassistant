package com.cat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NeonCyan = Color(0xFF00FFFF)
val NeonMagenta = Color(0xFFFF00EA)
val NeonLime = Color(0xFFD6FF00)
val Ink = Color(0xFF000000)
val Panel = Color(0xFF07080C)
val PanelRaised = Color(0xFF10131A)
val Mist = Color(0xFFBFE8FF)
val Paper = Color(0xFFEAFBFF)

private val CATColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Ink,
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A0033),
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonLime,
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF1A3300),
    onTertiaryContainer = NeonLime,
    background = Ink,
    onBackground = Paper,
    surface = Panel,
    onSurface = Paper,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = Mist,
    outline = NeonCyan,
    error = NeonMagenta,
    onError = Ink
)

private val CATType = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 56.sp,
        letterSpacing = 1.sp,
        color = NeonCyan
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.4.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = Paper
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun CATTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CATColors,
        typography = CATType,
        content = content
    )
}
